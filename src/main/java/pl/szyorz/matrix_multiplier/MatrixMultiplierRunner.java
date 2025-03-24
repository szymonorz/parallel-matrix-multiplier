package pl.szyorz.matrix_multiplier;

import org.pcj.PCJ;
import pl.szyorz.dummy.DummyData;
import pl.szyorz.matrix_multiplier.pcj.PCJMatrixMultiplierImpl;
import pl.szyorz.matrix_multiplier.threads.MatrixMultiplierThreadRunner;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class MatrixMultiplierRunner {
    public static void main(String ...args) throws IOException, URISyntaxException {
        URL nodeResource = MatrixMultiplierRunner.class.getClassLoader().getResource("nodes.txt");

        MatrixMultiplierThreadRunner runner = new MatrixMultiplierThreadRunner(DummyData.matrix1, DummyData.matrix2, 10);
        runner.multiply();

        PCJ.executionBuilder(PCJMatrixMultiplierImpl.class)
                .addNodes(new File(nodeResource.toURI()))
                .addProperty("key", "value")
                .deploy();
    }
}
