package pl.szyorz.matrix_multiplier.threads;

import java.nio.file.Path;

public class MatrixMultiplierThreadRunner {
    private Path srcA, srcB;
    private int N;
    private int[][] A, B;
    private MatrixMultiplierThread[] threads;
    public MatrixMultiplierThreadRunner(int[][] A, int[][] B, int N) {
        this.N = N;
        this.A = A;
        this.B = B;
        this.threads = new MatrixMultiplierThread[N];
    }

    public void multiply() {
        int[][] C = new int[N][N];
        for (int i = 0; i < N; i++) {
            threads[i] = new MatrixMultiplierThread(A, B, C, i, N);
            threads[i].start();
        }

        for (int i = 0; i < N; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Print the resulting matrix
        System.out.println("Result Matrix C:");
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                System.out.print(C[i][j] + " ");
            }
            System.out.println();
        }
    }
}
