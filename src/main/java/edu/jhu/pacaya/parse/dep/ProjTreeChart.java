package edu.jhu.pacaya.parse.dep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.arrays.IntArrays;
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
    final double[][][][] scores;

    // Backpointers, indexed just like the chart.
    //
    // The value at bps[s][t][d][c] will be the split point (r) for the
    // maximum chart entry.
    final int[][][][] bps;
    
    final DepParseType type;
    
    public ProjTreeChart(int nplus, DepParseType type) {
        this.type = type;
        scores = new double[nplus][nplus][2][2];
        bps = new int[nplus][nplus][2][2];
        
        // Initialize chart to negative infinities.
        DoubleArrays.fill(scores, Double.NEGATIVE_INFINITY);
        
        // Fill backpointers with -1.
        IntArrays.fill(bps, -1);            
    }
    
    // TODO: Consider using this method and making chart/bps private.
    public final double getScore(int s, int t, int d, int ic) {
        return scores[s][t][d][ic];
    }
    
    public final int getBp(int s, int t, int d, int ic) {
        return bps[s][t][d][ic];
    }
    
    public final void updateCell(int s, int t, int d, int ic, double score, int r) {
        if (this.type == DepParseType.VITERBI) {
            if (score > scores[s][t][d][ic]) {
                scores[s][t][d][ic] = score;
                bps[s][t][d][ic] = r;
            }
        } else {
            scores[s][t][d][ic] = FastMath.logAdd(scores[s][t][d][ic], score);
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
