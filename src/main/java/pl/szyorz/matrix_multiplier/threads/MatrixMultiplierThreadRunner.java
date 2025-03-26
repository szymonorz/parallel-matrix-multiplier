package pl.szyorz.matrix_multiplier.threads;

import pl.szyorz.matrix_multiplier.utils.MatrixFileIO;

import java.io.IOException;
import java.nio.file.Path;

public class MatrixMultiplierThreadRunner {
    private Path srcA, srcB;
    private int N;
    private MatrixMultiplierThread[] threads;
    private MatrixFileIO.Matrix A, B;
    private String destinationPath;

    public MatrixMultiplierThreadRunner(String A_path, String B_path, String destinationPath) throws IOException {
        this.A = MatrixFileIO.readFromFile(A_path);
        this.B = MatrixFileIO.readFromFile(B_path);
        this.destinationPath = destinationPath;

        if (A.N != B.N) {
            throw new IllegalStateException("Both matrices must be the same size. A is " + A.N +" B is " + B.N);
        }
        this.N = A.N;
        this.threads = new MatrixMultiplierThread[1];
    }

    public MatrixMultiplierThreadRunner(String A_path, String B_path,  String destinationPath, int threadsNum) throws IOException {
        this.A = MatrixFileIO.readFromFile(A_path);
        this.B = MatrixFileIO.readFromFile(B_path);
        this.destinationPath = destinationPath;

        if (A.N != B.N) {
            throw new IllegalStateException("Both matrices must be the same size. A is " + A.N +" B is " + B.N);
        }
        this.N = A.N;
        this.threads = new MatrixMultiplierThread[threadsNum];
    }

    public void multiply() throws IOException {
        System.out.println("Start!");
        long start = System.currentTimeMillis();

        int[][] C = new int[N][N];

        int threadCount = threads.length;
        int rowsPerThread = N / threadCount;
        int extraRows = N % threadCount;

        int startRow = 0;
        System.out.println("Computing.....");
        for (int i = 0; i < threads.length; i++) {
            int endRow = startRow + rowsPerThread + (i < extraRows ? 1 : 0);
            threads[i] = new MatrixMultiplierThread(A.values, B.values, C, startRow, endRow, N);
            threads[i].start();
            startRow = endRow;
        }

        for (int i = 0; i < threads.length; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        MatrixFileIO.writeToFile(new MatrixFileIO.Matrix(N, C), destinationPath);
        long finished = System.currentTimeMillis();

        long timeElapsed = finished - start;
        System.out.println("Finished! Elapsed time (ms): " + timeElapsed);
        System.out.println("Results were written to: " + destinationPath);
    }
}
