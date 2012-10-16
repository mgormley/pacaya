package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.jhu.hltcoe.gridsearch.dmv.CptBoundsDelta.Lu;
import edu.jhu.hltcoe.gridsearch.dmv.CptBoundsDelta.Type;
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
    public List<CptBoundsDelta> split(CptBounds bounds, VariableId varId) {
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

    public static List<CptBoundsDelta> splitHalfProbSpace(CptBounds origBounds, int c, int m) {
        // Split the current LB-UB probability space in half
        double lb = origBounds.getLb(Type.PARAM, c, m);
        double ub = origBounds.getUb(Type.PARAM, c, m);
        double mid = Utilities.logAdd(lb, ub) - Utilities.log(2.0);
    
        return splitAtMidPoint(origBounds, c, m, mid);
    }

    public static List<CptBoundsDelta> splitHalfLogProbSpace(CptBounds origBounds, int c, int m) {
        // Split the current LB-UB probability space in half
        double lb = origBounds.getLb(Type.PARAM, c, m);
        double ub = origBounds.getUb(Type.PARAM, c, m);
        double mid = (lb + ub)/2.0;
        
        return splitAtMidPoint(origBounds, c, m, mid);
    }

    public static List<CptBoundsDelta> splitAtMidPoint(CptBounds origBounds, int c, int m, double mid) {
        // e.g. [0.5, 1.0]
        CptBoundsDelta lDelta = new CptBoundsDelta(Type.PARAM, c, m, Lu.LOWER, mid - origBounds.getLb(Type.PARAM, c, m));
        // e.g. [0.0, 0.5]
        CptBoundsDelta uDelta = new CptBoundsDelta(Type.PARAM, c, m, Lu.UPPER, mid - origBounds.getUb(Type.PARAM, c, m));
    
        List<CptBoundsDelta> deltasList = new ArrayList<CptBoundsDelta>();
        deltasList.add(lDelta);
        deltasList.add(uDelta);
        return deltasList;
    }

}
