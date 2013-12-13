package edu.jhu.globalopt.cpt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.jhu.globalopt.cpt.CptBoundsDelta.Lu;
import edu.jhu.globalopt.cpt.CptBoundsDelta.Type;
import edu.jhu.prim.util.math.FastMath;

public class MidpointVarSplitter implements VariableSplitter {

    public enum MidpointChoice {
        HALF_PROB, HALF_LOGPROB
    }

    private MidpointChoice choice;
    
    public MidpointVarSplitter(MidpointChoice choice) {
        this.choice = choice;
    }
    
    @Override
    public List<CptBoundsDeltaList> split(CptBounds bounds, VariableId varId) {
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

    public static List<CptBoundsDeltaList> splitHalfProbSpace(CptBounds origBounds, int c, int m) {
        // Split the current LB-UB probability space in half
        double lb = origBounds.getLb(Type.PARAM, c, m);
        double ub = origBounds.getUb(Type.PARAM, c, m);
        double mid = FastMath.logAdd(lb, ub) - FastMath.log(2.0);
    
        return splitAtMidPoint(origBounds, c, m, mid);
    }

    public static List<CptBoundsDeltaList> splitHalfLogProbSpace(CptBounds origBounds, int c, int m) {
        // Split the current LB-UB probability space in half
        double lb = origBounds.getLb(Type.PARAM, c, m);
        double ub = origBounds.getUb(Type.PARAM, c, m);
        double mid = (lb + ub)/2.0;
        
        return splitAtMidPoint(origBounds, c, m, mid);
    }

    public static List<CptBoundsDeltaList> splitAtMidPoint(CptBounds origBounds, int c, int m, double mid) {
        // ------- Upper Bound ---------
        // e.g. [0.0, 0.4]
        CptBoundsDelta uDelta = new CptBoundsDelta(Type.PARAM, c, m, Lu.UPPER, mid - origBounds.getUb(Type.PARAM, c, m));
        CptBoundsDeltaList uDeltas = new CptBoundsDeltaList(uDelta);
        
        // Compute log(sum_{n=1}^{M_c} w_cn^{max})
        double logSumUbs = Double.NEGATIVE_INFINITY;
        for (int n = 0; n < origBounds.getNumParams(c); n++) {
            if (n == m) {
                logSumUbs = FastMath.logAdd(logSumUbs, mid);                
            } else {
                logSumUbs = FastMath.logAdd(logSumUbs, origBounds.getUb(Type.PARAM, c, n));
            }
        }
        if (logSumUbs >= 0.0) {
            // Compute the implicit lower bound for each parameter n.
            // w_cm >= log(1.0 - max(sum_{n != m} w_cn^{max}, 1.0))
            for (int n = 0; n < origBounds.getNumParams(c); n++) {
                if (n == m) {
                    continue;
                }
                // Since we already have logSumUbs, we subtract off the contribution of
                // the current parameter w_cn^{max}, to compute the sum relative to this
                // parameter.
                double logSumUbsForN = FastMath.logSubtract(logSumUbs, origBounds.getUb(Type.PARAM, c, n));
                
                if (logSumUbsForN >= 0.0) {
                    // The implicit lower bound will be loose (i.e. negative), 
                    // so don't make any further adjustments.
                    continue;
                }
                
                double newLbForN = FastMath.logSubtract(0.0, logSumUbsForN);
                if (origBounds.getLb(Type.PARAM, c, n) < newLbForN) {
                    // e.g. [0.6, 0.0] implied by sum-to-one constraints
                    uDelta = new CptBoundsDelta(Type.PARAM, c, n, Lu.LOWER, newLbForN - origBounds.getLb(Type.PARAM, c, n));
                    uDeltas.add(uDelta);
                }
            }
        }
        // else: The bounds are infeasible since they will sum to < 1.0.
        
        // ------- Lower Bound ---------
        // e.g. [0.4, 1.0]
        CptBoundsDelta lDelta = new CptBoundsDelta(Type.PARAM, c, m, Lu.LOWER, mid - origBounds.getLb(Type.PARAM, c, m));
        CptBoundsDeltaList lDeltas = new CptBoundsDeltaList(lDelta);
        
        // Compute log(sum_{n=1}^{M_c} w_cn^{min})
        double logSumLbs = Double.NEGATIVE_INFINITY;
        for (int n = 0; n < origBounds.getNumParams(c); n++) {
            if (n == m) {
                logSumLbs = FastMath.logAdd(logSumLbs, mid);                
            } else {
                logSumLbs = FastMath.logAdd(logSumLbs, origBounds.getLb(Type.PARAM, c, n));
            }
        }
        if (logSumLbs <= 0.0) {
            // Compute the implicit upper bound for each parameter n.
            // w_cm <= log(1.0 - max(sum_{n != m} w_cn^{min}, 1.0))
            for (int n = 0; n < origBounds.getNumParams(c); n++) {
                if (n == m) {
                    continue;
                }
                // Since we already have logSumLbs, we subtract off the contribution of
                // the current parameter w_cn^{min}, to compute the sum relative to this
                // parameter.
                double logSumLbsForN = FastMath.logSubtract(logSumLbs, origBounds.getLb(Type.PARAM, c, n));
                assert (logSumLbsForN <= 0.0);
                double newUbForN = FastMath.logSubtract(0.0, logSumLbsForN);
                if (origBounds.getUb(Type.PARAM, c, n) > newUbForN) {
                    // e.g. [0.0, 0.6] implied by sum-to-one constraints
                    lDelta = new CptBoundsDelta(Type.PARAM, c, n, Lu.UPPER, newUbForN - origBounds.getUb(Type.PARAM, c, n));
                    lDeltas.add(lDelta);
                }
            }
        }
        // else:
        // The current bounds are infeasible, since the other parameters will be
        // forced to sum to > 1.0.
            
        List<CptBoundsDeltaList> deltasList = new ArrayList<CptBoundsDeltaList>();
        deltasList.add(uDeltas);
        deltasList.add(lDeltas);
        return deltasList;
    }

}
