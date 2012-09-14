package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.Projector;
import edu.jhu.hltcoe.gridsearch.RelaxedSolution;
import edu.jhu.hltcoe.gridsearch.Solution;
import edu.jhu.hltcoe.model.dmv.DmvMStep;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelConverter;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvWeightCopier;
import edu.jhu.hltcoe.model.dmv.SmoothedDmvWeightCopier;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.parse.pr.DepProbMatrix;
import edu.jhu.hltcoe.train.ViterbiTrainer;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;

public class ViterbiEmDmvProjector implements Projector {

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

    private static Logger log = Logger.getLogger(ViterbiEmDmvProjector.class);

    private DmvProjector dmvProjector;
    private SentenceCollection sentences;
    private DmvRelaxation dwRelax;
    private DmvSolution initFeasSol;

    public ViterbiEmDmvProjector(SentenceCollection sentences, DmvRelaxation dwRelax, DmvSolution initFeasSol) {
        dmvProjector = new DmvProjector(sentences, dwRelax);
        this.sentences = sentences;
        this.dwRelax = dwRelax;
        this.initFeasSol = initFeasSol;
    }
    
    @Override
    public Solution getProjectedSolution(RelaxedSolution relaxSol) {
        return getProjectedDmvSolution((RelaxedDmvSolution) relaxSol);
    }

    public DmvSolution getProjectedDmvSolution(RelaxedDmvSolution relaxSol) {
        if (relaxSol == null) {
            throw new IllegalStateException("No relaxed solution cached.");
        }
        List<DmvSolution> solutions = new ArrayList<DmvSolution>();
        if (initFeasSol != null) {
            // Only consider this solution once at the root.
            solutions.add(initFeasSol);
            initFeasSol = null;
        }
        
        DmvSolution projectedSol = dmvProjector.getProjectedDmvSolution(relaxSol);
        solutions.add(projectedSol);
        // TODO: These solutions might not be feasible according to the bounds.
        // TODO: Decide on a better heuristic for when to do this (e.g. depth >
        // dwRelax.getIdm().getNumTotalParams())
        double random = Prng.nextDouble();
        double proportionViterbiImprove = 0.1;
        if (random < proportionViterbiImprove) {
            // Run Viterbi EM starting from the randomly rounded solution.
            if (random < proportionViterbiImprove / 2.0) {
                solutions.add(getImprovedSol(sentences, projectedSol.getTreebank()));
            } else {
                solutions.add(getImprovedSol(sentences, projectedSol.getLogProbs(), projectedSol.getIdm()));
            }
        }

        return Collections.max(solutions, new DmvSolutionComparator());
    }
    

    private DmvSolution getImprovedSol(SentenceCollection sentences, double[][] logProbs, IndexedDmvModel idm) {
        double lambda = 1e-6;
        // TODO: this is a slow conversion
        DmvModel model = idm.getDmvModel(logProbs);
        // We must smooth the weights so that there exists some valid parse
        DmvModelFactory modelFactory = new DmvModelFactory(new SmoothedDmvWeightCopier(model, lambda));
        return runViterbiEmHelper(sentences, modelFactory, 0);
    }
    
    private DmvSolution getImprovedSol(SentenceCollection sentences, DepTreebank treebank) {  
        double lambda = 0.1;
        // Do one M-step to create a model
        DmvMStep mStep = new DmvMStep(lambda);
        DmvModel model = (DmvModel) mStep.getModel(treebank);
        DmvModelFactory modelFactory = new DmvModelFactory(new DmvWeightCopier(model));
        // Then run Viterbi EM
        return runViterbiEmHelper(sentences, modelFactory, 0);
    }

    private DmvSolution runViterbiEmHelper(SentenceCollection sentences, 
            DmvModelFactory modelFactory, int numRestarts) {
        // Run Viterbi EM to get a reasonable starting incumbent solution
        int iterations = 25;        
        double lambda = 0.1;
        double convergenceRatio = 0.99999;

        ViterbiParser parser = new DmvCkyParser();
        DmvMStep mStep = new DmvMStep(lambda);
        ViterbiTrainer trainer = new ViterbiTrainer(parser, mStep, modelFactory, iterations, convergenceRatio, numRestarts, Double.POSITIVE_INFINITY, null);
        trainer.train(sentences);
        
        DepTreebank treebank = trainer.getCounts();
        IndexedDmvModel idm = dwRelax.getIdm();
        DepProbMatrix dpm = DmvModelConverter.getDepProbMatrix((DmvModel)trainer.getModel(), sentences.getLabelAlphabet());
        double[][] logProbs = idm.getCmLogProbs(dpm);
        
        // Compute the score for the solution
        double score = dwRelax.computeTrueObjective(logProbs, treebank);
        log.debug("Computed true objective: " + score);
        assert Utilities.equals(score, trainer.getLogLikelihood(), 1e-5) : "difference = " + (score - trainer.getLogLikelihood());
                
        // We let the DmvProblemNode compute the score
        DmvSolution sol = new DmvSolution(logProbs, idm, treebank, score);
        return sol;
    }

}
