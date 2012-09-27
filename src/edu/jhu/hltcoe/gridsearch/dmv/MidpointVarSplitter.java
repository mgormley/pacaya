package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.jhu.hltcoe.gridsearch.dmv.DmvBoundsDelta.Lu;
import edu.jhu.hltcoe.util.Utilities;

public class MidpointVarSplitter implements VariableSplitter {

    public enum MidpointChoice {
        HALF_PROB, HALF_LOGPROB
    }

    private MidpointChoice choice;
    
    public MidpointVarSplitter(MidpointChoice choice) {
        this.choice = choice;
    }
    
    @Override
    public List<DmvBoundsDelta> split(DmvBounds bounds, VariableId varId) {
        if (!varId.hasVar()) {
            return Collections.emptyList();
        }
        
        if (choice == MidpointChoice.HALF_PROB) {
            return splitHalfProbSpace(bounds, varId.get(0), varId.get(1));
        } else if (choice == MidpointChoice.HALF_LOGPROB) {
            return splitHalfLogProbSpace(bounds, varId.get(0), varId.get(1));
        } else {
            throw new IllegalStateException("Unknown midpoint choice");
        }
    }

    public static List<DmvBoundsDelta> splitHalfProbSpace(DmvBounds origBounds, int c, int m) {
        // Split the current LB-UB probability space in half
        double lb = origBounds.getLb(c, m);
        double ub = origBounds.getUb(c, m);
        double mid = Utilities.logAdd(lb, ub) - Utilities.log(2.0);
    
        return splitAtMidPoint(origBounds, c, m, mid);
    }

    public static List<DmvBoundsDelta> splitHalfLogProbSpace(DmvBounds origBounds, int c, int m) {
        // Split the current LB-UB probability space in half
        double lb = origBounds.getLb(c, m);
        double ub = origBounds.getUb(c, m);
        double mid = (lb + ub)/2.0;
        
        return splitAtMidPoint(origBounds, c, m, mid);
    }

    public static List<DmvBoundsDelta> splitAtMidPoint(DmvBounds origBounds, int c, int m, double mid) {
        // e.g. [0.5, 1.0]
        DmvBoundsDelta lDelta = new DmvBoundsDelta(c, m, Lu.LOWER, mid - origBounds.getLb(c, m));
        // e.g. [0.0, 0.5]
        DmvBoundsDelta uDelta = new DmvBoundsDelta(c, m, Lu.UPPER, mid - origBounds.getUb(c, m));
    
        List<DmvBoundsDelta> deltasList = new ArrayList<DmvBoundsDelta>();
        deltasList.add(lDelta);
        deltasList.add(uDelta);
        return deltasList;
    }

}
