/**
 * 
 */
package edu.jhu.hltcoe.gridsearch.rlt;

public class RltIds {
    private int startId;
    public RltIds(int numVars) {
        // Add one to account for the constant var.
        this.startId = numVars + 1;
    }
    /**
     * Get the ID for the RLT variable w_{iIdx,jIdx}.
     */
    public int get(int iIdx, int jIdx) {
        assert (iIdx < startId - 1 && jIdx < startId - 1);
        int i = Math.max(iIdx, jIdx);
        int j = Math.min(iIdx, jIdx);
        return (startId*i + j) + startId;
    }
    public int getI(int id) {
        return (id - startId) / startId;
    }
    public int getJ(int id) {
        return (id - startId) % startId;
    }
}