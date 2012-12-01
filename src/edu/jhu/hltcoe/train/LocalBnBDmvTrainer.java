package edu.jhu.hltcoe.train;

import org.apache.log4j.Logger;
import org.jboss.dna.common.statistic.Stopwatch;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.eval.DependencyParserEvaluator;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.cpt.CptBounds;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDeltaFactory;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDeltaList;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Lu;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.hltcoe.gridsearch.dmv.DmvDantzigWolfeRelaxationTest;
import edu.jhu.hltcoe.gridsearch.dmv.DmvProblemNode;
import edu.jhu.hltcoe.gridsearch.dmv.DmvRelaxation;
import edu.jhu.hltcoe.gridsearch.dmv.DmvSolution;
import edu.jhu.hltcoe.gridsearch.dmv.IndexedDmvModel;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.RandomDmvModelFactory;
import edu.jhu.hltcoe.model.dmv.SupervisedDmvModelFactory;
import edu.jhu.hltcoe.model.dmv.UniformDmvModelFactory;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Time;
import edu.jhu.hltcoe.util.Utilities;

public class LocalBnBDmvTrainer implements Trainer<DepTreebank> {

    private static final Logger log = Logger.getLogger(LocalBnBDmvTrainer.class);

    ViterbiTrainer viterbiTrainer;
    private LazyBranchAndBoundSolver bnbSolver;
    private CptBoundsDeltaFactory brancher;
    private DmvRelaxation relax;
    private int numRestarts;
    private double offsetProb;
    private double probOfSkipCm;
    private double incumbentScore;
    private DmvSolution incumbentSolution;
    private double timeoutSeconds;
    private DependencyParserEvaluator evaluator;
    
    public LocalBnBDmvTrainer(ViterbiTrainer viterbiTrainer, LazyBranchAndBoundSolver bnbSolver, CptBoundsDeltaFactory brancher,
            DmvRelaxation relax, int numRestarts, double offsetProb, double probOfSkipCm, 
            double timeoutSeconds, DependencyParserEvaluator evaluator) {
        this.viterbiTrainer = viterbiTrainer;
        this.bnbSolver = bnbSolver;
        this.brancher = brancher;
        this.relax = relax;
        this.numRestarts = numRestarts;
        this.offsetProb = offsetProb;
        this.probOfSkipCm = probOfSkipCm;
        this.timeoutSeconds = timeoutSeconds;
        this.evaluator = evaluator;
    }

    @Override
    public void train(TrainCorpus c) {
        DmvTrainCorpus corpus = (DmvTrainCorpus)c;
        // Initialize
        this.incumbentSolution = null;
        this.incumbentScore = LazyBranchAndBoundSolver.WORST_SCORE;
        IndexedDmvModel idm = new IndexedDmvModel(corpus);
        DmvProblemNode rootNode = new DmvProblemNode(corpus, brancher, relax);

        Stopwatch timer = new Stopwatch();
        timer.start();
        for (int r=0; r<=numRestarts; r++) {
            // Run Viterbi EM with no random restarts.
            viterbiTrainer.train(corpus);
            
            // Construct the solution object.
            DepTreebank treebank = viterbiTrainer.getCounts();
            double[][] logProbs = idm.getCmLogProbs((DmvModel)viterbiTrainer.getModel());
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
	    
	    timer.stop();
            if (Time.totSec(timer) > timeoutSeconds) {
                // Timeout reached.
                break;
            }
	    timer.start();
        }
        evalIncumbent();
        rootNode.end();
    }

