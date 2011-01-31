package edu.jhu.hltcoe;

import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class PipelineRunner {

    public PipelineRunner() {
        // TODO:
    }

    public void run(CommandLine cmd) throws ParseException {
        // Read the data
        String dataPath = cmd.getOptionValue("data");
        DataReader dataReader = new DataReader();
        dataReader.loadPath(dataPath);

        // Train the model
        Model model = ModelFactory.getModel(cmd);
        model.train(dataReader.getData());

        // Evaluate the model
        PrintWriter pw = new PrintWriter(System.out);
        if (cmd.hasOption("psuedo-word")) {
            Evaluator pwEval = new PsuedoWordEvaluator();
            pwEval.evaluate(model);
            pwEval.print(pw);
        }
    }

    public static Options createOptions() {
        Options options = new Options();
        
        // Options not specific to the model
        options.addOption("d", "data", true, "Input data.");

        ModelFactory.addOptions(options);
        return options;
    }

    public static void main(String[] args) {
        String usage = "java " + PipelineRunner.class.getName() + " [OPTIONS]";
        CommandLineParser parser = new PosixParser();
        Options options = createOptions();
        String[] requiredOptions = new String[] { "data" };

        CommandLine cmd = null;
        final HelpFormatter formatter = new HelpFormatter();
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e1) {
            formatter.printHelp(usage, options, true);
            System.exit(1);
        }
        for (String requiredOption : requiredOptions) {
            if (!cmd.hasOption(requiredOption)) {
                formatter.printHelp(usage, options, true);
                System.exit(1);
            }
        }

        PipelineRunner pipeline = new PipelineRunner();
        try {
            pipeline.run(cmd);
        } catch (ParseException e1) {
            formatter.printHelp(usage, options, true);
            System.exit(1);
        }
    }

}
