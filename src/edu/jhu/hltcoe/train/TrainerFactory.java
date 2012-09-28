package edu.jhu.hltcoe.train;

import java.io.File;
import java.util.Comparator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.eval.DependencyParserEvaluator;
import edu.jhu.hltcoe.gridsearch.BfsComparator;
import edu.jhu.hltcoe.gridsearch.DfsBfcComparator;
import edu.jhu.hltcoe.gridsearch.ProblemNode;
import edu.jhu.hltcoe.gridsearch.dmv.BasicDmvBoundsDeltaFactory;
import edu.jhu.hltcoe.gridsearch.dmv.BnBDmvTrainer;
import edu.jhu.hltcoe.gridsearch.dmv.DmvBoundsDeltaFactory;
import edu.jhu.hltcoe.gridsearch.dmv.DmvDantzigWolfeRelaxation;
import edu.jhu.hltcoe.gridsearch.dmv.DmvDantzigWolfeRelaxationResolution;
import edu.jhu.hltcoe.gridsearch.dmv.DmvRelaxation;
import edu.jhu.hltcoe.gridsearch.dmv.FullStrongVariableSelector;
import edu.jhu.hltcoe.gridsearch.dmv.MidpointVarSplitter;
import edu.jhu.hltcoe.gridsearch.dmv.PseudocostVariableSelector;
import edu.jhu.hltcoe.gridsearch.dmv.RandomVariableSelector;
import edu.jhu.hltcoe.gridsearch.dmv.RegretVariableSelector;
import edu.jhu.hltcoe.gridsearch.dmv.VariableSelector;
import edu.jhu.hltcoe.gridsearch.dmv.VariableSplitter;
import edu.jhu.hltcoe.gridsearch.dmv.DmvDantzigWolfeRelaxation.CutCountComputer;
import edu.jhu.hltcoe.gridsearch.dmv.MidpointVarSplitter.MidpointChoice;
import edu.jhu.hltcoe.ilp.IlpSolverFactory;
import edu.jhu.hltcoe.ilp.IlpSolverFactory.IlpSolverId;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvMStep;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvRandomWeightGenerator;
import edu.jhu.hltcoe.model.dmv.DmvSupervisedWeightGenerator;
import edu.jhu.hltcoe.model.dmv.DmvUniformWeightGenerator;
import edu.jhu.hltcoe.parse.DeltaGenerator;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.FactorDeltaGenerator;
import edu.jhu.hltcoe.parse.FixedIntervalDeltaGenerator;
import edu.jhu.hltcoe.parse.IlpFormulation;
import edu.jhu.hltcoe.parse.IlpViterbiParser;
import edu.jhu.hltcoe.parse.IlpViterbiParserWithDeltas;
import edu.jhu.hltcoe.parse.IlpViterbiSentenceParser;
import edu.jhu.hltcoe.parse.InitializedIlpViterbiParserWithDeltas;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.util.Command;

/**
 * TODO: Consider switching to annotations
 * http://docs.oracle.com/javase/tutorial/java/javaOO/annotations.html
 * http://tutorials.jenkov.com/java-reflection/annotations.html
 * 
 * @author mgormley
 * 
 */
public class TrainerFactory {

    private static ViterbiParser evalParser = null;

