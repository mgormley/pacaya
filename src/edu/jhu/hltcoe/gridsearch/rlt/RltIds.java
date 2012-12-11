/**
 * 
 */
package edu.jhu.hltcoe.gridsearch.rlt;

import edu.jhu.hltcoe.util.SafeCast;


public class RltIds {
    private long startId;
    public RltIds(int numVars) {
        // Add one to account for the constant var.
        this.startId = numVars + 1;
    }
    /**
     * Get the ID for the RLT variable w_{iIdx,jIdx}.
     */
    public long get(int iIdx, int jIdx) {
        return getFromLongs(iIdx, jIdx);
    }
    
    private long getFromLongs(long iIdx, long jIdx) {
        assert (iIdx < startId - 1 && jIdx < startId - 1);
        long i = Math.max(iIdx, jIdx);
        long j = Math.min(iIdx, jIdx);
        return (startId*i + j) + startId;
    }
    
    public int getI(long id) {
        return SafeCast.safeToInt((id - startId) / startId);
    }
    
    public int getJ(long id) {
        return SafeCast.safeToInt((id - startId) % startId);
    }
}