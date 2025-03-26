package pl.szyorz.matrix_multiplier.pcj;

import org.pcj.PCJ;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;
import pl.szyorz.dummy.DummyData;
import pl.szyorz.matrix_multiplier.utils.MatrixFileIO;

import java.io.IOException;

@RegisterStorage(PCJMatrixMultiplierImpl.Shared.class)
public class PCJMatrixMultiplierImpl implements StartPoint {

    private int N;

    @Storage
    enum Shared {
        A, B, N
    }

    private int[][] A, B;
    private int rank, row, col;

    @Override
    public void main() {
        rank = PCJ.myId();
        String destination = PCJ.getProperty("destination");
        MatrixFileIO.Matrix _A;
        MatrixFileIO.Matrix _B;
        if (rank == 0) {
            System.out.println("Loading data from files.....");
            try {
                _A = MatrixFileIO.readFromFile(PCJ.getProperty("source1"));
                _B = MatrixFileIO.readFromFile(PCJ.getProperty("source2"));

                A = _A.values;
                B = _B.values;

                N = _A.N;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            System.out.println("Initial left shift....");
            for (int i = 0; i < N; i++) {
                for (int shift = 0; shift < i; shift++) {
                    int temp = A[i][0];
                    for (int j = 0; j < N - 1; j++) {
                        A[i][j] = A[i][j + 1];
                    }
                    A[i][N - 1] = temp;
                }
            }

            System.out.println("Initial up shift.....");
            for (int j = 0; j < N; j++) {
                for (int shift = 0; shift < j; shift++) {
                    int temp = B[0][j];
                    for (int i = 0; i < N - 1; i++) {
                        B[i][j] = B[i + 1][j];
                    }
                    B[N - 1][j] = temp;
                }
            }

            System.out.println("Broadcasting data to other processes....");
            PCJ.broadcast(A, Shared.A);
            PCJ.broadcast(B, Shared.B);
            PCJ.broadcast(N, Shared.N);
        }

        PCJ.waitFor(Shared.A);
        PCJ.waitFor(Shared.B);
        PCJ.waitFor(Shared.N);

        row = rank / N;
        col = rank % N;

        PCJ.barrier();

        int C = 0;

        for(int l=0; l<N; l++) {
            C += + (A[row][col] * B[row][col]);
            PCJ.barrier();


            if (rank == 0) {
                // Left shift
                for (int i = 0; i < N; i++) {
                        int temp = A[i][0];
                        for (int j = 0; j < N - 1; j++) {
                            A[i][j] = A[i][j + 1];
                        }
                        A[i][N - 1] = temp;
                }
                PCJ.broadcast(A, Shared.A);

                // Up shift
                for (int j = 0; j < N; j++) {
                        int temp = B[0][j];
                        for (int i = 0; i < N - 1; i++) {
                            B[i][j] = B[i + 1][j];
                        }
                        B[N - 1][j] = temp;
                }
                PCJ.broadcast(B,Shared.B);
            }
            PCJ.waitFor(Shared.A);
            PCJ.waitFor(Shared.B);
            PCJ.barrier();
        }

        PCJ.barrier();
        try {
            MatrixFileIO.writePart(C, destination, row, col);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (rank == 0) {
            System.out.println("Finished");
        }
    }
}
