package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.parse.pr.DepInstance;
import edu.jhu.hltcoe.parse.pr.DepSentenceDist;
import edu.jhu.hltcoe.util.Pair;

public class IndexedDmvModel {

    public String getName(int c, int m) {
        // TODO Auto-generated method stub
        return null;
    }

    public int getNumParams(int c) {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getNumConds() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Zero corresponds to the wall token for i and j
     */
    public int getC(int s, int i) {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Zero corresponds to the wall token for i and j
     */
    public int getM(int s, int i) {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getNumSentParams(int sentLen) {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * TODO: Maybe this is in the wrong place?
     */
    public int[] getSentSol(DepTree depTree) {
        // TODO Auto-generated method stub
        return null;
    }

    public DepSentenceDist getDepSentenceDist(Sentence sentence, double[] sentParams) {
        int[] tags = new int[sentence.size()];
        DepInstance depInstance = new DepInstance(tags);
        DepSentenceDist sd = null; //new DepSentenceDist();
        return sd;
    }

    public DmvModel getDmvModel(double[][] modelParams) {
        // TODO Auto-generated method stub
        return null;
    }

}
