package edu.jhu.hltcoe.gridsearch.cpt;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.hltcoe.gridsearch.dmv.DmvProblemNode;
import edu.jhu.hltcoe.util.IntTuple;
import edu.jhu.hltcoe.util.Utilities;

public class RegretVariableSelector implements VariableSelector {

    private static Logger log = Logger.getLogger(RegretVariableSelector.class);
    private RandomVariableSelector randBrancher;

    public RegretVariableSelector() {
        this.randBrancher = new RandomVariableSelector(true);
    }

    @Override
    public VariableId select(DmvProblemNode node) {
        CptBounds origBounds = node.getBounds();
        double[][] regret = node.getRegretCm();

        if (regret == null) {
            // Back off to random branching.
            log.warn("Regret not available. Backing off to random.");
            return randBrancher.select(node);
        }
        
        // Don't branch on variables that have bottomed out
        for (int c=0; c<regret.length; c++) {
            for (int m=0; m<regret[c].length; m++) {
                if (!origBounds.canBranch(Type.PARAM,c, m)) {
                    regret[c][m] = Double.NEGATIVE_INFINITY;
                }
            }
        }
        
        IntTuple max = Utilities.getArgmax(regret);
        int c = max.get(0);
        int m = max.get(1);

        if (c == -1 || m == -1 || regret[c][m] == Double.NEGATIVE_INFINITY) {
            log.warn("Branching bottomed-out at node " + node.getId());
            return new VariableId();
        }
        
        String name = node.getIdm().getName(c, m);
        log.info(String.format("Branching: c=%d m=%d name=%s regret=%f", c, m, name, regret[c][m]));
        assert(regret[c][m] != Double.NEGATIVE_INFINITY);
        
        // TODO: make this an option: split at current value
        // TODO: as is, this is buggy: it will sometimes set the ub to -inf which is lower than the lb
        //RelaxedDmvSolution relaxSol = node.getRelaxedSolution();
        //return splitAtMidPoint(origBounds, c, m, relaxSol.getLogProbs()[c][m]);
        
        return new VariableId(c, m);
    }

}
