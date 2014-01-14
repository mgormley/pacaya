package edu.jhu.induce.train.dmv;

import java.util.Random;

import org.apache.log4j.Logger;

import edu.jhu.data.DepTreebank;
import edu.jhu.eval.DependencyParserEvaluator;
import edu.jhu.globalopt.LazyBranchAndBoundSolver;
import edu.jhu.globalopt.Projector;
import edu.jhu.globalopt.LazyBranchAndBoundSolver.LazyBnbSolverFactory;
import edu.jhu.globalopt.LazyBranchAndBoundSolver.LazyBnbSolverPrm;
import edu.jhu.globalopt.cpt.BasicCptBoundsDeltaFactory;
import edu.jhu.globalopt.cpt.CptBounds;
import edu.jhu.globalopt.cpt.CptBoundsDelta;
import edu.jhu.globalopt.cpt.CptBoundsDeltaFactory;
import edu.jhu.globalopt.cpt.CptBoundsDeltaList;
import edu.jhu.globalopt.cpt.MidpointVarSplitter;
import edu.jhu.globalopt.cpt.RegretVariableSelector;
import edu.jhu.globalopt.cpt.CptBoundsDelta.Lu;
import edu.jhu.globalopt.cpt.CptBoundsDelta.Type;
import edu.jhu.globalopt.cpt.MidpointVarSplitter.MidpointChoice;
import edu.jhu.globalopt.dmv.DmvProblemNode;
import edu.jhu.globalopt.dmv.DmvRelaxation;
import edu.jhu.globalopt.dmv.DmvSolFactory;
import edu.jhu.globalopt.dmv.DmvSolution;
import edu.jhu.globalopt.dmv.IndexedDmvModel;
import edu.jhu.globalopt.dmv.BasicDmvProjector.DmvProjectorFactory;
import edu.jhu.globalopt.dmv.DmvDantzigWolfeRelaxation.DmvDwRelaxPrm;
import edu.jhu.globalopt.dmv.DmvDantzigWolfeRelaxation.DmvRelaxationFactory;
import edu.jhu.globalopt.dmv.DmvSolFactory.DmvSolFactoryPrm;
import edu.jhu.globalopt.dmv.ViterbiEmDmvProjector.ViterbiEmDmvProjectorPrm;
import edu.jhu.induce.model.Model;
import edu.jhu.induce.model.dmv.DmvModel;
import edu.jhu.induce.train.SemiSupervisedCorpus;
import edu.jhu.induce.train.Trainer;
import edu.jhu.induce.train.dmv.DmvViterbiEMTrainer.DmvViterbiEMTrainerPrm;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.Prng;
import edu.jhu.util.Timer;

public class LocalBnBDmvTrainer implements Trainer<DepTreebank> {

    private static final Logger log = Logger.getLogger(LocalBnBDmvTrainer.class);

    // TODO: Add defaults.
    public static class LocalBnBDmvTrainerPrm {
        public DmvViterbiEMTrainer viterbiTrainer = new DmvViterbiEMTrainer(new DmvViterbiEMTrainerPrm()); // TODO: switch to just the prm, not the trainer.
        public CptBoundsDeltaFactory brancher = new BasicCptBoundsDeltaFactory(new RegretVariableSelector(), new MidpointVarSplitter(MidpointChoice.HALF_PROB));
        public LazyBnbSolverFactory bnbSolverFactory = new LazyBnbSolverPrm();
        public DmvRelaxationFactory relaxFactory = new DmvDwRelaxPrm(); 
        public DmvProjectorFactory projectorFactory = new ViterbiEmDmvProjectorPrm();
        
        public int numRestarts = 10;
        public double offsetProb = 0.1;
        public double probOfSkipCm = 0.1;
        public double timeoutSeconds = 1e+75;
        public DependencyParserEvaluator evaluator = null;
        public DmvSolFactoryPrm initSolPrm = new DmvSolFactoryPrm();

        public LocalBnBDmvTrainerPrm() { }
        
        public LocalBnBDmvTrainerPrm(DmvViterbiEMTrainer viterbiTrainer, LazyBnbSolverFactory bnbSolverFactory, CptBoundsDeltaFactory brancher,
                DmvRelaxationFactory relaxFactory, DmvProjectorFactory projectorFactory, int numRestarts, double offsetProb, double probOfSkipCm, 
                double timeoutSeconds, DependencyParserEvaluator evaluator, DmvSolFactoryPrm initSolPrm) {
            this.viterbiTrainer = viterbiTrainer;
            this.brancher = brancher;
            this.bnbSolverFactory = bnbSolverFactory;
            this.relaxFactory = relaxFactory;
            this.projectorFactory = projectorFactory;
            this.numRestarts = numRestarts;
            this.offsetProb = offsetProb;
            this.probOfSkipCm = probOfSkipCm;
            this.timeoutSeconds = timeoutSeconds;
            this.evaluator = evaluator;
            this.initSolPrm = initSolPrm;
        }
    }

    public LazyBranchAndBoundSolver bnbSolver = null;
    public DmvRelaxation relax = null;
    private double incumbentScore;
    private DmvSolution incumbentSolution;
    
    private LocalBnBDmvTrainerPrm prm;
    
    public LocalBnBDmvTrainer(LocalBnBDmvTrainerPrm prm) {
        this.prm = prm;
    }

