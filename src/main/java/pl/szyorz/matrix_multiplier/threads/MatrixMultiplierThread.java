package pl.szyorz.matrix_multiplier.threads;

class MatrixMultiplierThread extends Thread {
    private int[][] A, B, C;
    private int startRow, endRow;
    private int N;

    public MatrixMultiplierThread(int[][] A, int[][] B, int[][] C, int startRow, int endRow, int N) {
        this.A = A;
        this.B = B;
        this.C = C;
        this.startRow = startRow;
        this.endRow = endRow;
        this.N = N;
    }

    @Override
    public void run() {
        for (int i = startRow; i < endRow; i++) {
            for (int j = 0; j < N; j++) {
                C[i][j] = 0;
                for (int k = 0; k < N; k++) {
                    C[i][j] += A[i][k] * B[k][j];
                }
            }
        }
    }
}