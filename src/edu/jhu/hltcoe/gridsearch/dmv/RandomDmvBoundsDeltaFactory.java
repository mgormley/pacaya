package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.dmv.DmvBoundsDelta.Dir;
import edu.jhu.hltcoe.gridsearch.dmv.DmvBoundsDelta.Lu;
import edu.jhu.hltcoe.gridsearch.dmv.IndexedDmvModel.CM;
import edu.jhu.hltcoe.math.Multinomials;
import edu.jhu.hltcoe.util.Utilities;

public class RandomDmvBoundsDeltaFactory implements DmvBoundsDeltaFactory {

    private double[] freqs;
    private CM[] cms;

    public RandomDmvBoundsDeltaFactory(SentenceCollection sentences) {
        // We could share this IDM computation with Dantzig-Wolfe if desired
        this(sentences, new IndexedDmvModel(sentences));
        
    }
    
    public RandomDmvBoundsDeltaFactory(SentenceCollection sentences, IndexedDmvModel idm) {
        int[][] maxFreqCm = idm.getTotalMaxFreqCm();

        // Restructure the max freqs for efficient sampling
        int numCm = 0;
        for (int c=0; c<maxFreqCm.length; c++) {
            numCm += maxFreqCm[c].length;
        }
        cms = new CM[numCm];
        freqs = new double[numCm];
        
        for (int c=0, i=0; c<maxFreqCm.length; c++) {
            for (int m=0; m<maxFreqCm[c].length; m++) {
                cms[i] = new CM(c,m);
                freqs[i] = maxFreqCm[c][m]; 
                i++;
            }
        }

        // Smooth this distribution by adding on the maximum possible frequency
        double maxFreq = 0;
        for (int i=0; i<freqs.length; i++) {
            if (freqs[i] > maxFreq) {
                maxFreq = freqs[i];
            }
        }
        for (int i=0; i<freqs.length; i++) {
            freqs[i] += maxFreq;
        }
    }

    @Override
    public List<DmvBoundsDelta> getDmvBounds(DmvProblemNode dmvProblemNode) {
        DmvBounds origBounds = dmvProblemNode.getBounds();

        // Choose a model parameter with probability proportional to its 
        // possible occurence in the corpus
        // TODO: it might be better to consider the current frequency of use in
        // the parent node's feasible solution
        // TODO: this could be more efficient if we kept cumulative frequencies
        // and did binary search for the sampling value.
        int cmId = Multinomials.sampleFromProportions(freqs);
        int c = cms[cmId].get1();
        int m = cms[cmId].get2();

        // Split the current LB-UB space in half
        double lb = origBounds.getLb(c, m);
        double ub = origBounds.getUb(c, m);
        double mid = Utilities.logAdd(lb, ub) - Utilities.log(2.0);
        // e.g. [0.0, 0.5]
        DmvBoundsDelta deltas1 = new DmvBoundsDelta(c, m, Lu.UPPER, Dir.SUBTRACT, Utilities.logSubtract(ub, mid));
        // e.g. [0.5, 1.0]
        DmvBoundsDelta deltas2 = new DmvBoundsDelta(c, m, Lu.LOWER, Dir.ADD, Utilities.logSubtract(mid, lb));

        List<DmvBoundsDelta> deltasList = new ArrayList<DmvBoundsDelta>();
        deltasList.add(deltas1);
        deltasList.add(deltas2);
        return deltasList;
    }

}