    @Override
    public void train(SemiSupervisedCorpus c) {
        DmvTrainCorpus corpus = (DmvTrainCorpus)c;

        // Get initial solution for B&B and (sometimes) the relaxation.
        DmvSolFactory dsf = new DmvSolFactory(prm.initSolPrm);
        this.incumbentSolution =  dsf.getInitFeasSol(corpus);
        this.incumbentScore = incumbentSolution.getScore();
        log.info("Initial solution score: " + incumbentSolution.getScore());

//      DmvRelaxation relax = (DmvRelaxation) prm.bnbSolver.getRelaxation();
//      prm.relax.init1(corpus);
//      prm.relax.init2(incumbentSolution);
        relax = prm.relaxFactory.getInstance(corpus, incumbentSolution);
        Projector projector = prm.projectorFactory.getInstance(corpus, relax);
        bnbSolver = prm.bnbSolverFactory.getInstance(relax, projector);
        
        // Initialize B&B.
        IndexedDmvModel idm = new IndexedDmvModel(corpus);
        DmvProblemNode rootNode = new DmvProblemNode(prm.brancher);
        
        Timer timer = new Timer();
        timer.start();
        for (int r=0; r<=prm.numRestarts; r++) {
            // Run Viterbi EM with no random restarts.
            prm.viterbiTrainer.train(corpus);
            
            // Construct the solution object.
            DepTreebank treebank = prm.viterbiTrainer.getCounts();
            double[][] logProbs = idm.getCmLogProbs((DmvModel)prm.viterbiTrainer.getModel());
            double vemScore = relax.computeTrueObjective(logProbs, treebank);
            DmvSolution vemSol = new DmvSolution(logProbs, idm, treebank, vemScore);
            
            // Update the incumbent solution.
            if (vemScore > incumbentScore) {
                incumbentScore = vemScore;
                incumbentSolution = vemSol;
                evalIncumbent();
            }
            
            // Set bounds on the root node from the resulting solution.
            setBoundsFromInitSol(relax, vemSol, prm.offsetProb, prm.probOfSkipCm);
            // Add the Viterbi EM solution to the root node.
            relax.addFeasibleSolution(vemSol);
            
            // Run branch-and-bound.
            bnbSolver.runBranchAndBound(rootNode, incumbentSolution, incumbentScore);
            // Clear any cached information.
            rootNode.clear();
            
            // Update the incumbent solution.
            if (bnbSolver.getIncumbentScore() > incumbentScore) {
                log.info("New incumbent from B&B");
                incumbentScore = bnbSolver.getIncumbentScore();
                incumbentSolution = (DmvSolution) bnbSolver.getIncumbentSolution();
                evalIncumbent();
            }
	    
	    timer.stop();
            if (timer.totSec() > prm.timeoutSeconds) {
                // Timeout reached.
                break;
            }
	    timer.start();
        }
        evalIncumbent();
        relax.end();
    }

    private void evalIncumbent() {
        if (prm.evaluator != null) {
            log.info("Incumbent logLikelihood: " + incumbentScore);
            log.info("Incumbent accuracy: " + prm.evaluator.evaluate(incumbentSolution.getTreebank()));
        }
    }
    
    @Override
    public Model getModel() {
        // Create a new DmvModel from these model parameters
        return incumbentSolution.getIdm().getDmvModel(incumbentSolution.getLogProbs());
    }
    
    public static void setBoundsFromInitSol(DmvRelaxation dw, DmvSolution initSol, double offsetProb, double probOfSkipCm) {
        boolean forward = true;
        double offsetLogProb = FastMath.log(offsetProb);
        double[][] logProbs = initSol.getLogProbs();
        int[][] featCounts = initSol.getFeatCounts(); 
              
        // We need a separate PRNG here so that the bounds are consistent across different methods.
        Random rand = new Random(Prng.seed);
        
        // Adjust bounds
        for (int c=0; c<dw.getIdm().getNumConds(); c++) {
            for (int m=0; m<dw.getIdm().getNumParams(c); m++) {
    
                double newL, newU;
                CptBounds origBounds = dw.getBounds();
                double lb = origBounds.getLb(Type.PARAM, c, m);
                double ub = origBounds.getUb(Type.PARAM, c, m);
                
                if (rand.nextDouble() < probOfSkipCm) {
                    // Don't constrain this variable
                    newL = CptBounds.DEFAULT_LOWER_BOUND;
                    newU = CptBounds.DEFAULT_UPPER_BOUND;
                } else {
                    // Constrain the bounds to be +/- offsetLogProb from logProbs[c][m]
                    newU = FastMath.logAdd(logProbs[c][m], offsetLogProb);
                    if (newU > CptBounds.DEFAULT_UPPER_BOUND) {
                        newU = CptBounds.DEFAULT_UPPER_BOUND;
                    }
    
                    if (logProbs[c][m] > offsetLogProb) {
                        newL = FastMath.logSubtract(logProbs[c][m], offsetLogProb);                    
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
                log.debug(String.format("Updated bounds: %s = [%f, %f]", dw.getIdm().getName(c, m), dw.getBounds().getLb(Type.PARAM,c, m), dw.getBounds().getUb(Type.PARAM,c, m)));
            }
        }
    }
}
