package edu.jhu.hltcoe.train;

import org.apache.log4j.Logger;
import org.jboss.dna.common.statistic.Stopwatch;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.eval.DependencyParserEvaluator;
import edu.jhu.hltcoe.gridsearch.BfsComparator;
import edu.jhu.hltcoe.gridsearch.DmvLazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.dmv.DmvBounds;
import edu.jhu.hltcoe.gridsearch.dmv.DmvBoundsDelta;
import edu.jhu.hltcoe.gridsearch.dmv.DmvBoundsDeltaFactory;
import edu.jhu.hltcoe.gridsearch.dmv.DmvDantzigWolfeRelaxationTest;
import edu.jhu.hltcoe.gridsearch.dmv.DmvProblemNode;
import edu.jhu.hltcoe.gridsearch.dmv.DmvRelaxation;
import edu.jhu.hltcoe.gridsearch.dmv.DmvSolution;
import edu.jhu.hltcoe.gridsearch.dmv.IndexedDmvModel;
import edu.jhu.hltcoe.gridsearch.dmv.DmvBoundsDelta.Lu;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelConverter;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvRandomWeightGenerator;
import edu.jhu.hltcoe.model.dmv.DmvUniformWeightGenerator;
import edu.jhu.hltcoe.model.dmv.DmvWeightGenerator;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.parse.pr.DepProbMatrix;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Time;
import edu.jhu.hltcoe.util.Utilities;

public class LocalBnBDmvTrainer implements Trainer {

    private Logger log = Logger.getLogger(LocalBnBDmvTrainer.class);

    ViterbiTrainer viterbiTrainer;
    private LazyBranchAndBoundSolver bnbSolver;
    private DmvBoundsDeltaFactory brancher;
    private DmvRelaxation relax;
    private int numRestarts;
    private double offsetProb;
    private double probOfSkipCm;
    private double incumbentScore;
    private DmvSolution incumbentSolution;
    private double timeoutSeconds;
    private DependencyParserEvaluator evaluator;
    
    public LocalBnBDmvTrainer(ViterbiTrainer viterbiTrainer, double epsilon, DmvBoundsDeltaFactory brancher,
            DmvRelaxation relax, double bnbTimeoutSeconds, int numRestarts, double offsetProb, double probOfSkipCm, 
            double timeoutSeconds, DependencyParserEvaluator evaluator) {
        this.viterbiTrainer = viterbiTrainer;
        this.bnbSolver = new DmvLazyBranchAndBoundSolver(epsilon, new BfsComparator(), bnbTimeoutSeconds, null);
        this.brancher = brancher;
        this.relax = relax;
        this.numRestarts = numRestarts;
        this.offsetProb = offsetProb;
        this.probOfSkipCm = probOfSkipCm;
        this.timeoutSeconds = timeoutSeconds;
        this.evaluator = evaluator;
    }

    @Override
    public void train(SentenceCollection sentences) {
        // Initialize
        this.incumbentSolution = null;
        this.incumbentScore = LazyBranchAndBoundSolver.WORST_SCORE;
        IndexedDmvModel idm = new IndexedDmvModel(sentences);
        DmvProblemNode rootNode = new DmvProblemNode(sentences, brancher, relax);

        Stopwatch timer = new Stopwatch();
        timer.start();
        for (int r=0; r<=numRestarts; r++) {
            // Run Viterbi EM with no random restarts.
            viterbiTrainer.train(sentences);
            
            // Construct the solution object.
            DepTreebank treebank = viterbiTrainer.getCounts();
            DepProbMatrix dpm = DmvModelConverter.getDepProbMatrix((DmvModel)viterbiTrainer.getModel(), sentences.getLabelAlphabet());
            double[][] logProbs = idm.getCmLogProbs(dpm);
            double vemScore = relax.computeTrueObjective(logProbs, treebank);
            DmvSolution vemSol = new DmvSolution(logProbs, idm, treebank, vemScore);
            
            // Update the incumbent solution.
            if (vemScore > incumbentScore) {
                incumbentScore = vemScore;
                incumbentSolution = vemSol;
                evalIncumbent();
            }
            
            // Set bounds on the root node from the resulting solution.
            setBoundsFromInitSol(rootNode.getRelaxation(), vemSol, offsetProb, probOfSkipCm);
            // Add the Viterbi EM solution to the root node.
            rootNode.getRelaxation().addFeasibleSolution(vemSol);
            
            // Run branch-and-bound.
            bnbSolver.runBranchAndBound(rootNode, incumbentSolution, incumbentScore);
            // Clear any cached information.
            rootNode.setAsActiveNode();
            rootNode.clear();
            
            // Update the incumbent solution.
            if (bnbSolver.getIncumbentScore() > incumbentScore) {
                log.info("New incumbent from B&B");
                incumbentScore = bnbSolver.getIncumbentScore();
                incumbentSolution = (DmvSolution) bnbSolver.getIncumbentSolution();
                evalIncumbent();
            }
            
            if (Time.totSec(timer) > timeoutSeconds) {
                // Timeout reached.
                break;
            }
        }
        evalIncumbent();
        rootNode.end();
    }

    private void evalIncumbent() {
        log.info("Incumbent logLikelihood: " + incumbentScore);
        if (evaluator != null) {
            log.info("Incumbent accuracy: " + evaluator.evaluate(incumbentSolution.getTreebank()));
        }
    }
    
