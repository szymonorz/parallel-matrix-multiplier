package pl.szyorz.matrix_multiplier.pcj;

import org.pcj.PCJ;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;
import pl.szyorz.matrix_multiplier.utils.MatrixFileIO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

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

    private int left, up;

    @Override
    public void main() {
        long start = System.currentTimeMillis();

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

        up = row == 0 ? ((sqrtP-1)*sqrtP) + col : ((row-1)*sqrtP) + col;
        left = col == 0 ? (row*sqrtP) + sqrtP-1 : (row*sqrtP) + col - 1;

        // Initial alignment
        int destA = (row*sqrtP) + ((col - row + sqrtP) % sqrtP);
        int destB = (((row - col + sqrtP) % sqrtP) * sqrtP) + col;

        PCJ.put(A_local, destA, Shared.A);
        PCJ.put(B_local, destB, Shared.B);
        PCJ.waitFor(Shared.A);
        PCJ.waitFor(Shared.B);
        PCJ.barrier();
        if (rank == 0) {
            System.out.println("Finished initial alignment");
        }

        A_local = A.clone();
        B_local = B.clone();

        int[][] C = new int[blockSize][blockSize];

        PCJ.barrier();
        for(int l=0; l<sqrtP; l++) {
            multiplyMatrix(A_local, B_local, C);
            shiftLeft();
            shiftUp();
        }

        PCJ.barrier();
        try {
            MatrixFileIO.writePart(C, destination, blockSize, row, col);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        long finished = System.currentTimeMillis();

        if (PCJ.myId() == 0) {
            System.out.println("Elapsed time (ms): " + (finished - start));
        }
    }

    private void shiftLeft() {
        PCJ.put(A_local, left, Shared.A);
        PCJ.waitFor(Shared.A);
        PCJ.barrier();
        A_local = A.clone();
    }

    private void shiftUp() {
        PCJ.put(B_local, up, Shared.B);
        PCJ.waitFor(Shared.B);
        PCJ.barrier();
        B_local = B.clone();
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

            blockSize = (int)(N / sqrtP);

            if (N % blockSize != 0) {
                throw new IllegalArgumentException("Matrix size must be divisible by blockSize.");
            }

            int numBlocks = N / blockSize;

            row = rank / sqrtP;
            col = rank % sqrtP;


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
