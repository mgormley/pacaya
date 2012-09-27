package edu.jhu.hltcoe.gridsearch.dmv;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.util.IntTuple;
import edu.jhu.hltcoe.util.Utilities;

public abstract class AbstractScoringVariableSelector implements VariableSelector {

    private static Logger log = Logger.getLogger(AbstractScoringVariableSelector.class);
                      
    public AbstractScoringVariableSelector() {
    }

    @Override
    public VariableId select(DmvProblemNode node) {
        double[][] scores = getScores(node);

        // Don't branch on variables that have bottomed out
        DmvBounds origBounds = node.getBounds();
        for (int c=0; c<scores.length; c++) {
            for (int m=0; m<scores[c].length; m++) {
                if (!origBounds.canBranch(c,m)) {
                    scores[c][m] = Double.NEGATIVE_INFINITY;
                }
            }
        }
        
        // Get the max, breaking ties randomly.
        IntTuple max = Utilities.getArgmax(scores);
        int c = max.get(0);
        int m = max.get(1);

        if (c == -1 || m == -1 || scores[c][m] == Double.NEGATIVE_INFINITY) {
            log.warn("Branching bottomed-out at node " + node.getId());
            return new VariableId();
        }
        
        String name = node.getIdm().getName(c, m);
        log.info(String.format("Branching: c=%d m=%d name=%s score=%f", c, m, name, scores[c][m]));
        return new VariableId(c, m);
    }

    protected abstract double[][] getScores(DmvProblemNode node);
    
}
