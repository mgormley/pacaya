package edu.jhu.gridsearch.cpt;

import java.util.Arrays;

import org.apache.log4j.Logger;

import edu.jhu.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.gridsearch.dmv.DmvProblemNode;
import edu.jhu.gridsearch.dmv.DmvRelaxation;
import edu.jhu.gridsearch.dmv.IndexedDmvModel;
import edu.jhu.gridsearch.dmv.DmvRelaxedSolution;
import edu.jhu.gridsearch.dmv.IndexedDmvModel.CM;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.arrays.Multinomials;

public class RandomVariableSelector implements VariableSelector {

    private static final Logger log = Logger.getLogger(RandomVariableSelector.class);

    private boolean uniform; 
    private double[] freqs;
    private CM[] cms;
    
    /**
     * @param uniform If true, the sampling will be uniform over the used parameters
     */
    public RandomVariableSelector(boolean uniform) {
        this.uniform = uniform;        
    }

    private void init(IndexedDmvModel idm) {
        int[][] maxFreqCm = idm.getTotUnsupervisedMaxFreqCm();

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
                freqs[i] = uniform && maxFreqCm[c][m] > 0 ? 1.0 : maxFreqCm[c][m]; 
                i++;
            }
        }

        // TODO: remove
        // We don't need this because we're zeroing out unused parameters
//        // Smooth this distribution by adding on the maximum possible frequency
//        double maxFreq = 0;
//        for (int i=0; i<freqs.length; i++) {
//            if (freqs[i] > maxFreq) {
//                maxFreq = freqs[i];
//            }
//        }
//        for (int i=0; i<freqs.length; i++) {
//            freqs[i] += maxFreq;
//        }
    }

    @Override
    public VariableId select(DmvProblemNode node, DmvRelaxation relax, DmvRelaxedSolution relaxSol) {
        if (freqs == null) {
            init(relax.getIdm());
        }
        
        CptBounds origBounds = relax.getBounds();

        // Choose a model parameter with probability proportional to its 
        // possible occurrence in the corpus -- or uniformly from the used parameters.
        // TODO: it might be better to consider the current frequency of use in
        // the parent node's feasible solution
        // TODO: this could be more efficient if we kept cumulative frequencies
        // and did binary search for the sampling value.
        
        int c;
        int m;

        // Only sample from nodes that haven't bottomed out.
        int numNonZeros = 0;
        double[] adjusted = DoubleArrays.copyOf(freqs);
        for (int i = 0; i < adjusted.length; i++) {
            c = cms[i].get1();
            m = cms[i].get2();
            if (!origBounds.canBranch(Type.PARAM, c, m)) {
                adjusted[i] = 0.0;
            } else if (freqs[i] != 0.0) {
                numNonZeros++;
            } else {
                // Do nothing: the value is already 0.
            }
        }
        if (numNonZeros == 0) {
            log.warn("Branching bottomed-out at node " + node.getId());
            return new VariableId();
        }
        
        do {
            int cmId = Multinomials.sampleFromProportions(adjusted);
            c = cms[cmId].get1();
            m = cms[cmId].get2();
        } while(!origBounds.canBranch(Type.PARAM, c, m));

        String name = relax.getIdm().getName(c, m);
        log.info(String.format("Branching: c=%d m=%d name=%s", c, m, name));
        return new VariableId(c, m);
    }

}
