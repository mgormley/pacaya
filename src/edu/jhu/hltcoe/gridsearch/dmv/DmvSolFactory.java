package edu.jhu.hltcoe.gridsearch.dmv;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.UniformDmvModelFactory;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.train.DmvViterbiEMTrainer;
import edu.jhu.hltcoe.train.TrainCorpus;
import edu.jhu.hltcoe.train.DmvViterbiEMTrainer.DmvViterbiEMTrainerPrm;

public class DmvSolFactory {

    private static final Logger log = Logger.getLogger(DmvSolFactory.class);

    public static class DmvSolFactoryPrm {
        // EM Parameters for getting an initial solution.
        public DmvViterbiEMTrainerPrm vemPrm = new DmvViterbiEMTrainerPrm();
        public DmvSolFactoryPrm() {
            vemPrm.emPrm.iterations = 25;        
            vemPrm.emPrm.convergenceRatio = 0.99999;
            vemPrm.emPrm.numRestarts = 9;
            vemPrm.emPrm.timeoutSeconds = Double.POSITIVE_INFINITY;
            vemPrm.lambda = 0.1;
            vemPrm.evaluator = null;
        }
    }
    
    private DmvSolFactoryPrm prm;
    
    public DmvSolFactory(DmvSolFactoryPrm prm) {
        this.prm = prm;
    }
    
    public DmvSolution getInitFeasSol(TrainCorpus corpus) {
        return getSol((DmvTrainCorpus)corpus, prm.vemPrm);
    }
    
    private static DmvSolution getSol(DmvTrainCorpus corpus, DmvViterbiEMTrainerPrm vemPrm) {
        // TODO: pass this in.
        IndexedDmvModel idm = new IndexedDmvModel(corpus);

        // Run Viterbi EM.
        ViterbiParser parser = new DmvCkyParser();
        DmvModelFactory modelFactory = new UniformDmvModelFactory();
        DmvViterbiEMTrainer trainer = new DmvViterbiEMTrainer(vemPrm, parser, modelFactory);
        trainer.train(corpus);
        
        DepTreebank treebank = trainer.getCounts();
        DmvModel dmv = (DmvModel)trainer.getModel();
        double[][] logProbs = idm.getCmLogProbs(dmv);
        
        // TODO: we used to use the true objective to compute the score. No we just leave it out.
        //        // Compute the score for the solution.
        //        double score = relax.computeTrueObjective(logProbs, treebank);
        //        log.debug("Computed true objective: " + score);
        //        assert Utilities.equals(score, trainer.getLogLikelihood(), 1e-5) : "difference = " + (score - trainer.getLogLikelihood());
        double score = trainer.getLogLikelihood();
        
        return new DmvSolution(logProbs, idm, treebank, score);
    }
    
}
