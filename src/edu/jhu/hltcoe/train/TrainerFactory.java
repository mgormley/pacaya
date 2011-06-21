package edu.jhu.hltcoe.train;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.ilp.IlpSolverFactory;
import edu.jhu.hltcoe.ilp.IlpSolverFactory.IlpSolverId;
import edu.jhu.hltcoe.model.DmvMStep;
import edu.jhu.hltcoe.model.DmvModelFactory;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.model.DmvModelFactory.RandomWeightGenerator;
import edu.jhu.hltcoe.parse.DeltaGenerator;
import edu.jhu.hltcoe.parse.FactorDeltaGenerator;
import edu.jhu.hltcoe.parse.FixedIntervalDeltaGenerator;
import edu.jhu.hltcoe.parse.IlpFormulation;
import edu.jhu.hltcoe.parse.IlpViterbiParser;
import edu.jhu.hltcoe.parse.IlpViterbiParserWithDeltas;
import edu.jhu.hltcoe.parse.IlpViterbiSentenceParser;
import edu.jhu.hltcoe.parse.ViterbiParser;

public class TrainerFactory {

    public static void addOptions(Options options) {
        options.addOption("a", "algorithm", true, "Inference algorithm.");
        options.addOption("i", "iterations", true, "Number of iterations.");
        options.addOption("m", "model", true, "Model.");
        options.addOption("p", "parser", true, "Parser.");
        options.addOption("d", "deltaGenerator", true, "Delta generator.");
        options.addOption("in", "interval", true, "Only for fixed-interval delta generator.");
        options.addOption("fa", "factor", true, "Only for factor delta generator.");
        options.addOption("nps", "numPerSide", true, "For symmetric delta generators.");
        options.addOption("f", "formulation", true, "ILP formulation for parsing");
        options.addOption("l", "lambda", true, "Value for add-lambda smoothing.");
        options.addOption("t", "threads", true, "Number of threads for parallel impl");
        options.addOption("ilp", "ilpSolver", true, "The ILP solver to use " + IlpSolverId.getIdList());
        options.addOption("ilpwmm", "ilpWorkMemMegs", true, "The working memory allotted for the ILP solver in megabytes");
    }

    public static Trainer getModel(CommandLine cmd) throws ParseException {

        final String algorithm = cmd.hasOption("algorithm") ? cmd.getOptionValue("algorithm") : "viterbi";
        final int iterations = cmd.hasOption("iterations") ? Integer.parseInt(cmd.getOptionValue("iterations")) : 10;
        final String modelName = cmd.hasOption("model") ? cmd.getOptionValue("model") : "dmv";
        final String parserName = cmd.hasOption("parser") ? cmd.getOptionValue("parser") : "ilp-sentence";
        final String deltaGenerator = cmd.hasOption("deltaGenerator") ? cmd.getOptionValue("deltaGenerator") : "fixed";
        final double interval = cmd.hasOption("interval") ? Double.parseDouble(cmd.getOptionValue("interval")) : 0.01;
        final double factor = cmd.hasOption("factor") ? Double.parseDouble(cmd.getOptionValue("factor")) : 1.1;
        final int numPerSide = cmd.hasOption("numPerSide") ? Integer.parseInt(cmd.getOptionValue("numPerSide")) : 2;
        final IlpFormulation formulation = cmd.hasOption("formulation") ? IlpFormulation.getById(cmd
                .getOptionValue("formulation")) : IlpFormulation.DP_PROJ;
        final double lambda = cmd.hasOption("lambda") ? Double.parseDouble(cmd.getOptionValue("lambda")) : 0.1;
        final int numThreads = cmd.hasOption("threads") ? Integer.valueOf(cmd.getOptionValue("threads")) : 2;
        final String ilpSolver = cmd.hasOption("ilpSolver") ? cmd.getOptionValue("ilpSolver") : "cplex";
        final int ilpWorkMemMegs = cmd.hasOption("ilpWorkMemMegs") ? Integer.valueOf(cmd.getOptionValue("ilpWorkMemMegs")) : 512;
        
        Trainer trainer = null;
        if (algorithm.equals("viterbi")) {
            ViterbiParser parser;
            MStep<DepTreebank> mStep;
            ModelFactory modelFactory;

            if (modelName.equals("dmv")) {
                IlpSolverFactory ilpSolverFactory = null;
                if (parserName.equals("ilp-sentence") || parserName.equals("ilp-corpus")
                        || parserName.equals("ilp-deltas")) {
                    ilpSolverFactory = new IlpSolverFactory(IlpSolverId.getById(ilpSolver), numThreads, ilpWorkMemMegs);
                }

                if (parserName.equals("ilp-sentence")) {
                    parser = new IlpViterbiSentenceParser(formulation, ilpSolverFactory);
                } else if (parserName.equals("ilp-corpus")) {
                    parser = new IlpViterbiParser(formulation, ilpSolverFactory);
                } else if (parserName.equals("ilp-deltas")) {
                    DeltaGenerator deltaGen;
                    if (deltaGenerator.equals("fixed-interval")) {
                        deltaGen = new FixedIntervalDeltaGenerator(interval, numPerSide);
                    } else if (deltaGenerator.equals("factor")) {
                        deltaGen = new FactorDeltaGenerator(factor, numPerSide);
                    } else {
                        throw new ParseException("Delta generator not supported: " + deltaGenerator);
                    }
                    parser = new IlpViterbiParserWithDeltas(formulation, ilpSolverFactory, deltaGen);
                } else {
                    throw new ParseException("Parser not supported: " + parserName);
                }
                mStep = new DmvMStep(lambda);
                modelFactory = new DmvModelFactory(new RandomWeightGenerator(lambda));
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
