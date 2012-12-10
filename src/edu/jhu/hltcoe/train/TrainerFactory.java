package edu.jhu.hltcoe.train;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.eval.DependencyParserEvaluator;
import edu.jhu.hltcoe.gridsearch.BfsComparator;
import edu.jhu.hltcoe.gridsearch.DfsBfcComparator;
import edu.jhu.hltcoe.gridsearch.DmvLazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.NodeOrderer;
import edu.jhu.hltcoe.gridsearch.PqNodeOrderer;
import edu.jhu.hltcoe.gridsearch.cpt.BasicCptBoundsDeltaFactory;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDeltaFactory;
import edu.jhu.hltcoe.gridsearch.cpt.FullStrongVariableSelector;
import edu.jhu.hltcoe.gridsearch.cpt.MidpointVarSplitter;
import edu.jhu.hltcoe.gridsearch.cpt.PseudocostVariableSelector;
import edu.jhu.hltcoe.gridsearch.cpt.RandomVariableSelector;
import edu.jhu.hltcoe.gridsearch.cpt.RegretVariableSelector;
import edu.jhu.hltcoe.gridsearch.cpt.VariableSelector;
import edu.jhu.hltcoe.gridsearch.cpt.VariableSplitter;
import edu.jhu.hltcoe.gridsearch.cpt.LpSumToOneBuilder.CutCountComputer;
import edu.jhu.hltcoe.gridsearch.cpt.LpSumToOneBuilder.LpStoBuilderPrm;
import edu.jhu.hltcoe.gridsearch.cpt.MidpointVarSplitter.MidpointChoice;
import edu.jhu.hltcoe.gridsearch.dmv.BnBDmvTrainer;
import edu.jhu.hltcoe.gridsearch.dmv.DmvDantzigWolfeRelaxation;
import edu.jhu.hltcoe.gridsearch.dmv.DmvRelaxation;
import edu.jhu.hltcoe.gridsearch.dmv.DmvRltRelaxation;
import edu.jhu.hltcoe.gridsearch.dmv.ResDmvDantzigWolfeRelaxation;
import edu.jhu.hltcoe.gridsearch.dmv.DmvDantzigWolfeRelaxation.DmvDwRelaxPrm;
import edu.jhu.hltcoe.gridsearch.dmv.DmvRltRelaxation.DmvRltRelaxPrm;
import edu.jhu.hltcoe.gridsearch.dmv.ResDmvDantzigWolfeRelaxation.ResDmvDwRelaxPrm;
import edu.jhu.hltcoe.gridsearch.randwalk.DepthStratifiedBnbNodeSampler;
import edu.jhu.hltcoe.gridsearch.randwalk.DfsRandChildAtDepthNodeOrderer;
import edu.jhu.hltcoe.gridsearch.randwalk.DfsRandWalkNodeOrderer;
import edu.jhu.hltcoe.gridsearch.randwalk.DepthStratifiedBnbNodeSampler.DepthStratifiedBnbSamplerPrm;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt.RltPrm;
import edu.jhu.hltcoe.gridsearch.rlt.filter.MaxNumRltRowFilter;
import edu.jhu.hltcoe.gridsearch.rlt.filter.RandPropRltRowFilter;
import edu.jhu.hltcoe.ilp.IlpSolverFactory;
import edu.jhu.hltcoe.ilp.IlpSolverFactory.IlpSolverId;
import edu.jhu.hltcoe.lp.CplexPrm;
import edu.jhu.hltcoe.model.FixableModelFactory;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvMStep;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.RandomDmvModelFactory;
import edu.jhu.hltcoe.model.dmv.SupervisedDmvModelFactory;
import edu.jhu.hltcoe.model.dmv.UniformDmvModelFactory;
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
        options.addOption("rx", "relaxation", true, "Relaxation [dw,dw-res,rlt]");
        options.addOption("rx", "maxSimplexIterations", true, "(D-W only) The maximum number of simplex iterations");
        options.addOption("rx", "maxDwIterations", true, "(D-W only) The maximum number of dantzig-wolfe algorithm iterations");
        options.addOption("rx", "maxSetSizeToConstrain", true, "(STO only) The maximum size of sets to contrain to be <= 1.0");
        options.addOption("rx", "maxCutRounds", true, "(D-W only) The maximum number of rounds to add cuts");
        options.addOption("rx", "rootMaxCutRounds", true, "(D-W only) The maximum number of rounds to add cuts for the root node");
        options.addOption("rx", "minSumForCuts", true, "(STO only) The minimum threshold at which to stop adding cuts");
        options.addOption("rx", "maxStoCuts", true, "(STO only) The maximum number of sum-to-one cuts");
        options.addOption("dwt", "dwTempDir", true, "(D-W only) For testing only. The temporary directory to which CPLEX files should be written");
        options.addOption("op", "offsetProb", true, "How much to offset the bounds in probability space from the initial bounds point");
        options.addOption("op", "probOfSkipCm", true, "The probability of not bounding a particular variable");
        options.addOption("op", "timeoutSeconds", true, "The timeout in seconds for training run");
        options.addOption("op", "bnbTimeoutSeconds", true, "[Viterbi-B&B only] The timeout in seconds for branch-and-bound");
        options.addOption("df", "disableFathoming", true, "Disables fathoming in branch-and-bound");
        options.addOption("eo", "envelopeOnly", true, "Whether to use only the convex/concave envelope for the RLT relaxation");
        options.addOption("eo", "rltFilter", true, "RLT filter type [obj-var, prop]");
        options.addOption("eo", "rltInitProp", true, "(prop only) Proportion of initial rows to accept.");
        options.addOption("eo", "rltCutProp", true, "(prop only) Proportion of cut rows to accept.");
        options.addOption("eo", "rltInitMax", true, "(max only) Max number of initial rows to accept.");
        options.addOption("eo", "rltCutMax", true, "(max only) Max number of cut rows to accept.");
        options.addOption("eo", "rltNames", true, "Whether to set RLT variable/constraint names.");
        options.addOption("eo", "addBindingCons", true, "Whether to add binding constraints as factors to RLT.");
    }

    public static Object getTrainer(CommandLine cmd, DepTreebank trainTreebank, DmvModel trueModel) throws ParseException {

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
        final int rootMaxCutRounds = Command.getOptionValue(cmd, "rootMaxCutRounds", maxCutRounds);
        final double minSumForCuts = Command.getOptionValue(cmd, "minSumForCuts", 1.01);
        final int maxStoCuts = Command.getOptionValue(cmd, "maxStoCuts", 1000);
        final String dwTempDir = Command.getOptionValue(cmd, "dwTempDir", "");
        final double offsetProb = Command.getOptionValue(cmd, "offsetProb", 1.0);
        final double probOfSkipCm = Command.getOptionValue(cmd, "probOfSkipCm", 0.0);
        final double timeoutSeconds = Command.getOptionValue(cmd, "timeoutSeconds", Double.POSITIVE_INFINITY);
        final double bnbTimeoutSeconds = Command.getOptionValue(cmd, "bnbTimeoutSeconds", Double.POSITIVE_INFINITY);
        final boolean disableFathoming = Command.getOptionValue(cmd, "disableFathoming", false);
        final boolean envelopeOnly = Command.getOptionValue(cmd, "envelopeOnly", true);
        final String rltFilter = Command.getOptionValue(cmd, "rltFilter", "obj-var");
        final double rltInitProp = Command.getOptionValue(cmd, "rltInitProp", 0.1);
        final double rltCutProp = Command.getOptionValue(cmd, "rltCutProp", 0.1);
        final int rltInitMax = Command.getOptionValue(cmd, "rltInitMax", 10000);
        final int rltCutMax = Command.getOptionValue(cmd, "rltCutMax", 1000);
        final boolean rltNames = Command.getOptionValue(cmd, "rltNames", false);
        final boolean addBindingCons = Command.getOptionValue(cmd, "addBindingCons", false);
        
        if (!modelName.equals("dmv")) {
            throw new ParseException("Model not supported: " + modelName);
        }

        double simplexTimeout = algorithm.equals("viterbi-bnb") ? bnbTimeoutSeconds : timeoutSeconds;
        CplexPrm cplexPrm = new CplexPrm(ilpWorkMemMegs, numThreads, maxSimplexIterations, simplexTimeout);

        LpStoBuilderPrm stoPrm = new LpStoBuilderPrm();
        stoPrm.initCutCountComp = new CutCountComputer();
        stoPrm.maxSetSizeToConstrain = maxSetSizeToConstrain;
        stoPrm.minSumForCuts = minSumForCuts;
        stoPrm.maxStoCuts = maxStoCuts;
        
        DmvRelaxation relax = null;
        if (cmd.hasOption("relaxOnly") || algorithm.equals("bnb") || algorithm.equals("viterbi-bnb")) {
            File dwTemp = dwTempDir.equals("") ? null : new File(dwTempDir);
            if (relaxation.equals("dw")) {
                DmvDwRelaxPrm dwPrm = new DmvDwRelaxPrm();
                dwPrm.tempDir = dwTemp;
                dwPrm.maxCutRounds = maxCutRounds;
                dwPrm.rootMaxCutRounds = rootMaxCutRounds;
                dwPrm.cplexPrm = cplexPrm;
                dwPrm.maxDwIterations = maxDwIterations;
                dwPrm.stoPrm = stoPrm;
                relax = new DmvDantzigWolfeRelaxation(dwPrm);
            } else if (relaxation.equals("dw-res")) {
                ResDmvDwRelaxPrm dwPrm = new ResDmvDwRelaxPrm();
                dwPrm.tempDir = dwTemp;
                dwPrm.maxCutRounds = maxCutRounds;
                dwPrm.rootMaxCutRounds = rootMaxCutRounds;
                dwPrm.cplexPrm = cplexPrm;
                dwPrm.maxDwIterations = maxDwIterations;
                relax = new ResDmvDantzigWolfeRelaxation(dwPrm);
            } else if (relaxation.equals("rlt")) {
                RltPrm rltPrm = new RltPrm();
                rltPrm.nameRltVarsAndCons = false;
                rltPrm.envelopeOnly = envelopeOnly;
                rltPrm.nameRltVarsAndCons = rltNames;    
                
                DmvRltRelaxPrm rrPrm = new DmvRltRelaxPrm();
                rrPrm.tempDir = dwTemp;
                rrPrm.maxCutRounds = maxCutRounds;
                rrPrm.rootMaxCutRounds = rootMaxCutRounds;
                rrPrm.addBindingCons = addBindingCons;
                rrPrm.cplexPrm = cplexPrm;
                rrPrm.rltPrm = rltPrm;
                rrPrm.stoPrm = stoPrm;
                
                rrPrm.objVarFilter = false;
                if (rltFilter.equals("obj-var")) {
                    rrPrm.objVarFilter = true;
                    rltPrm.factorFilter = null;
                    rltPrm.rowFilter = null;
                } else if (rltFilter.equals("prop")) {
                    rltPrm.rowFilter = new RandPropRltRowFilter(rltInitProp, rltCutProp);
                } else if (rltFilter.equals("max")) {
                    rltPrm.rowFilter = new MaxNumRltRowFilter(rltInitMax, rltCutMax);
                } else {
                    throw new ParseException("RLT filter type not supported: " + rltFilter);
                }
                relax = new DmvRltRelaxation(rrPrm);
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
                modelFactory = new UniformDmvModelFactory();
            } else if (initWeights.equals("random")) {
                modelFactory = new RandomDmvModelFactory(lambda);
            } else if (initWeights.equals("supervised")) {
                modelFactory = new SupervisedDmvModelFactory(trainTreebank);
            } else if (initWeights.equals("gold")) {
                modelFactory = new FixableModelFactory(null);
                ((FixableModelFactory)modelFactory).fixModel(trueModel);
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
        
        CptBoundsDeltaFactory brancher = null;
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
            
            brancher =  new BasicCptBoundsDeltaFactory(varSelector, varSplitter);
        }

        NodeOrderer nodeOrderer = null;
        if (nodeOrder.equals("bfs")) {
            nodeOrderer = new PqNodeOrderer(new BfsComparator());
        } else if (nodeOrder.equals("dfs")) {
            nodeOrderer = new PqNodeOrderer(new DfsBfcComparator());
        } else if (nodeOrder.equals("dfs-rand")) {
            nodeOrderer = new DfsRandChildAtDepthNodeOrderer(60);
        } else if (nodeOrder.equals("dfs-randwalk")) {
            nodeOrderer = new DfsRandWalkNodeOrderer(60);
        }
        
        if (algorithm.equals("viterbi-bnb")) {
            // Use a null evaluator so that the incumbent is not repeatedly printed out.
            LazyBranchAndBoundSolver bnbSolver = new DmvLazyBranchAndBoundSolver(epsilon, nodeOrderer, bnbTimeoutSeconds, null);
            bnbSolver.setDisableFathoming(disableFathoming);
            trainer = new LocalBnBDmvTrainer(viterbiTrainer, bnbSolver, brancher, relax, numRestarts,
                    offsetProb, probOfSkipCm, timeoutSeconds, parserEvaluator);
        } else if (algorithm.equals("bnb")) {
            LazyBranchAndBoundSolver bnbSolver;
            if (false && disableFathoming) {
                DepthStratifiedBnbSamplerPrm prm = new DepthStratifiedBnbSamplerPrm();
                prm.maxDepth = 60;
                bnbSolver = new DepthStratifiedBnbNodeSampler(prm, timeoutSeconds, parserEvaluator);
            } else {
                bnbSolver = new DmvLazyBranchAndBoundSolver(epsilon, nodeOrderer, timeoutSeconds, parserEvaluator);
                bnbSolver.setDisableFathoming(disableFathoming);
            }
            trainer = new BnBDmvTrainer(bnbSolver, brancher, relax);
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
