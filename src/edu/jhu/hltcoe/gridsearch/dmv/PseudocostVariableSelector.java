package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.gridsearch.ProblemNode;
import edu.jhu.hltcoe.gridsearch.dmv.CptBoundsDelta.Lu;

public class PseudocostVariableSelector extends AbstractScoringVariableSelector implements VariableSelector {
    private static Logger log = Logger.getLogger(PseudocostVariableSelector.class);

    private static final int RELIABILITY_THRESHOLD = 2;
    private int[][][] numObserved;
    private double[][][] deltaSum;
    private double[][] scores;
    private IndexedDmvModel idm;
    private VariableSplitter varSplitter;

    public PseudocostVariableSelector(VariableSplitter varSplitter) {
        this.varSplitter = varSplitter;
    }
    
    @Override
    public double[][] getScores(DmvProblemNode node) {
        if (scores == null) {
            idm = node.getIdm();

            scores = new double[idm.getNumConds()][];
            deltaSum = new double[idm.getNumConds()][][];
            numObserved = new int[idm.getNumConds()][][];
            
            for (int c = 0; c < idm.getNumConds(); c++) {
                scores[c] = new double[idm.getNumParams(c)];
                deltaSum[c] = new double[idm.getNumParams(c)][2];
                numObserved[c] = new int[idm.getNumParams(c)][2];
            }
        }
        
        // Initialize unreliable pseudocost values with strong branching.
        CptBounds origBounds = node.getBounds();
        double parentBound = node.getOptimisticBound();
        for (int c = 0; c < idm.getNumConds(); c++) {
            for (int m = 0; m < idm.getNumParams(c); m++) {
                if (numObserved[c][m][0] < RELIABILITY_THRESHOLD || numObserved[c][m][1] < RELIABILITY_THRESHOLD) {
                    node.setAsActiveNode();
                    List<CptBoundsDelta> deltas = varSplitter.split(origBounds, new VariableId(c, m));
                    List<ProblemNode> children = node.branch(deltas);
                    assert(children.size() == 2);
                    for (int lu = 0; lu < 2; lu++) {
                        if (numObserved[c][m][lu] < RELIABILITY_THRESHOLD) {
                            DmvProblemNode child = (DmvProblemNode)children.get(lu);
                            assert(child.getDelta().getLu().getAsInt() == lu);
                            child.setAsActiveNode();
                            double cBound = child.getOptimisticBound();
                            double cDelta = parentBound - cBound;
                            deltaSum[c][m][lu] += cDelta;
                            numObserved[c][m][lu]++;

                            String name = node.getIdm().getName(c, m);
                            log.trace(String.format("Probing: c=%d m=%d lu=%d name=%s diff=%f", c, m, lu, name, cDelta));
                        }
                    }
                    updateScore(c, m);
                    node.setAsActiveNode();
                }
            }
        }
        
        // Update the pseudocosts with the current node.
        CptBoundsDelta dmvBoundsDelta = node.getDelta();
        if (dmvBoundsDelta != null) {
            int c = dmvBoundsDelta.getC();
            int m = dmvBoundsDelta.getM();
            int lu = dmvBoundsDelta.getLu().getAsInt();
            // Since we're doing maximization...
            deltaSum[c][m][lu] += node.getParent().getOptimisticBound() - node.getOptimisticBound();
            numObserved[c][m][lu]++;
            updateScore(c, m);
        }
        
        return scores;
    }

    private void updateScore(int c, int m) {
        double lDelta = deltaSum[c][m][Lu.LOWER.getAsInt()] / numObserved[c][m][Lu.LOWER.getAsInt()];
        double uDelta = deltaSum[c][m][Lu.UPPER.getAsInt()] / numObserved[c][m][Lu.UPPER.getAsInt()];
        // The product score used in SCIP. See Eq (5.2) in Tobias Achterberg's thesis.
        scores[c][m] = FullStrongVariableSelector.computeScore(lDelta, uDelta);
    }

}
