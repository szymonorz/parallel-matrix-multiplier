package pl.szyorz.matrix_multiplier.threads;

class MatrixMultiplierThread extends Thread {
    private int[][] A, B, C;
    private int row;
    private int N;

    public MatrixMultiplierThread(int[][] A, int[][] B, int[][] C, int row, int N) {
        this.A = A;
        this.B = B;
        this.C = C;
        this.row = row;
        this.N = N;
    }

    @Override
    public void run() {
        for (int j = 0; j < N; j++) {
            C[row][j] = 0;
            for (int k = 0; k < N; k++) {
                C[row][j] += A[row][k] * B[k][j];
            }
        }
    }
}