    public static void addOptions(Options options) {
        options.addOption("a", "algorithm", true, "Inference algorithm");
        options.addOption("i", "iterations", true, "Number of iterations");
        options.addOption("i", "convergenceRatio", true, "Convergence ratio");
        options.addOption("nr", "numRestarts", true, "Number of random restarts");
        options.addOption("m", "model", true, "Model");
        options.addOption("p", "parser", true, "Parser");
        options.addOption("d", "initWeights", true, "Method for initializing the weights [uniform, random, supervised]");
        options.addOption("d", "deltaGenerator", true, "Delta generator");
        options.addOption("in", "interval", true, "Only for fixed-interval delta generator");
        options.addOption("fa", "factor", true, "Only for factor delta generator");
        options.addOption("nps", "numPerSide", true, "For symmetric delta generators");
        options.addOption("f", "formulation", true, "ILP formulation for parsing");
        options.addOption("l", "lambda", true, "Value for add-lambda smoothing");
        options.addOption("t", "threads", true, "Number of threads for parallel impl");
        options.addOption("ilp", "ilpSolver", true, "The ILP solver to use " + IlpSolverId.getIdList());
        options.addOption("ilpwmm", "ilpWorkMemMegs", true, "The working memory allotted for the ILP solver in megabytes");
        options.addOption("e", "epsilon", true, "Suboptimality convergence criterion for branch-and-bound");
        options.addOption("vse", "varSelection", true, "Variable selection strategy for branching [full,regret,rand-uniform,rand-weighted]");
        options.addOption("vsp", "varSplit", true, "Variable splitting strategy for branching [half-prob, half-logprob]");
        options.addOption("no", "nodeOrder", true, "Strategy for node selection [bfs, dfs]");
        options.addOption("rx", "relaxation", true, "Relaxation [dw,dw-res,lp]");
        options.addOption("rx", "maxSimplexIterations", true, "(D-W only) The maximum number of simplex iterations");
        options.addOption("rx", "maxDwIterations", true, "(D-W only) The maximum number of dantzig-wolfe algorithm iterations");
        options.addOption("rx", "maxSetSizeToConstrain", true, "(D-W only) The maximum size of sets to contrain to be <= 1.0");
        options.addOption("rx", "maxCutRounds", true, "(D-W only) The maximum number of rounds to add cuts");
        options.addOption("rx", "minSumForCuts", true, "(D-W only) The minimum threshold at which to stop adding cuts");
        options.addOption("dwt", "dwTempDir", true, "(D-W only) For testing only. The temporary directory to which CPLEX files should be written");
        options.addOption("op", "offsetProb", true, "How much to offset the bounds in probability space from the initial bounds point");
        options.addOption("op", "numDoubledCms", true, "How many model parameters around which the bounds should be doubled");
        options.addOption("op", "probOfSkipCm", true, "The probability of not bounding a particular variable");
        options.addOption("op", "timeoutSeconds", true, "The timeout in seconds for training run");
        options.addOption("op", "bnbTimeoutSeconds", true, "[Viterbi-B&B only] The timeout in seconds for branch-and-bound");
    }