    @Override
    public Model getModel() {
        // Create a new DmvModel from these model parameters
        return incumbentSolution.getIdm().getDmvModel(incumbentSolution.getLogProbs());
    }
    
    public static void setBoundsFromInitSol(DmvRelaxation dw, DmvSolution initSol, double offsetProb, double probOfSkipCm) {
        boolean forward = true;
        double offsetLogProb = Utilities.log(offsetProb);
        double[][] logProbs = initSol.getLogProbs();
        
        // Adjust bounds
        for (int c=0; c<dw.getIdm().getNumConds(); c++) {
            for (int m=0; m<dw.getIdm().getNumParams(c); m++) {
    
                double newL, newU;
                DmvBounds origBounds = dw.getBounds();
                double lb = origBounds.getLb(c, m);
                double ub = origBounds.getUb(c, m);
                
                if (Prng.nextDouble() < probOfSkipCm) {
                    // Don't constrain this variable
                    newL = DmvBounds.DEFAULT_LOWER_BOUND;
                    newU = DmvBounds.DEFAULT_UPPER_BOUND;
                } else {
                    // Constrain the bounds to be +/- offsetLogProb from logProbs[c][m]
                    newU = Utilities.logAdd(logProbs[c][m], offsetLogProb);
                    if (newU > DmvBounds.DEFAULT_UPPER_BOUND) {
                        newU = DmvBounds.DEFAULT_UPPER_BOUND;
                    }
    
                    if (logProbs[c][m] > offsetLogProb) {
                        newL = Utilities.logSubtract(logProbs[c][m], offsetLogProb);                    
                    } else {
                        newL = DmvBounds.DEFAULT_LOWER_BOUND;
                    }
                }
                
                double deltU = newU - ub;
                double deltL = newL - lb;
                //double mid = Utilities.logAdd(lb, ub) - Utilities.log(2.0);
                DmvBoundsDelta deltas1 = new DmvBoundsDelta(c, m, Lu.UPPER, deltU);
                DmvBoundsDelta deltas2 = new DmvBoundsDelta(c, m, Lu.LOWER, deltL);
                if (forward) {
                    if (lb <= newU) {
                        dw.forwardApply(deltas1);
                        dw.forwardApply(deltas2);
                    } else {
                        dw.forwardApply(deltas2);
                        dw.forwardApply(deltas1);
                    }
                } else {
                    // TODO: Remove this case or at least handle the ordering properly.
                    dw.reverseApply(deltas1);
                    dw.reverseApply(deltas2);
                }
                System.out.println("l, u = " + dw.getBounds().getLb(c,m) + ", " + dw.getBounds().getUb(c,m));
            }
        }
    }

    public static DmvSolution updateBounds(SentenceCollection sentences, DmvRelaxation dw, InitSol opt, double offsetProb,
            double probOfSkipCm, int numDoubledCms) {
        IndexedDmvModel idm = dw.getIdm();
    
        DmvSolution initBoundsSol;
        if (opt == InitSol.VITERBI_EM) {
            // TODO: hacky to call a test method and Trainer ignore parameters
            initBoundsSol = DmvDantzigWolfeRelaxationTest.getInitFeasSol(sentences);
        } else if (opt == InitSol.GOLD) {
            
            // TODO initSol = goldSol;
            throw new RuntimeException("not implemented");                
            
        } else if (opt == InitSol.RANDOM || opt == InitSol.UNIFORM){
            DmvWeightGenerator weightGen;
            if (opt == InitSol.RANDOM) {
                Prng.seed(System.currentTimeMillis());
                weightGen = new DmvRandomWeightGenerator(0.00001);
            } else {
                weightGen = new DmvUniformWeightGenerator();
            }
            DmvModelFactory modelFactory = new DmvModelFactory(weightGen);
            DmvModel randModel = (DmvModel)modelFactory.getInstance(sentences);
            double[][] logProbs = idm.getCmLogProbs(DmvModelConverter.getDepProbMatrix(randModel, sentences.getLabelAlphabet()));
            ViterbiParser parser = new DmvCkyParser();
            DepTreebank treebank = parser.getViterbiParse(sentences, randModel);
            initBoundsSol = new DmvSolution(logProbs, idm, treebank, dw.computeTrueObjective(logProbs, treebank));            
        } else {
            throw new IllegalStateException("unsupported initialization: " + opt);
        }
    
        if (numDoubledCms > 0) {
            // TODO:
            throw new RuntimeException("not implemented");
        }
        
        LocalBnBDmvTrainer.setBoundsFromInitSol(dw, initBoundsSol, offsetProb, probOfSkipCm);
        
        return initBoundsSol;
    }

    public enum InitSol {
        VITERBI_EM("viterbi-em"), 
        GOLD("gold"), 
        RANDOM("random"), 
        UNIFORM("uniform"),
        NONE("none");
        
        private String id;

        InitSol(String id) {
          this.id = id;
        }

        @Override
        public String toString() {
            return id;
        }
        
        public static InitSol getById(String id) {
            for (InitSol is : values()) {
                if (is.id.equals(id)) {
                    return is;
                }
            }
            throw new IllegalArgumentException("Unrecognized InitSol id: " + id);
        }
    }
}