    private void evalIncumbent() {
        if (evaluator != null) {
            log.info("Incumbent logLikelihood: " + incumbentScore);
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
        int[][] featCounts = initSol.getFeatCounts(); 
              
        // Adjust bounds
        for (int c=0; c<dw.getIdm().getNumConds(); c++) {
            for (int m=0; m<dw.getIdm().getNumParams(c); m++) {
    
                double newL, newU;
                CptBounds origBounds = dw.getBounds();
                double lb = origBounds.getLb(Type.PARAM, c, m);
                double ub = origBounds.getUb(Type.PARAM, c, m);
                
                if (Prng.nextDouble() < probOfSkipCm) {
                    // Don't constrain this variable
                    newL = CptBounds.DEFAULT_LOWER_BOUND;
                    newU = CptBounds.DEFAULT_UPPER_BOUND;
                } else {
                    // Constrain the bounds to be +/- offsetLogProb from logProbs[c][m]
                    newU = Utilities.logAdd(logProbs[c][m], offsetLogProb);
                    if (newU > CptBounds.DEFAULT_UPPER_BOUND) {
                        newU = CptBounds.DEFAULT_UPPER_BOUND;
                    }
    
                    if (logProbs[c][m] > offsetLogProb) {
                        newL = Utilities.logSubtract(logProbs[c][m], offsetLogProb);                    
                    } else {
                        newL = CptBounds.DEFAULT_LOWER_BOUND;
                    }
                }
                
                double deltU = newU - ub;
                double deltL = newL - lb;
                //double mid = Utilities.logAdd(lb, ub) - Utilities.log(2.0);
                CptBoundsDeltaList deltas1 = new CptBoundsDeltaList(new CptBoundsDelta(Type.PARAM, c, m, Lu.UPPER, deltU));
                CptBoundsDeltaList deltas2 = new CptBoundsDeltaList(new CptBoundsDelta(Type.PARAM, c, m, Lu.LOWER, deltL));
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
                log.debug("l, u = " + dw.getBounds().getLb(Type.PARAM,c, m) + ", " + dw.getBounds().getUb(Type.PARAM,c, m));
            }
        }
    }

    public static DmvSolution getInitSol(InitSol opt, DmvTrainCorpus corpus, DmvRelaxation dw, DepTreebank trainTreebank) {
        return getInitSol(opt, corpus, dw, trainTreebank, null);
    }
    
    public static DmvSolution getInitSol(InitSol opt, DmvTrainCorpus corpus, DmvRelaxation dw, DepTreebank trainTreebank, DmvSolution goldSol) {
        IndexedDmvModel idm;
        if (dw != null) {
            idm = dw.getIdm();
        } else {
            idm = new IndexedDmvModel(corpus);
        }
    
        DmvSolution initBoundsSol;
        if (opt == InitSol.VITERBI_EM) {
            // TODO: hacky to call a test method and Trainer ignore parameters
            initBoundsSol = DmvDantzigWolfeRelaxationTest.getInitFeasSol(corpus);
        } else if (opt == InitSol.GOLD) {
            initBoundsSol = goldSol;
        } else if (opt == InitSol.RANDOM || opt == InitSol.UNIFORM || opt == InitSol.SUPERVISED){
            
            DmvModelFactory modelFactory;
            if (opt == InitSol.RANDOM) {
                modelFactory = new RandomDmvModelFactory(0.00001);
            } else if (opt == InitSol.SUPERVISED) {
                modelFactory = new SupervisedDmvModelFactory(trainTreebank);
            } else {
                modelFactory = new UniformDmvModelFactory();
            }
            DmvModel randModel = modelFactory.getInstance(corpus.getLabelAlphabet());
            double[][] logProbs = idm.getCmLogProbs(randModel);
            ViterbiParser parser = new DmvCkyParser();
            DepTreebank treebank = parser.getViterbiParse(corpus, randModel);
            double score;
            if (dw != null) {
                score = dw.computeTrueObjective(logProbs, treebank);
            } else {
                score = parser.getLastParseWeight();
            }
            initBoundsSol = new DmvSolution(logProbs, idm, treebank, score);            
        } else {
            throw new IllegalStateException("unsupported initialization: " + opt);
        }
        return initBoundsSol;
    }

    public enum InitSol {
        VITERBI_EM("viterbi-em"), 
        GOLD("gold"), 
        RANDOM("random"), 
        UNIFORM("uniform"),
        SUPERVISED("supervised"),
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