    public static Object getTrainer(CommandLine cmd, DepTreebank trainTreebank) throws ParseException {

        final String algorithm = Command.getOptionValue(cmd, "algorithm", "viterbi");
        final int iterations = Command.getOptionValue(cmd, "iterations", 10);
        final double convergenceRatio = Command.getOptionValue(cmd, "convergenceRatio", 0.99999);
        final int numRestarts = Command.getOptionValue(cmd, "numRestarts", 0);
        final String modelName = Command.getOptionValue(cmd, "model", "dmv");
        final String parserName = Command.getOptionValue(cmd, "parser", "ilp-sentence");
        final String initWeights = Command.getOptionValue(cmd, "initWeights", "uniform");
        final String deltaGenerator = Command.getOptionValue(cmd, "deltaGenerator", "fixed");
        final double interval = Command.getOptionValue(cmd, "interval", 0.01);
        final double factor = Command.getOptionValue(cmd, "factor", 1.1);
        final int numPerSide = Command.getOptionValue(cmd, "numPerSide", 2);
        final IlpFormulation formulation = getOptionValue(cmd, "formulation", IlpFormulation.DP_PROJ);
        final double lambda = Command.getOptionValue(cmd, "lambda", 0.1);
        final int numThreads = Command.getOptionValue(cmd, "threads", 2);
        final String ilpSolver = Command.getOptionValue(cmd, "ilpSolver", "cplex");
        final double ilpWorkMemMegs = Command.getOptionValue(cmd, "ilpWorkMemMegs", 512.0);
        final double epsilon = Command.getOptionValue(cmd, "epsilon", 0.1);
        final String varSelection = Command.getOptionValue(cmd, "varSelection", "regret");
        final String varSplit = Command.getOptionValue(cmd, "varSplit", "half-prob");
        final String nodeOrder = Command.getOptionValue(cmd, "nodeOrder", "bfs");
        final String relaxation = Command.getOptionValue(cmd, "relaxation", "dw");
        final int maxSimplexIterations = Command.getOptionValue(cmd, "maxSimplexIterations", 2100000000);
        final int maxDwIterations = Command.getOptionValue(cmd, "maxDwIterations", 1000);
        final int maxSetSizeToConstrain = Command.getOptionValue(cmd, "maxSetSizeToConstrain", 2);
        final int maxCutRounds = Command.getOptionValue(cmd, "maxCutRounds", 100);
        final double minSumForCuts = Command.getOptionValue(cmd, "minSumForCuts", 1.01);
        final String dwTempDir = Command.getOptionValue(cmd, "dwTempDir", "");
        double offsetProb = Command.getOptionValue(cmd, "offsetProb", 1.0);
        double probOfSkipCm = Command.getOptionValue(cmd, "probOfSkipCm", 0.0);
        int numDoubledCms = Command.getOptionValue(cmd, "numDoubledCms", 0);
        double timeoutSeconds = Command.getOptionValue(cmd, "timeoutSeconds", Double.POSITIVE_INFINITY);
        double bnbTimeoutSeconds = Command.getOptionValue(cmd, "bnbTimeoutSeconds", Double.POSITIVE_INFINITY);

        if (!modelName.equals("dmv")) {
            throw new ParseException("Model not supported: " + modelName);
        }

        DmvRelaxation relax = null;
        if (cmd.hasOption("relaxOnly") || algorithm.equals("bnb") || algorithm.equals("viterbi-bnb")) {
            File dwTemp = dwTempDir.equals("") ? null : new File(dwTempDir);
            if (relaxation.equals("dw")) {
                DmvDantzigWolfeRelaxation dw = new DmvDantzigWolfeRelaxation(dwTemp, maxCutRounds,
                        new CutCountComputer());
                dw.setMaxSimplexIterations(maxSimplexIterations);
                dw.setMaxDwIterations(maxDwIterations);
                dw.setMaxSetSizeToConstrain(maxSetSizeToConstrain);
                dw.setMinSumForCuts(minSumForCuts);
                relax = dw;
            } else if (relaxation.equals("dw-res")) {
                DmvDantzigWolfeRelaxationResolution dw = new DmvDantzigWolfeRelaxationResolution(dwTemp);
                dw.setMaxSimplexIterations(maxSimplexIterations);
                dw.setMaxDwIterations(maxDwIterations);
                relax = dw;
            } else if (relaxation.equals("lp")) {
                throw new RuntimeException("LP relaxation not yet implemented");
            } else {
                throw new ParseException("Relaxation not supported: " + relaxation);
            }
            if (cmd.hasOption("relaxOnly")) {
                return relax;
            }
        }

        evalParser = new DmvCkyParser();
        DependencyParserEvaluator parserEvaluator = new DependencyParserEvaluator(evalParser, trainTreebank, "train"); 
        
        Trainer trainer = null;
        ViterbiTrainer viterbiTrainer = null;
        if (algorithm.equals("viterbi") || algorithm.equals("viterbi-bnb")) {
            ViterbiParser parser;
            IlpSolverFactory ilpSolverFactory = null;
            if (parserName.startsWith("ilp-")) {
                IlpSolverId ilpSolverId = IlpSolverId.getById(ilpSolver);
                ilpSolverFactory = new IlpSolverFactory(ilpSolverId, numThreads, ilpWorkMemMegs);
                // TODO: make this an option
                //ilpSolverFactory.setBlockFileWriter(new DeltaParseBlockFileWriter(formulation));
            }

            if (parserName.equals("cky")) {
                parser = new DmvCkyParser();
            } else if (parserName.equals("ilp-sentence")) {
                parser = new IlpViterbiSentenceParser(formulation, ilpSolverFactory);
            } else if (parserName.equals("ilp-corpus")) {
                parser = new IlpViterbiParser(formulation, ilpSolverFactory);
            } else if (parserName.equals("ilp-deltas") || parserName.equals("ilp-deltas-init")) {
                DeltaGenerator deltaGen;
                if (deltaGenerator.equals("fixed-interval")) {
                    deltaGen = new FixedIntervalDeltaGenerator(interval, numPerSide);
                } else if (deltaGenerator.equals("factor")) {
                    deltaGen = new FactorDeltaGenerator(factor, numPerSide);
                } else {
                    throw new ParseException("Delta generator not supported: " + deltaGenerator);
                }
                if (parserName.equals("ilp-deltas")) {
                    parser = new IlpViterbiParserWithDeltas(formulation, ilpSolverFactory, deltaGen);
                } else if (parserName.equals("ilp-deltas-init")) {
                    parser = new InitializedIlpViterbiParserWithDeltas(formulation, ilpSolverFactory, deltaGen,
                            ilpSolverFactory);
                } else {
                    throw new ParseException("Parser not supported: " + parserName);
                }
            } else {
                throw new ParseException("Parser not supported: " + parserName);
            }

            MStep<DepTreebank> mStep;
            mStep = new DmvMStep(lambda);

            ModelFactory modelFactory;
            if (initWeights.equals("uniform")) {
                modelFactory = new DmvModelFactory(new DmvUniformWeightGenerator());
            } else if (initWeights.equals("random")) {
                modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
            } else if (initWeights.equals("supervised")) {
                modelFactory = new DmvModelFactory(new DmvSupervisedWeightGenerator(trainTreebank));
            } else {
                throw new ParseException("initWeights not supported: " + initWeights);
            }

            if (algorithm.equals("viterbi")) {
                trainer = new ViterbiTrainer(parser, mStep, modelFactory, iterations, convergenceRatio, numRestarts,
                        timeoutSeconds, parserEvaluator);
            }
            if (algorithm.equals("viterbi-bnb")) {
                // Use zero random restarts and no timeout for local search.
                viterbiTrainer = new ViterbiTrainer(parser, mStep, modelFactory, iterations, convergenceRatio, 0,
                        Double.POSITIVE_INFINITY, null);
            }
        }
        
        DmvBoundsDeltaFactory brancher = null;
        if (algorithm.equals("bnb") || algorithm.equals("viterbi-bnb")) {
            
            VariableSplitter varSplitter;
            if (varSplit.equals("half-prob")) {
                varSplitter = new MidpointVarSplitter(MidpointChoice.HALF_PROB);
            } else if (varSplit.equals("half-logprob")) {
                varSplitter = new MidpointVarSplitter(MidpointChoice.HALF_LOGPROB);
            } else {
                throw new ParseException("Variable splitting strategy not supported: " + varSplit);
            }

            VariableSelector varSelector;
            if (varSelection.equals("full")) {
                varSelector = new FullStrongVariableSelector(varSplitter);
            } else if (varSelection.equals("pseudocost")) {
                varSelector = new PseudocostVariableSelector(varSplitter);
            } else if (varSelection.equals("regret")) {
                varSelector = new RegretVariableSelector();
            } else if (varSelection.equals("rand-uniform")) {
                varSelector = new RandomVariableSelector(true);
            } else if (varSelection.equals("rand-weighted")) {
                varSelector = new RandomVariableSelector(false);
            } else {
                throw new ParseException("Variable selection strategy not supported: " + varSelection);
            }
            
            brancher =  new BasicDmvBoundsDeltaFactory(varSelector, varSplitter);
        }

        Comparator<ProblemNode> leafComparator = null;
        if (nodeOrder.equals("bfs")) {
            leafComparator = new BfsComparator();
        } else if (nodeOrder.equals("dfs")) {
            leafComparator = new DfsBfcComparator();
        }
        
        if (algorithm.equals("viterbi-bnb")) {
            trainer = new LocalBnBDmvTrainer(viterbiTrainer, epsilon, brancher, relax, bnbTimeoutSeconds, numRestarts,
                    offsetProb, probOfSkipCm, timeoutSeconds, parserEvaluator);
        } else if (algorithm.equals("bnb")) {
            trainer = new BnBDmvTrainer(epsilon, brancher, relax, timeoutSeconds, parserEvaluator, leafComparator);
        }
        
        if (trainer == null) {
            throw new ParseException("Algorithm not supported: " + algorithm);
        }

        return trainer;
    }

    public static IlpFormulation getOptionValue(CommandLine cmd, String name, IlpFormulation defaultValue) {
        return cmd.hasOption(name) ? IlpFormulation.getById(cmd.getOptionValue(name)) : defaultValue;
    }

    /**
     * TODO: This is a bit hacky, but convenient.
     */
    public static ViterbiParser getEvalParser() {
        return evalParser;
    }

}
