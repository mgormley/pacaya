package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelConverter;
import edu.jhu.hltcoe.train.DmvTrainCorpus;

public class DmvObjective {
    
    private DmvTrainCorpus corpus;
    private IndexedDmvModel idm;
    
    
    public DmvObjective(DmvTrainCorpus corpus) {
        this.corpus = corpus;
        this.idm = new IndexedDmvModel(corpus);
    }
    
    public double computeTrueObjective(double[][] logProbs, DepTreebank treebank) {
        double score = 0.0;
        for (int s = 0; s < corpus.size(); s++) {
            Sentence sentence = corpus.getSentence(s);
            DepTree tree = treebank.get(s);
            int[] sentSol = idm.getSentSol(sentence, s, tree);
            for (int i=0; i<sentSol.length; i++) {
                int c = idm.getC(s, i);
                int m = idm.getM(s, i);
                if (sentSol[i] != 0) {
                    // This if-statement is to ensure that 0 * -inf == 0.
                    score += sentSol[i] * logProbs[c][m];
                }
                assert (!Double.isNaN(score));
            }
        }
        return score;
    }

    public Double computeTrueObjective(DmvModel model, DepTreebank treebank) {
        double[][] logProbs = idm.getCmLogProbs(DmvModelConverter.getDepProbMatrix(model, corpus.getLabelAlphabet()));
        return computeTrueObjective(logProbs, treebank);
    }

}
