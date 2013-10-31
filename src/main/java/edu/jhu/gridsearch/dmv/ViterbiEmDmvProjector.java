package edu.jhu.gridsearch.dmv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.data.DepTreebank;
import edu.jhu.gridsearch.Projector;
import edu.jhu.gridsearch.RelaxedSolution;
import edu.jhu.gridsearch.Solution;
import edu.jhu.gridsearch.cpt.Projections.ProjectionsPrm.ProjectionType;
import edu.jhu.gridsearch.dmv.BasicDmvProjector.DmvProjectorFactory;
import edu.jhu.gridsearch.dmv.BasicDmvProjector.DmvProjectorPrm;
import edu.jhu.model.dmv.CopyingDmvModelFactory;
import edu.jhu.model.dmv.DmvMStep;
import edu.jhu.model.dmv.DmvModel;
import edu.jhu.model.dmv.DmvModelFactory;
import edu.jhu.parse.DepParser;
import edu.jhu.parse.dmv.DmvCkyParser;
import edu.jhu.prim.Primitives;
import edu.jhu.train.DmvTrainCorpus;
import edu.jhu.train.DmvViterbiEMTrainer;
import edu.jhu.train.DmvViterbiEMTrainer.DmvViterbiEMTrainerPrm;
import edu.jhu.util.Prng;
import edu.jhu.util.math.FastMath;

public class ViterbiEmDmvProjector implements DmvProjector {

    public static class ViterbiEmDmvProjectorPrm implements DmvProjectorFactory {
        public double proportionViterbiImproveTreebank = 0.05;
        public double proportionViterbiImproveModel = 0.05;
        public DmvProjectorPrm dprojPrm = new DmvProjectorPrm();
        // EM Parameters for improving the projected solutions.
        public DmvViterbiEMTrainerPrm vemPrm = new DmvViterbiEMTrainerPrm();
        public ViterbiEmDmvProjectorPrm() {
            vemPrm.emPrm.iterations = 25;        
            vemPrm.emPrm.convergenceRatio = 0.99999;
            vemPrm.emPrm.numRestarts = 0;
            vemPrm.emPrm.timeoutSeconds = Double.POSITIVE_INFINITY;
            vemPrm.lambda = 0.1;
            vemPrm.evaluator = null;
        }
        @Override
        public Projector getInstance(DmvTrainCorpus corpus, DmvRelaxation relax) {
            return new ViterbiEmDmvProjector(this, corpus, relax);
        }
    }
    
    private final class DmvSolutionComparator implements Comparator<DmvSolution> {
        /**
         * This will only return nulls if there are no non-null entries
         */
        @Override
        public int compare(DmvSolution sol1, DmvSolution sol2) {
            if (sol1 == null && sol2 == null) {
                return 0;
            } else if (sol1 == null) {
                return 1;
            } else if (sol2 == null) {
                return -1;
            } else {
                return Double.compare(sol1.getScore(), sol2.getScore());
            }
        }
    }

    private static final Logger log = Logger.getLogger(ViterbiEmDmvProjector.class);

    private ViterbiEmDmvProjectorPrm prm;
    private BasicDmvProjector normProjector;
    private BasicDmvProjector euclidProjector;
    private DmvTrainCorpus corpus;
    private DmvRelaxation relax;

    public ViterbiEmDmvProjector(ViterbiEmDmvProjectorPrm prm, DmvTrainCorpus corpus, DmvRelaxation dwRelax) {
        this.prm = prm;
        
        DmvProjectorPrm prm1 = new DmvProjectorPrm();
        prm1.rootBounds = prm.dprojPrm.rootBounds;
        prm1.projPrm.tempDir = prm.dprojPrm.projPrm.tempDir ;
        prm1.projPrm.lambdaSmoothing =  prm.dprojPrm.projPrm.lambdaSmoothing;
        prm1.projPrm.type =  ProjectionType.NORMALIZE;
        normProjector = new BasicDmvProjector(prm1, corpus);
        
        DmvProjectorPrm prm2 = new DmvProjectorPrm();
        prm2.rootBounds = prm.dprojPrm.rootBounds;
        prm2.projPrm.tempDir = prm.dprojPrm.projPrm.tempDir ;
        prm2.projPrm.lambdaSmoothing =  prm.dprojPrm.projPrm.lambdaSmoothing;
        prm2.projPrm.type =  ProjectionType.UNBOUNDED_MIN_EUCLIDEAN;
        euclidProjector = new BasicDmvProjector(prm2, corpus);

        this.corpus = corpus;
        this.relax = dwRelax;
    }
    
