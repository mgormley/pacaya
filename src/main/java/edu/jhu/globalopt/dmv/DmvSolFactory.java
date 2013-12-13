package edu.jhu.globalopt.dmv;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.jhu.data.DepTreebank;
import edu.jhu.model.dmv.DmvModel;
import edu.jhu.model.dmv.DmvModelFactory;
import edu.jhu.model.dmv.RandomDmvModelFactory;
import edu.jhu.model.dmv.SupervisedDmvModelFactory;
import edu.jhu.model.dmv.UniformDmvModelFactory;
import edu.jhu.parse.dep.DepParser;
import edu.jhu.parse.dmv.DmvCkyParser;
import edu.jhu.train.SemiSupervisedCorpus;
import edu.jhu.train.dmv.DmvTrainCorpus;
import edu.jhu.train.dmv.DmvTrainerFactory;
import edu.jhu.train.dmv.DmvViterbiEMTrainer;
import edu.jhu.train.dmv.DmvViterbiEMTrainer.DmvViterbiEMTrainerPrm;

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

    private DmvSolFactoryPrm prm;
    
    public DmvSolFactory(DmvSolFactoryPrm prm) {
        this.prm = prm;
    }
    
    public DmvSolution getInitFeasSol(SemiSupervisedCorpus corpus) {
        return getSol((DmvTrainCorpus)corpus);
    }
    
    private DmvSolution getSol(DmvTrainCorpus corpus) {
        // TODO: pass this in.
        IndexedDmvModel idm = new IndexedDmvModel(corpus);

        // Run Viterbi EM.
        DmvViterbiEMTrainer trainer = new DmvViterbiEMTrainer(prm.vemPrm);
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

    // TODO: Remove this since it doesn't fully respect the command line arguments.
    @Deprecated
    public static DmvSolution getInitSol(InitSol opt, DmvTrainCorpus corpus, DmvRelaxation relax, DepTreebank trainTreebank, DmvSolution goldSol) throws ParseException {
        IndexedDmvModel idm;
        if (relax != null) {
            idm = relax.getIdm();
        } else {
            idm = new IndexedDmvModel(corpus);
        }
    
        DmvSolution initBoundsSol;
        if (opt == InitSol.VITERBI_EM) {
            DmvSolFactory initSolFactory = new DmvSolFactory(DmvTrainerFactory.getDmvSolFactoryPrm(null, null));
            initBoundsSol = initSolFactory.getInitFeasSol(corpus);
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
            DepParser parser = new DmvCkyParser();
            DepTreebank treebank = parser.getViterbiParse(corpus, randModel);
            double score;
            if (relax != null) {
                score = relax.computeTrueObjective(logProbs, treebank);
            } else {
                score = parser.getLastParseWeight();
            }
            initBoundsSol = new DmvSolution(logProbs, idm, treebank, score);            
        } else {
            throw new IllegalStateException("unsupported initialization: " + opt);
        }
        return initBoundsSol;
    }
    
}
