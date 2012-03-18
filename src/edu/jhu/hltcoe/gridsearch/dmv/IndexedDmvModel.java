package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.model.dmv.DmvModel;

public class IndexedDmvModel extends DmvModel {

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
    public int getSolValue(DmvSolution initFeasSol, int s, int i) {
        // TODO Auto-generated method stub
        return 0;
    }

}
