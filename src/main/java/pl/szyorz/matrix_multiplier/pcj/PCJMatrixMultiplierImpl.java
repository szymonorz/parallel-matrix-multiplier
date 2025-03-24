package pl.szyorz.matrix_multiplier.pcj;

import org.pcj.PCJ;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;
import pl.szyorz.dummy.DummyData;

@RegisterStorage(PCJMatrixMultiplierImpl.Shared.class)
public class PCJMatrixMultiplierImpl implements StartPoint {

    private static final int N = 100; // Global matrix size
    private static final int P = 10; // Process grid (P×P)
    private static final int blockSize = N / P;


    @Storage
    enum Shared {
        A, B
    }

    private int[][] A, B;
    private int rank, row, col, left, right, up, down;

    @Override
    public void main() {
        rank = PCJ.myId();
        int size = PCJ.threadCount();

        if (size != P * P) {
            if (rank == 0) {
                System.out.println("Number of threads must be a perfect square!");
            }
            return;
        }

        row = rank / P;
        col = rank % P;

        A = DummyData.matrix1;
        B = DummyData.matrix2;


        if (rank == 0) {
            // Initial left shift
            for (int i = 0; i < P; i++) {
                for (int shift = 0; shift < i; shift++) {
                    int temp = A[i][0];
                    for (int j = 0; j < P - 1; j++) {
                        A[i][j] = A[i][j + 1];
                    }
                    A[i][P - 1] = temp;
                }
            }

            // Initial up shift
            for (int j = 0; j < P; j++) {
                for (int shift = 0; shift < j; shift++) {
                    int temp = B[0][j];
                    for (int i = 0; i < P - 1; i++) {
                        B[i][j] = B[i + 1][j];
                    }
                    B[P - 1][j] = temp;
                }
            }

            PCJ.broadcast(A, Shared.A);
            PCJ.broadcast(B, Shared.B);
        }
        PCJ.waitFor(Shared.A);
        PCJ.waitFor(Shared.B);

        PCJ.barrier();

        int[][] C = new int[blockSize][blockSize];

        C[row][col]=0;
        for(int l=0; l<P; l++) {
            C[row][col] = C[row][col] + (A[row][col] * B[row][col]);
//            if (rank == 1) {
//                System.out.println("Row: " + row + " col: " + col);
//                System.out.println("A --------------");
//                printMatrix(A);
//                System.out.println("B -------------- ");
//                printMatrix(B);
//                System.out.println("C: " + C[row][col]);
//                System.out.println("A: " + A[row][col]);
//                System.out.println("B: " + B[row][col]);
//            }
            PCJ.barrier();


            if (rank == 0) {
                // Left shift
                for (int i = 0; i < P; i++) {
                        int temp = A[i][0];
                        for (int j = 0; j < P - 1; j++) {
                            A[i][j] = A[i][j + 1];
                        }
                        A[i][P - 1] = temp;
                }
                PCJ.broadcast(A, Shared.A);

                // Up shift
                for (int j = 0; j < P; j++) {
                        int temp = B[0][j];
                        for (int i = 0; i < P - 1; i++) {
                            B[i][j] = B[i + 1][j];
                        }
                        B[P - 1][j] = temp;
                }
                PCJ.broadcast(B,Shared.B);
            }
            PCJ.waitFor(Shared.A);
            PCJ.waitFor(Shared.B);
            PCJ.barrier();
        }

        PCJ.barrier();
        if (rank == 1) printMatrix(C);
    }

    private void printMatrix(int[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.print(matrix[i][j] + " ");
            }
            System.out.println(); // Move to the next line after each row
        }
    }
}
