package edu.jhu.parse.dep;

import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.util.math.FastMath;

/**
 * Dependency parse chart, which is just a projective spanning tree parse
 * chart with an additional special cell for the wall node.
 * 
 * @author mgormley
 */
public class DepParseChart extends ProjTreeChart {

    // Indexed by word position in the sentence. So goal[i] gives the
    // maximum projective dependency tree where the i'th word is the unique
    // child of the wall node.
    final double wallScore[];
    // The score for the overall parse tree.
    double goalScore;
    // The position of the word that heads the maximum projective dependency
    // tree.
    int goalBp;
    
    public DepParseChart(int n, DepParseType type) {
        super(n, type);
        wallScore = new double[n];
        // Initialize chart to negative infinities.
        DoubleArrays.fill(wallScore, Double.NEGATIVE_INFINITY);
        goalScore = Double.NEGATIVE_INFINITY;
        // Fill backpointers with -1.
        goalBp = -1;
    }

    public final void updateGoalCell(int child, double score) {
        if (this.type == DepParseType.VITERBI) {
            if (score > wallScore[child]) {
                // This if statement will always be true.
                wallScore[child] = score;
            }
            if (score > goalScore) {
                goalScore = score;
                goalBp = child;
            }
        } else {
            wallScore[child] = FastMath.logAdd(wallScore[child], score);
            goalScore = FastMath.logAdd(goalScore, score);
            // Don't update the backpointer.
        }
    }
}