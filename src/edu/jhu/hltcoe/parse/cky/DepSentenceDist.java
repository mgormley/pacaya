package edu.jhu.hltcoe.parse.cky;

import static depparsing.globals.Constants.LEFT;
import static depparsing.globals.Constants.RIGHT;
import depparsing.model.NonterminalMap;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.util.Utilities;

/**
 * MRG: This class was modified to remove dependence on the corpus.
 */
public class DepSentenceDist {
    public final DepInstance depInst;

    // Sentence likelihood
    public double insideRoot;

    // One probability per wordIdx
    public double root[];

    // 1st index = childIdx, 2nd index = parentIdx, 3rd index = valency
    public double child[][][];

    // 1st index = parentIdx, 2nd index = direction, 3rd index = valency,
    // 4th index = choice (0 for stop, 1 for continue)
    public double decision[][][][];

    public final NonterminalMap nontermMap;

    public DepSentenceDist(Sentence sentence, DepProbMatrix depProbMatrix) {
        this(new DepInstance(sentence.getLabelIds()), depProbMatrix);
    }
    
    public DepSentenceDist(DepInstance depInst, DepProbMatrix depProbMatrix) {
        this(depInst, depProbMatrix.nontermMap);
        cacheModel(depProbMatrix);
    }

    public DepSentenceDist(DepInstance depInst, NonterminalMap nontermMap, double[] fracRoot, double[][] fracChild) {
        this(depInst, nontermMap);
        cacheModel(fracRoot, fracChild);
    }

    private DepSentenceDist(DepInstance depInst, NonterminalMap nontermMap) {
        this.depInst = depInst;
        this.nontermMap = nontermMap;
        initSentenceDist();
    }

    /**
     * MRG: This is very convenient since in the D-W we'll care about exactly
     * these weights as opposed to weights for the tags
     * 
     * Copies model parameters to local caches so that we don't have to
     * constantly look up mappings between the indices of words in the sentence
     * and their corresponding POS numbers when computing the posteriors and
     * running inside outside.
     */
    private void cacheModel(DepProbMatrix model) {
        // FIXME -- is this necessary
        int[] i2tag = depInst.postags;
        for (int c = 0; c < i2tag.length; c++) {
            int ctag = i2tag[c];
            root[c] = model.root[ctag];
            for (int p = 0; p < i2tag.length; p++) {
                if (c == p)
                    continue;
                int ptag = i2tag[p];
                int dir = (c < p ? LEFT : RIGHT);
                for (int v = 0; v < model.nontermMap.childValency; v++) {
                    child[c][p][v] = model.child[ctag][ptag][dir][v];
                }
            }
            for (int dir = 0; dir < 2; dir++) {
                for (int v = 0; v < model.nontermMap.decisionValency; v++) {
                    for (int choice = 0; choice < 2; choice++) {
                        decision[c][dir][v][choice] = model.decision[ctag][dir][v][choice];
                    }
                }
            }
        }
    }

    /**
     * Initializes from a fractional parse in order to project back into the
     * feasible region.
     * 
     * @param fracRoot
     */
    private void cacheModel(double[] fracRoot, double[][] fracChild) {
        int[] i2tag = depInst.postags;
        for (int c = 0; c < i2tag.length; c++) {
            root[c] = Utilities.log(fracRoot[c]);
            for (int p = 0; p < i2tag.length; p++) {
                if (c == p)
                    continue;
                if (nontermMap.childValency > 1) {
                    throw new IllegalStateException("childValency > 1 not handled");
                }
                // Oddly: the child field is indexed by child then parent
                // not parent then child as my fracChild var is.
                child[c][p][0] = Utilities.log(fracChild[p][c]);
            }
            for (int dir = 0; dir < 2; dir++) {
                for (int v = 0; v < nontermMap.decisionValency; v++) {
                    for (int choice = 0; choice < 2; choice++) {
                        // Give all of these zero weight
                        decision[c][dir][v][choice] = 0.0;
                    }
                }
            }
        }
    }

    public void initSentenceDist() {
        int numWords = depInst.numWords;

        // Create caches for model probabilities
        // (note root/child/decision indices mean different things here than in
        // the model)
        root = new double[numWords];
        child = new double[numWords][numWords][nontermMap.childValency];
        decision = new double[numWords][2][nontermMap.decisionValency][2];
    }

}
