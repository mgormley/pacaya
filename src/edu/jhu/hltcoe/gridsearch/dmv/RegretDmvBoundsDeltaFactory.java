package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.gridsearch.dmv.DmvBoundsDelta.Lu;
import edu.jhu.hltcoe.util.IntTuple;
import edu.jhu.hltcoe.util.Utilities;

public class RegretDmvBoundsDeltaFactory implements DmvBoundsDeltaFactory {

    private static Logger log = Logger.getLogger(RegretDmvBoundsDeltaFactory.class);

    public RegretDmvBoundsDeltaFactory() {
        
    }

    @Override
    public List<DmvBoundsDelta> getDmvBounds(DmvProblemNode node) {
        DmvBounds origBounds = node.getBounds();
        RelaxedDmvSolution relaxSol = node.getRelaxedSolution();
        double[][] regret = node.getRegretCm();

        IntTuple max = Utilities.getArgmax(regret);
        int c = max.get(0);
        int m = max.get(1);
        
        String name = node.getIdm().getName(c, m);
        log.info(String.format("Branching: c=%d m=%d name=%s regret=%f", c, m, name, regret[c][m]));
        
        return splitHalfLogProbSpace(origBounds, c, m);
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
