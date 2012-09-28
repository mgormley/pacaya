package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelConverter;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.Utilities;

public class DmvObjective {
    
    private static final double EQUALITY_TOLERANCE = 1e-8;
    private DmvTrainCorpus corpus;
    private IndexedDmvModel idm;
    
    
    public DmvObjective(DmvTrainCorpus corpus) {
        this.corpus = corpus;
        this.idm = new IndexedDmvModel(corpus);
    }

    public double computeTrueObjective(double[][] logProbs, double[][] featCounts) {
        double quadObj = 0.0;
        for (int c = 0; c < idm.getNumConds(); c++) {
            for (int m = 0; m < idm.getNumParams(c); m++) {
                if (!Utilities.equals(featCounts[c][m], 0.0, EQUALITY_TOLERANCE)) {
                    quadObj += (logProbs[c][m] * featCounts[c][m]);
                }
                assert (!Double.isNaN(quadObj));
            }
        }
        return quadObj;
    }
    
    public double computeTrueObjective(double[][] logProbs, int[][] featCounts) {
        double quadObj = 0.0;
        for (int c = 0; c < idm.getNumConds(); c++) {
            for (int m = 0; m < idm.getNumParams(c); m++) {
                if (featCounts[c][m] > 0) {
                    quadObj += (logProbs[c][m] * featCounts[c][m]);
                }
                assert (!Double.isNaN(quadObj));
            }
        }
        return quadObj;
    }
    
    public double computeTrueObjective(double[][] logProbs, DepTreebank treebank) {
        int[][] featCounts = idm.getTotFreqCm(treebank);
        
        return computeTrueObjective(logProbs, featCounts);
    }

    public Double computeTrueObjective(DmvModel model, DepTreebank treebank) {
        double[][] logProbs = idm.getCmLogProbs(DmvModelConverter.getDepProbMatrix(model, corpus.getLabelAlphabet()));
        return computeTrueObjective(logProbs, treebank);
    }

}
