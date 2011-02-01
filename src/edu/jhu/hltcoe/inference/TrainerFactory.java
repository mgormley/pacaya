package edu.jhu.hltcoe.inference;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;



public class TrainerFactory {

    public static void addOptions(Options options) {
        options.addOption("i", "inference", true, "Inference algorithm.");
    }

    public static Trainer getModel(CommandLine cmd) throws ParseException  {

        Trainer model = null;
        final String algorithm = cmd.hasOption("inference") ? 
        		cmd.getOptionValue("inference") : "viterbi";
        
        if (algorithm.equals("viterbi")) {
            model = new ViterbiTrainer();
        } else {
            throw new ParseException("Algorithm not supported: " + algorithm);
        }
        
        return model;
    }

}