    @Override
    public Solution getProjectedSolution(RelaxedSolution relaxSol) {
        return getProjectedDmvSolution((DmvRelaxedSolution) relaxSol);
    }

    public DmvSolution getProjectedDmvSolution(DmvRelaxedSolution relaxSol) {
        if (relaxSol == null) {
            throw new IllegalStateException("No relaxed solution cached.");
        }
        List<DmvSolution> solutions = new ArrayList<DmvSolution>();

        // Add the null solution, so that the collection isn't empty.
        
        // TODO: bug fix: this pair of projectors is projecting the parse twice!
        DmvSolution normSol = normProjector.getProjectedDmvSolution(relaxSol);
        log.debug("Normalized projected solution score: " + safeGetScore(normSol));
        solutions.add(normSol);
        
        DmvSolution euclidSol = euclidProjector.getProjectedDmvSolution(relaxSol);
        log.debug("Euclidean projected solution score: " + safeGetScore(euclidSol));
        solutions.add(euclidSol);
        
        DmvSolution projectedSol = Collections.max(solutions, new DmvSolutionComparator());
        if (projectedSol != null) {
            // TODO: These solutions might not be feasible according to the
            // root bounds.
            // TODO: Decide on a better heuristic for when to do this 
            // (e.g. depth > dwRelax.getIdm().getNumTotalParams())

            // Run Viterbi EM starting from the randomly rounded solution.
            if (Prng.nextDouble() < prm.proportionViterbiImproveTreebank) {
                DmvSolution vemTreesSol = getImprovedSol(projectedSol.getTreebank());
                log.debug("VEM on treebank score: " + safeGetScore(vemTreesSol));
                solutions.add(vemTreesSol);
            }
            if (Prng.nextDouble() < prm.proportionViterbiImproveModel) {
                DmvSolution vemModelSol = getImprovedSol(projectedSol.getLogProbs(), projectedSol.getIdm());
                log.debug("VEM on model score: " + safeGetScore(vemModelSol));
                solutions.add(vemModelSol);
            }
        }

        return Collections.max(solutions, new DmvSolutionComparator());
    }    

    private String safeGetScore(DmvSolution projectedSol) {
        if (projectedSol == null) {
            return "";
        }
        return Double.toString(projectedSol.getScore());        
    }

    private DmvSolution getImprovedSol(double[][] logProbs, IndexedDmvModel idm) {
        double lambda = 1e-6;
        // TODO: this is a slow conversion
        DmvModel model = idm.getDmvModel(logProbs);
        // We must smooth the weights so that there exists some valid parse
        model.logAddConstant(FastMath.log(lambda));
        model.logNormalize();
        DmvModelFactory modelFactory = new CopyingDmvModelFactory(model);
        return runViterbiEmHelper(modelFactory);
    }
    
    private DmvSolution getImprovedSol(DepTreebank treebank) {  
        double lambda = prm.vemPrm.lambda;
        // Do one M-step to create a model
        DmvMStep mStep = new DmvMStep(lambda);
        DmvModel model = (DmvModel) mStep.getModel(corpus, treebank);
        DmvModelFactory modelFactory = new CopyingDmvModelFactory(model);
        // Then run Viterbi EM
        return runViterbiEmHelper(modelFactory);
    }

    private DmvSolution runViterbiEmHelper(DmvModelFactory modelFactory) {
        // Run Viterbi EM to improve the projected solution.
        DmvViterbiEMTrainerPrm vemPrm = new DmvViterbiEMTrainerPrm(prm.vemPrm);
        vemPrm.modelFactory = modelFactory;
        DmvViterbiEMTrainer trainer = new DmvViterbiEMTrainer(prm.vemPrm);
        trainer.train(corpus);

        IndexedDmvModel idm = relax.getIdm();
        
        DepTreebank treebank = trainer.getCounts();
        DmvModel dmv = (DmvModel)trainer.getModel();
        if (treebank == null || dmv == null) {
            return null;
        }
        double[][] logProbs = idm.getCmLogProbs(dmv);
        
        // Compute the score for the solution
        double score = relax.computeTrueObjective(logProbs, treebank);
        log.debug("Computed true objective: " + score);
        assert Primitives.equals(score, trainer.getLogLikelihood(), 1e-5) : "difference = " + (score - trainer.getLogLikelihood());
                
        // We let the DmvProblemNode compute the score
        DmvSolution sol = new DmvSolution(logProbs, idm, treebank, score);
        return sol;
    }

}
