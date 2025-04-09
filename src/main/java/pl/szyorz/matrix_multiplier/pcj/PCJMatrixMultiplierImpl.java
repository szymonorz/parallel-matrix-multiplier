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
        A, B
    }

    private int[][] A, B, A_local, B_local;
    private int rank, row, col;
    private int p, sqrtP, blockSize;

    private final int debugRank = 2;

    @Override
    public void main() {
        rank = PCJ.myId();
        String destination = PCJ.getProperty("destination");

        p = Integer.parseInt(PCJ.getProperty("P"));
        sqrtP = (int)Math.sqrt(p);

        try {
            A_local = readSubMatrixFromFile(PCJ.getProperty("source1"));
            B_local = readSubMatrixFromFile(PCJ.getProperty("source2"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        if (rank == debugRank) {
//            System.out.println("Row: " + row +" col: " + col);
//        }
        for(int i=0; i<row; i++) shiftLeft();
        for(int j=0; j<col; j++) shiftUp();

        int[][] C = new int[blockSize][blockSize];

        PCJ.barrier();
        for(int l=0; l<sqrtP; l++) {
            multiplyMatrix(A_local, B_local, C);
            if (rank == debugRank) {
                printMatrix(A_local);
                printMatrix(B_local);
                printMatrix(C);
                System.out.println("==============");
            }
            shiftLeft();
            shiftUp();
        }

        PCJ.barrier();
//        try {
//            MatrixFileIO.writePart(C, destination, blockSize, row, col);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        if (rank == debugRank) {
            System.out.println("Finished");
            printMatrix(C);
        }
    }

    private void shiftLeft() {
        int left = col == 0 ? (row*sqrtP) + sqrtP-1 : (row*sqrtP) + col - 1;
        int right = col == sqrtP - 1 ? (row*sqrtP) : (row*sqrtP) + col + 1;
//        try {
//            Thread.sleep(rank * 1000L);
//        } catch (InterruptedException e) {
//            System.err.println(e.getMessage());
//        }
//        if (rank == debugRank) {
//            System.out.printf("Rank: " + rank);
//            System.out.println("Left: " + left);
//            System.out.println("Right: " + right);
//            printMatrix(A_local);
//        }

//        System.out.println("barrier: " + rank + " right: " + right + " left: " + left);
//        System.out.println("Barrier reached: " + rank);
        PCJ.put(A_local, left, Shared.A);
        PCJ.waitFor(Shared.A);
        A_local = A.clone();
    }

    private void shiftUp() {
        int up = row == 0 ? ((sqrtP-1)*sqrtP) + col : ((row-1)*sqrtP) + col;
        int down = row == sqrtP - 1 ? col : ((row+1)*sqrtP) + col;

        if (rank == debugRank) {
            System.out.println("Up: " + up);
            System.out.println("Down: " + down);
            printMatrix(B_local);
        }
        PCJ.put(B_local, up, Shared.B);
        if (rank == debugRank) System.out.println("Waiting for B: " + rank +" " + up + " " + down);
        PCJ.waitFor(Shared.B);
        B_local = B.clone();
        if (rank == debugRank) System.out.println("Got B");

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
