package pl.szyorz.matrix_multiplier.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MatrixFileIO {
    public static Matrix readFromFile(String path) throws IOException {
        return readFromFile(Path.of(path));
    }

    public static Matrix readFromFile(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);

        int N = Integer.parseInt(lines.get(0).trim());
        int[][] values = new int[N][N];

        for (int i = 0; i < N; i++) {
            String[] numbers = lines.get(i + 1).trim().split(" ");
            for (int j = 0; j < N; j++) {
                values[i][j] = Integer.parseInt(numbers[j]);
            }
        }

        return new Matrix(N, values);
    }

    public static void writeToFile(Matrix matrix, String path) throws IOException {
        writeToFile(matrix, Path.of(path));
    }

    public static void writeToFile(Matrix matrix, Path path) throws IOException {
        List<String> lines = IntStream.range(0, matrix.N)
                .mapToObj(i -> IntStream.of(matrix.values[i])
                        .mapToObj(String::valueOf)
                        .collect(Collectors.joining(" ")))
                .collect(Collectors.toList());

        lines.add(0, String.valueOf(matrix.N));
        Files.write(path, lines);
    }

    public static void writePart(int[][] value, String destination, int size, int i, int j) throws IOException {
        Path partPath = Path.of(destination + ".part-" + i + "-" + j);
        StringBuilder builder = new StringBuilder();
        builder.append(size).append("\n");
        for (int l=0; l<size; l++) {
            for (int k=0; k<size; k++) {
                builder.append(value[l][k]);
                if(k<size-1) builder.append(" ");
            }
            builder.append("\n");
        }
        Files.writeString(partPath, builder.toString());
    }

    public static class Matrix {
        public final int N;
        public final int[][] values;

        public Matrix(int N, int[][] values) {
            this.N = N;
            this.values = values;
        }
    }
}
