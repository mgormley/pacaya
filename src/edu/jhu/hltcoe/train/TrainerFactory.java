package edu.jhu.hltcoe.train;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.model.DmvMStep;
import edu.jhu.hltcoe.model.DmvModelFactory;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.model.DmvModelFactory.RandomWeightGenerator;
import edu.jhu.hltcoe.parse.IlpViterbiCorpusParser;
import edu.jhu.hltcoe.parse.IlpViterbiParser;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.parse.IlpViterbiParser.IlpFormulation;



public class TrainerFactory {

    public static void addOptions(Options options) {
        options.addOption("a", "algorithm", true, "Inference algorithm.");
        options.addOption("i", "iterations", true, "Number of iterations.");
        options.addOption("m", "model", true, "Model.");
        options.addOption("p", "parser", true, "Parser.");
        options.addOption("f", "formulation", true, "ILP formulation for parsing");
        options.addOption("l", "lambda", true, "Value for add-lambda smoothing.");
    }

    public static Trainer getModel(CommandLine cmd) throws ParseException  {

        final String algorithm = cmd.hasOption("algorithm") ? 
        		cmd.getOptionValue("algorithm") : "viterbi";
        final int iterations = cmd.hasOption("iterations") ?
                Integer.parseInt(cmd.getOptionValue("iterations")) : 10;
        final String modelName = cmd.hasOption("model") ? 
                cmd.getOptionValue("model") : "dmv";
        final String parserName = cmd.hasOption("parser") ? 
                cmd.getOptionValue("parser") : "ilp-sentence";
        final IlpFormulation formulation = cmd.hasOption("formulation") ? 
                IlpFormulation.getById(cmd.getOptionValue("formulation")) : IlpFormulation.DP_PROJ;
        final double lambda = cmd.hasOption("lambda") ?
                Double.parseDouble(cmd.getOptionValue("lambda")) : 0.1;
                
        Trainer trainer = null;
        if (algorithm.equals("viterbi")) {
            ViterbiParser parser;
            MStep<DepTreebank> mStep;
            ModelFactory modelFactory;
            
            if (modelName.equals("dmv")) {
                if (parserName.equals("ilp-sentence")) {
                    parser = new IlpViterbiParser(formulation);
                } else if (parserName.equals("ilp-corpus")) {
                    parser = new IlpViterbiCorpusParser(formulation);
                } else {
                    throw new ParseException("Parser not supported: " + parserName);
                }
                mStep = new DmvMStep(lambda);
                modelFactory = new DmvModelFactory(new RandomWeightGenerator());
            } else {
                throw new ParseException("Model not supported: " + modelName);
            }
            
            trainer = new ViterbiTrainer(parser, mStep, modelFactory, iterations);
        } else {
            throw new ParseException("Algorithm not supported: " + algorithm);
        }
        
        return trainer;
    }

}
