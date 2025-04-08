package pl.szyorz.matrix_multiplier.pcj;

import org.pcj.PCJ;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;
import pl.szyorz.dummy.DummyData;
import pl.szyorz.matrix_multiplier.utils.MatrixFileIO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;

@RegisterStorage(PCJMatrixMultiplierImpl.Shared.class)
public class PCJMatrixMultiplierImpl implements StartPoint {

    private int N;

    @Storage
    enum Shared {
        A, B, N
    }

    private int[][] A, B;
    private int rank, row, col;
    private int p, sqrtP, blockSize;

    private final int debugRank = 11;

    @Override
    public void main() {
        rank = PCJ.myId();
        String destination = PCJ.getProperty("destination");

        p = Integer.parseInt(PCJ.getProperty("P"));
        sqrtP = (int)Math.sqrt(p);

        try {
            A = readSubMatrixFromFile(PCJ.getProperty("source1"));
            B = readSubMatrixFromFile(PCJ.getProperty("source2"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        if (rank == debugRank) {
//            System.out.println("Row: " + row +" col: " + col);
//        }
        for(int i=0; i<row; i++) {
//            if (rank == debugRank) {
//                System.out.println(i);
//            }
            shiftLeft();
        }
//        if(rank==4) {
//            System.out.println("[" + rank + "]: Finished initial shiftLeft");
//        }
        for(int j=0; j<col; j++) shiftUp();
//        if (rank == 4) {
//            System.out.println("[" + rank + "]: Finished initial shiftUp");
//        }

//        System.out.println("Rank " + rank + " ready");
        if (rank == debugRank) {
            System.out.println("done");
        }
        PCJ.barrier();

        int[][] C = new int[blockSize][blockSize];


        for(int l=0; l<sqrtP; l++) {
            multiplyMatrix(A, B, C);
            if (rank == debugRank) {
                printMatrix(A);
                printMatrix(B);
                printMatrix(C);
                System.out.println("==============");
            }
            shiftLeft();
            shiftUp();
            PCJ.barrier();
        }

        PCJ.barrier();
        try {
            MatrixFileIO.writePart(C, destination, blockSize, row, col);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        if (rank == 3) {
//            System.out.println("Finished");
//            printMatrix(C);
//        }
    }

    private void shiftLeft() {
        int left = col == 0 ? (row*sqrtP) + sqrtP-1 : (row*sqrtP) + col - 1;
        int right = col == sqrtP - 1 ? (row*sqrtP) : (row*sqrtP) + col + 1;
//        if (rank == debugRank) {
//            System.out.println("Left: " + left);
//            System.out.println("Right: " + right);
//            printMatrix(A);
//        }
//        if (rank == debugRank) {
            System.out.println("barrier: " + rank);
//        }
        if (col != 0) {
            PCJ.barrier(right);
        }
        System.out.println("Barrier reached: " + rank);
        PCJ.put(A, left, Shared.A);
//        if (rank == debugRank) {
//            System.out.println("Rank: " + rank);
//
//            System.out.println("Left: " + left);
//            System.out.println("Right: " + right);
//            printMatrix(A);
//        }
        PCJ.waitFor(Shared.A);
    }

    private void shiftUp() {
        int up = row == 0 ? sqrtP + col : ((row-1)*sqrtP) + col;
        int down = row == sqrtP - 1 ? col : ((row+1)*sqrtP) + col;
        PCJ.barrier(down);
        if (rank == debugRank) {
            System.out.println("Up: " + up);
            System.out.println("Down: " + down);
            printMatrix(B);
        }
        PCJ.put(B, up, Shared.B);
        PCJ.waitFor(Shared.B);
        if (rank == debugRank) System.out.println("boing");
    }

    private void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            for (int val : row) {
                System.out.print(val + " ");
            }
            System.out.println();
        }
    }

    private int[][] readSubMatrixFromFile(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            N = Integer.parseInt(reader.readLine().trim());

//            System.out.println("P: " + p); 4
//            System.out.println("N: " + N); 4
            blockSize = (int)(N / sqrtP);

//            System.out.println("BlockSize: " + blockSize); 2
            if (N % blockSize != 0) {
                throw new IllegalArgumentException("Matrix size must be divisible by blockSize.");
            }

            int numBlocks = N / blockSize;

            row = rank / sqrtP;
            col = rank % sqrtP;

//            System.out.println("numBlocks: " + numBlocks); 4

//            if (rank == 2) {
//                System.out.println("Row: " + row + " col: " + col);
//            }

            if (row < 0 || row >= numBlocks || col < 0 || col >= numBlocks) {
                throw new IndexOutOfBoundsException("Block index out of range.");
            }


            int[][] subMatrix = new int[blockSize][blockSize];
            int startRow = row * blockSize;
            int endRow = startRow + blockSize;

            String line;
            int currentRow = 0;
            int subRow = 0;

            while ((line = reader.readLine()) != null) {
                if (currentRow >= startRow && currentRow < endRow) {
                    String[] tokens = line.trim().split("\\s+");

                    for (int _col = 0; _col < blockSize; _col++) {
                        subMatrix[subRow][_col] = Integer.parseInt(tokens[col * blockSize + _col]);
                    }

                    subRow++;
                }

                currentRow++;

                if (currentRow >= endRow) {
                    break;
                }
            }

            return subMatrix;
        }
    }

    private void multiplyMatrix(int[][] A, int[][] B, int[][] C) {
        for(int i=0; i<blockSize; i++) {
            for(int j=0; j<blockSize; j++) {
                for(int k=0; k<blockSize; k++) {
                    C[i][j] += A[i][k] * B[k][j];
                }
            }
        }
    }
}
