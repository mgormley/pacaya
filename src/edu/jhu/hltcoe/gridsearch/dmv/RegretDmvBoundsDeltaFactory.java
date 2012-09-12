package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.gridsearch.dmv.DmvBoundsDelta.Lu;
import edu.jhu.hltcoe.util.IntTuple;
import edu.jhu.hltcoe.util.Utilities;

public class RegretDmvBoundsDeltaFactory implements DmvBoundsDeltaFactory {

    private static Logger log = Logger.getLogger(RegretDmvBoundsDeltaFactory.class);
    private RandomDmvBoundsDeltaFactory randBrancher;

    public RegretDmvBoundsDeltaFactory() {
        this.randBrancher = new RandomDmvBoundsDeltaFactory(true);
    }

    @Override
    public List<DmvBoundsDelta> getDmvBounds(DmvProblemNode node) {
        DmvBounds origBounds = node.getBounds();
        double[][] regret = node.getRegretCm();

        if (regret == null) {
            // Back off to random branching.
            log.warn("Regret not available. Backing off to random.");
            return randBrancher.getDmvBounds(node);
        }
        
        // Don't branch on variables that have bottomed out
        for (int c=0; c<regret.length; c++) {
            for (int m=0; m<regret[c].length; m++) {
                if (!origBounds.canBranch(c,m)) {
                    regret[c][m] = Double.NEGATIVE_INFINITY;
                }
            }
        }
        
        IntTuple max = Utilities.getArgmax(regret);
        int c = max.get(0);
        int m = max.get(1);

        if (c == -1 || m == -1 || regret[c][m] == Double.NEGATIVE_INFINITY) {
            log.warn("Branching bottomed-out at node " + node.getId());
            return Collections.emptyList();
        }
        
        String name = node.getIdm().getName(c, m);
        log.info(String.format("Branching: c=%d m=%d name=%s regret=%f", c, m, name, regret[c][m]));
        assert(regret[c][m] != Double.NEGATIVE_INFINITY);
        
        // TODO: make this an option: split at current value
        // TODO: as is, this is buggy: it will sometimes set the ub to -inf which is lower than the lb
        //RelaxedDmvSolution relaxSol = node.getRelaxedSolution();
        //return splitAtMidPoint(origBounds, c, m, relaxSol.getLogProbs()[c][m]);
        
        return splitHalfProbSpace(origBounds, c, m);
    }

    static List<DmvBoundsDelta> splitHalfProbSpace(DmvBounds origBounds, int c, int m) {
        // Split the current LB-UB probability space in half
        double lb = origBounds.getLb(c, m);
        double ub = origBounds.getUb(c, m);
        double mid = Utilities.logAdd(lb, ub) - Utilities.log(2.0);

        return splitAtMidPoint(origBounds, c, m, mid);
    }
    
    static List<DmvBoundsDelta> splitHalfLogProbSpace(DmvBounds origBounds, int c, int m) {
        // Split the current LB-UB probability space in half
        double lb = origBounds.getLb(c, m);
        double ub = origBounds.getUb(c, m);
        double mid = (lb + ub)/2.0;
        
        return splitAtMidPoint(origBounds, c, m, mid);
    }

    static private List<DmvBoundsDelta> splitAtMidPoint(DmvBounds origBounds, int c, int m, double mid) {
        // e.g. [0.0, 0.5]
        DmvBoundsDelta deltas1 = new DmvBoundsDelta(c, m, Lu.UPPER, mid - origBounds.getUb(c, m));
        // e.g. [0.5, 1.0]
        DmvBoundsDelta deltas2 = new DmvBoundsDelta(c, m, Lu.LOWER, mid - origBounds.getLb(c, m));

        List<DmvBoundsDelta> deltasList = new ArrayList<DmvBoundsDelta>();
        deltasList.add(deltas1);
        deltasList.add(deltas2);
        return deltasList;
    }

}
