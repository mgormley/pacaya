package edu.jhu.hltcoe.inference;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;



public class TrainerFactory {

    public static void addOptions(Options options) {
        options.addOption("i", "algorithm", true, "Inference algorithm.");
    }

    public static Trainer getModel(CommandLine cmd) throws ParseException  {

        Trainer trainer = null;
        final String algorithm = cmd.hasOption("algorithm") ? 
        		cmd.getOptionValue("algorithm") : "viterbi";
        final String modelName = cmd.hasOption("model") ? 
                cmd.getOptionValue("model") : "dmv";
        
        Model model;
                
        if (algorithm.equals("viterbi")) {
            
            trainer = new ViterbiTrainer<>();
        } else {
            throw new ParseException("Algorithm not supported: " + algorithm);
        }
        
        return model;
    }

}
