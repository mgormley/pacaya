package edu.jhu.gridsearch.dmv;

import org.apache.log4j.Logger;

import edu.jhu.gridsearch.cpt.CptBounds;
import edu.jhu.gridsearch.cpt.VariableId;
import edu.jhu.gridsearch.cpt.VariableSelector;
import edu.jhu.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.util.tuple.IntTuple;

public abstract class AbstractScoringVariableSelector implements VariableSelector {

    private static final Logger log = Logger.getLogger(AbstractScoringVariableSelector.class);
                      
    public AbstractScoringVariableSelector() {
    }

    @Override
    public VariableId select(DmvProblemNode node, DmvRelaxation relax, DmvRelaxedSolution relaxSol) {
        double[][] scores = getScores(node, relax);

        // Don't branch on variables that have bottomed out
        CptBounds origBounds = relax.getBounds();
        for (int c=0; c<scores.length; c++) {
            for (int m=0; m<scores[c].length; m++) {
                if (!origBounds.canBranch(Type.PARAM, c, m)) {
                    scores[c][m] = Double.NEGATIVE_INFINITY;
                }
            }
        }
        
        // Get the max, breaking ties randomly.
        IntTuple max = DoubleArrays.getArgmax(scores);
        int c = max.get(0);
        int m = max.get(1);

        if (c == -1 || m == -1 || scores[c][m] == Double.NEGATIVE_INFINITY) {
            log.warn("Branching bottomed-out at node " + node.getId());
            return new VariableId();
        }
        
        String name = relax.getIdm().getName(c, m);
        log.info(String.format("Branching: c=%d m=%d name=%s score=%f", c, m, name, scores[c][m]));
        return new VariableId(c, m);
    }

    protected abstract double[][] getScores(DmvProblemNode node, DmvRelaxation relax);
    
}
