package pl.szyorz.matrix_multiplier;

import org.apache.commons.cli.*;
import org.pcj.ExecutionBuilder;
import org.pcj.PCJ;
import pl.szyorz.matrix_multiplier.pcj.PCJMatrixMultiplierImpl;
import pl.szyorz.matrix_multiplier.threads.MatrixMultiplierThreadRunner;
import pl.szyorz.matrix_multiplier.utils.MatrixFileIO;

import java.io.File;
import java.io.IOException;

public class MatrixMultiplierRunner {
    public static void main(String ...args) throws IOException {
        Options options = new Options();

        Option sourceOption = Option.builder("s")
                .longOpt("source")
                .desc("Provide two source file paths")
                .hasArgs()
                .numberOfArgs(2)
                .valueSeparator(' ')
                .required()
                .build();

        Option destinationOption = Option.builder("d")
                .longOpt("destination")
                .desc("Specify destination path")
                .hasArg()
                .numberOfArgs(1)
                .required()
                .build();
        options.addOption(sourceOption);
        options.addOption(destinationOption);


        options.addOption("f", "nodes-file", true, "File consisting a list of nodes (Only parallel-pcj mode)");
        options.addOption("p", "parallel-pcj", false, "Use PCJ to compute");

        options.addOption("t", "threads", false, "Use JVM threads");
        options.addOption("N", "thread-count", true, "Number of threads to run. Only applicable with --threads. Defaults to 1");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            String[] sourceFiles = cmd.getOptionValues("s");
            String destination = cmd.getOptionValue("d");


            if (cmd.hasOption("p")) {
                System.out.println("Parallel PCJ mode enabled");
                int N = MatrixFileIO.readFromFile(sourceFiles[0]).N;

                ExecutionBuilder pcjBuilder = PCJ.executionBuilder(PCJMatrixMultiplierImpl.class)
                        .addProperty("source1", sourceFiles[0])
                        .addProperty("source2", sourceFiles[1])
                        .addProperty("destination", destination);

                if (cmd.hasOption("f")) {
                    File nodesFile = new File(cmd.getOptionValue("f"));

                    pcjBuilder = pcjBuilder.addNodes(nodesFile);

                }else {
                    String[] nodes = new String[N * N];

                    for (int i = 0; i < N * N; i++) {
                        int port = 8000 + (i % 20);
                        nodes[i] = "localhost:" + port;
                    }

                    pcjBuilder = pcjBuilder.addNodes(nodes);
                }

                pcjBuilder.deploy();
            }

            if (cmd.hasOption("t")) {
                System.out.println("Threads mode enabled");
                int threadCount = Integer.parseInt(cmd.getOptionValue("N", "1")); // Default to 1 if not provided

                MatrixMultiplierThreadRunner runner = new MatrixMultiplierThreadRunner(sourceFiles[0], sourceFiles[1], destination, threadCount);
                runner.multiply();
            }

        } catch (ParseException e) {
            System.out.println("Error parsing arguments: " + e.getMessage());
        }
    }
}
