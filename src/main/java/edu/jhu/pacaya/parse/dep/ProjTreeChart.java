package edu.jhu.pacaya.parse.dep;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.util.math.FastMath;

/**
 * Projective spanning tree parse chart.
 * @author mgormley
 */
public class ProjTreeChart {

    private static final Logger log = LoggerFactory.getLogger(ProjTreeChart.class);

    public enum DepParseType { VITERBI, INSIDE }
    
    // Indexed by left position (s), right position (t), direction of dependency (d),
    // and whether or not the constituent is complete (c).
    //
    // The value at chart[s][t][d][COMPLETE] will be the weight of the
    // maximum projective spanning tree rooted at s (if d == RIGHT) or
    // rooted at t (if d == LEFT). 
    //
    // For incomplete constituents chart[s][t][d][INCOMPLETE] indicates that
    // s is the parent of t if (d == RIGHT) or that t is the parent of s if
    // (d==LEFT). That is the direction, d, indicates which side is the dependent.
    private final double[] scores;

    // Backpointers, indexed just like the chart.
    //
    // The value at bps[s][t][d][c] will be the split point (r) for the
    // maximum chart entry.
    private final int[] bps;
    private final int nplus;
    
    final DepParseType type;
    
    public ProjTreeChart(int nplus, DepParseType type) {
        this.type = type;
        this.nplus = nplus;
        this.scores = new double[nplus*nplus*2*2];
        this.bps = new int[nplus*nplus*2*2];
        
        // Initialize chart to negative infinities.
        DoubleArrays.fill(scores, Double.NEGATIVE_INFINITY);
        
        // Fill backpointers with -1.
        Arrays.fill(bps, -1);
    }
    
    private final int getIndex(int s, int t, int d, int ic) {
        // The below is equivalent to: return s*nplus*2*2 + t*2*2 + d*2 + ic;
        return ((((s*nplus) + t)*2) + d)*2 + ic; 
    }
    
    // TODO: Consider using this method and making chart/bps private.
    public final double getScore(int s, int t, int d, int ic) {
        return scores[getIndex(s, t, d, ic)];
    }

    public final void setScore(int s, int t, int d, int ic, double val) {
        scores[getIndex(s, t, d, ic)] = val;
    }
    
    public final int getBp(int s, int t, int d, int ic) {
        return bps[getIndex(s, t, d, ic)];
    }
    
    public final int getNplus() {
        return nplus;
    }
    
    public final void updateCell(int s, int t, int d, int ic, double score, int r) {
        int idx = getIndex(s, t, d, ic);
        if (this.type == DepParseType.VITERBI) {
            if (score > scores[idx]) {
                scores[idx] = score;
                bps[idx] = r;
            }
        } else {
            scores[idx] = FastMath.logAdd(scores[idx], score);
            // Don't update the backpointer.
            
            // Commented out for speed.
            //            log.debug(String.format("Cell: s=%d (r=%d) t=%d d=%s ic=%s score=%10.2f exp(score)=%.2f", 
            //                    s, r, t, 
            //                    d == ProjectiveDependencyParser.RIGHT ? "R" : "L",
            //                    ic == ProjectiveDependencyParser.COMPLETE ? "C" : "I", 
            //                    scores[s][t][d][ic], 
            //                    FastMath.exp(scores[s][t][d][ic])));
        }
    }
    
}
