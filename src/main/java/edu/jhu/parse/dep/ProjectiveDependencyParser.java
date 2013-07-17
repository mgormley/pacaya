package edu.jhu.parse.dep;

import java.util.Arrays;

import org.apache.log4j.Logger;

import edu.jhu.util.Utilities;

/**
 * Edge-factored projective dependency parser.
 * 
 * TODO: break ties randomly.
 * 
 * @author mgormley
 *
 */
public class ProjectiveDependencyParser {

    private static final Logger log = Logger.getLogger(ProjectiveDependencyParser.class);

    public enum DepParseType { VITERBI, INSIDE };
    
    /**
     * Dependency parse chart, which is just a projective spanning tree parse
     * chart with an additional special cell for the wall node.
     * 
     * @author mgormley
     */
    public static class DepParseChart extends ProjTreeChart {

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
            Utilities.fill(wallScore, Double.NEGATIVE_INFINITY);
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
                wallScore[child] = Utilities.logAdd(wallScore[child], score);
                goalScore = Utilities.logAdd(goalScore, score);
                // Don't update the backpointer.
            }
        }
    }
    
    /**
     * Projective spanning tree parse chart.
     * @author mgormley
     */
    private static class ProjTreeChart {
        
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
        
        public ProjTreeChart(int n, DepParseType type) {
            this.type = type;
            scores = new double[n][n][2][2];
            bps = new int[n][n][2][2];
            
            // Initialize chart to negative infinities.
            Utilities.fill(scores, Double.NEGATIVE_INFINITY);
            
            // Fill backpointers with -1.
            Utilities.fill(bps, -1);            
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
                scores[s][t][d][ic] = Utilities.logAdd(scores[s][t][d][ic], score);
                // Don't update the backpointer.
                
                // Commented out for speed.
                // log.debug(String.format("Cell: s=%d (r=%d) t=%d d=%s ic=%s score=%.2f exp(score)=%.2f", s, r, t, d == RIGHT ? "R" : "L",
                //        ic == COMPLETE ? "C" : "I", scores[s][t][d][ic], Utilities.exp(scores[s][t][d][ic])));
            }
        }
        
    }
    
    private static int LEFT = 0;
    private static int RIGHT = 1;
    private static int INCOMPLETE = 0;
    private static int COMPLETE = 1;
    
    private ProjectiveDependencyParser() {
        // Private constructor.
    }

    /**
     * Computes the maximum projective spanning tree with a single root node using the algorithm of
     * (Eisner, 1996) as described in McDonald (2006).  In the resulting tree,
     * the root node will have parent -1 and may have multiple children.
     * 
     * @param scores Input: The edge weights.
     * @param parents Output: The parent index of each node or -1 if it has no parent.
     * @return The score of the maximum projective spanning tree.
     */
    public static double maxProjSpanTree(double[][] scores, int[] parents) {
        return parse(new double[scores.length], scores, parents);
    }

    /**
     * This gives the maximum projective dependency tree using the algorithm of
     * (Eisner, 1996) as described in McDonald (2006). In the resulting tree,
     * the wall node (denoted as the parent -1) will be the root, and will have
     * exactly one child.
     * 
     * @param fracRoot Input: The edge weights from the wall to each child.
     * @param fracChild Input: The edge weights from parent to child.
     * @param parents Output: The parent index of each node or -1 if its parent
     *            is the wall node.
     * @return The score of the parse.
     */
    public static double parse(double[] fracRoot, double[][] fracChild, int[] parents) {
        assert (parents.length == fracRoot.length);
        assert (fracChild.length == fracRoot.length);    

        final int n = parents.length;
        final DepParseChart c = new DepParseChart(n, DepParseType.VITERBI);
        insideAlgorithm(fracRoot, fracChild, c);
        
        // Trace the backpointers to extract the parents.
        
        Arrays.fill(parents, -2);
        // Get the head of the sentence.
        int head = c.goalBp;
        parents[head] = -1; // The wall (-1) is its parent.
        // Extract parents left of the head.
        extractParentsComp(0, head, LEFT, c.scores, c.bps, parents);
        // Extract parents right of the head.
        extractParentsComp(head, n-1, RIGHT, c.scores, c.bps, parents);
        
        return c.goalScore;
    }
    
    /**
     * Runs the parsing algorithm of (Eisner, 1996) as described in McDonald
     * (2006), with special handling given to cell for the wall node.
     * 
     * @param fracRoot Input: The edge weights from the wall to each child.
     * @param fracChild Input: The edge weights from parent to child.
     * @param inChart Output: The parse chart.
     */
    private static void insideAlgorithm(double[] fracRoot, double[][] fracChild, DepParseChart inChart) {
        final int n = fracRoot.length;
        insideAlgorithm(fracChild, inChart);
        
        // Build goal constituents by combining left and right complete
        // constituents, on the left and right respectively. This corresponds to
        // left and right triangles. (Note: this is the opposite of how we
        // build an incomplete constituent.)
        for (int r=0; r<n; r++) {
            double score = inChart.scores[0][r][LEFT][COMPLETE] +
                           inChart.scores[r][n-1][RIGHT][COMPLETE] + 
                           fracRoot[r];
            inChart.updateGoalCell(r, score);
        }
    }
    
    /**
     * Runs the parsing algorithm of (Eisner, 1996) as described in McDonald (2006).
     * 
     * @param scores Input: The edge weights.
     * @param inChart Output: The parse chart.
     */
    private static void insideAlgorithm(final double[][] scores, final ProjTreeChart inChart) {             
        final int n = scores.length;        

        // Initialize.
        for (int s = 0; s < n; s++) {
            inChart.scores[s][s][RIGHT][COMPLETE] = 0.0;
            inChart.scores[s][s][LEFT][COMPLETE] = 0.0;
        }
                
        // Parse.
        for (int width = 1; width < n; width++) {
            for (int s = 0; s < n - width; s++) {
                int t = s + width;
                
                // First create incomplete items.
                for (int r=s; r<t; r++) {
                    for (int d=0; d<2; d++) {
                        double edgeScore = (d==LEFT) ? scores[t][s] : scores[s][t];
                        double score = inChart.scores[s][r][RIGHT][COMPLETE] +
                                       inChart.scores[r+1][t][LEFT][COMPLETE] +  
                                       edgeScore;
                        inChart.updateCell(s, t, d, INCOMPLETE, score, r);
                    }
                }
                
                // Second create complete items.
                // -- Left side.
                for (int r=s; r<t; r++) {
                    final int d = LEFT;
                    double score = inChart.scores[s][r][d][COMPLETE] +
                                inChart.scores[r][t][d][INCOMPLETE];
                    inChart.updateCell(s, t, d, COMPLETE, score, r);
                }
                // -- Right side.
                for (int r=s+1; r<=t; r++) {
                    final int d = RIGHT;
                    double score = inChart.scores[s][r][d][INCOMPLETE] +
                                   inChart.scores[r][t][d][COMPLETE];
                    inChart.updateCell(s, t, d, COMPLETE, score, r);
                }                
            }
        }
    }

    /**
     * An inside/outside chart for a dependency parse.
     * 
     * @author mgormley
     * 
     */
    public static class DepIoChart {
        final int n;
        final DepParseChart inChart;
        final DepParseChart outChart;

        public DepIoChart(DepParseChart inChart, DepParseChart outChart) {
            this.n = inChart.scores.length;
            this.inChart = inChart;
            this.outChart = outChart;
        }

        public double getLogInsideScore(int parent, int child) {
            if (parent == -1) {
                checkChild(child);
                return inChart.wallScore[child];
            } else {
                int start = Math.min(parent, child);
                int end = Math.max(parent, child);
                int d = getDirection(parent, child);
                checkCell(start, end);
                return inChart.getScore(start, end, d, INCOMPLETE);
            }
        }

        public double getLogOutsideScore(int parent, int child) {
            if (parent == -1) {
                checkChild(child);
                // These are always 0.0 on the outside chart, but it makes the
                // algorithmic differentiation clearer to include them.
                return outChart.wallScore[child];
            } else {
                int start = Math.min(parent, child);
                int end = Math.max(parent, child);
                int d = getDirection(parent, child);
                checkCell(start, end);
                return outChart.getScore(start, end, d, INCOMPLETE);
            }
        }

        public double getLogPartitionFunction() {
            return inChart.goalScore;
        }

        public double getLogSumOfPotentials(int parent, int child) {
            return getLogInsideScore(parent, child) + getLogOutsideScore(parent, child);
        }

        public double getLogExpectedCount(int parent, int child) {
            return getLogSumOfPotentials(parent, child) - getLogPartitionFunction();
        }

        private int getDirection(int parent, int child) {
            return (parent < child) ? RIGHT : LEFT;
        }

        /**
         * Checks that start \in [0, n-1] and end \in [1, n], where n is the
         * length of the sentence.
         */
        private void checkCell(int start, int end) {
            if (start > n - 1 || end > n || start < 0 || end < 1) {
                throw new IllegalStateException(String.format("Invalid cell: start=%d end=%d", start, end));
            }
        }

        private void checkChild(int child) {
            if (child > n - 1 || child < 0) {
                throw new IllegalStateException(String.format("Invalid child: %d", child));
            }
        }
    }
    

    /**
     * Runs the inside-outside algorithm for dependency parsing.
     * 
     * @param fracRoot Input: The edge weights from the wall to each child.
     * @param fracChild Input: The edge weights from parent to child.
     * @return The parse chart.
     */
    public static DepIoChart insideOutsideAlgorithm(double[] fracRoot, double[][] fracChild) {
        final int n = fracRoot.length;
        final DepParseChart inChart = new DepParseChart(n, DepParseType.INSIDE);
        final DepParseChart outChart = new DepParseChart(n, DepParseType.INSIDE);
        
        insideAlgorithm(fracRoot, fracChild, inChart);
        outsideAlgorithm(fracRoot, fracChild, inChart, outChart);
        
        return new DepIoChart(inChart, outChart);
    }
    
    /**
     * Runs the parsing algorithm of (Eisner, 1996) as described in McDonald
     * (2006), with special handling given to cell for the wall node.
     * 
     * @param fracRoot Input: The edge weights from the wall to each child.
     * @param fracChild Input: The edge weights from parent to child.
     * @param inChart Input: The inside parse chart.
     * @param outChart Output: The outside parse chart.
     */
    private static void outsideAlgorithm(double[] fracRoot, double[][] fracChild, final DepParseChart inChart, final DepParseChart outChart) {
        final int n = fracRoot.length;
        
        // Initialize.
        outChart.goalScore = 0.0;
        
        // The inside algorithm is effectively doing this...
        //
        // wallScore[r] log+=  inChart.scores[0][r][LEFT][COMPLETE] +
        //   inChart.scores[r][n-1][RIGHT][COMPLETE] + 
        //   fracRoot[r];
        //
        // goalScore log+= wallScore[r];
        
        for (int r=0; r<n; r++) {
            outChart.wallScore[r] = outChart.goalScore;
        }
        
        // Un-build goal constituents by combining left and right complete
        // constituents, on the left and right respectively. This corresponds to
        // left and right triangles. (Note: this is the opposite of how we
        // build an incomplete constituent.)
        for (int r=0; r<n; r++) {
            // Left child.
            double leftScore = outChart.wallScore[r] + inChart.scores[r][n - 1][RIGHT][COMPLETE] + fracRoot[r];
            outChart.updateCell(0, r, LEFT, COMPLETE, leftScore, -1);
            // Right child.
            double rightScore = outChart.wallScore[r] + inChart.scores[0][r][LEFT][COMPLETE] + fracRoot[r];
            outChart.updateCell(r, n - 1, RIGHT, COMPLETE, rightScore, -1);
        }
        
        outsideAlgorithm(fracChild, inChart, outChart, true);
    }

    /**
     * Runs the outside-algorithm for the parsing algorithm of (Eisner, 1996).
     * 
     * @param scores Input: The edge weights.
     * @param inChart Input: The inside parse chart.
     * @param outChart Output: The outside parse chart.
     */
    private static void outsideAlgorithm(final double[][] scores, final ProjTreeChart inChart, final ProjTreeChart outChart, boolean isInitialized) {             
        final int n = scores.length;

        if (!isInitialized) {
            // Base case.
            for (int d=0; d<2; d++) {
                outChart.scores[0][n-1][d][COMPLETE] = 0.0;
                outChart.scores[0][n-1][d][INCOMPLETE] = 0.0;
            }
        }
        
        // Parse.
        for (int width = n - 1; width >= 1; width--) {
            for (int s = 0; s < n - width; s++) {
                int t = s + width;
                
                // First create complete items (opposite of inside).
                // -- Left side.
                for (int r=s; r<t; r++) {
                    final int d = LEFT;
                    // Left child.
                    double leftScore = outChart.scores[s][t][d][COMPLETE] + inChart.scores[r][t][d][INCOMPLETE];
                    outChart.updateCell(s, r, d, COMPLETE, leftScore, -1);
                    // Right child.
                    double rightScore = outChart.scores[s][t][d][COMPLETE] + inChart.scores[s][r][d][COMPLETE];
                    outChart.updateCell(r, t, d, INCOMPLETE, rightScore, -1);
                }
                // -- Right side.
                for (int r=s+1; r<=t; r++) {
                    final int d = RIGHT;
                    // Left child.
                    double leftScore = outChart.scores[s][t][d][COMPLETE] + inChart.scores[r][t][d][COMPLETE];
                    outChart.updateCell(s, r, d, INCOMPLETE, leftScore, -1);
                    // Right child.
                    double rightScore = outChart.scores[s][t][d][COMPLETE] + inChart.scores[s][r][d][INCOMPLETE];
                    outChart.updateCell(r, t, d, COMPLETE, rightScore, -1);
                }
                
                // Second create incomplete items (opposite of inside).
                for (int r=s; r<t; r++) {
                    for (int d=0; d<2; d++) {
                        double edgeScore = (d == LEFT) ? scores[t][s] : scores[s][t];
                        // Left child.
                        double leftScore = outChart.scores[s][t][d][INCOMPLETE]
                                + inChart.scores[r + 1][t][LEFT][COMPLETE] + edgeScore;
                        outChart.updateCell(s, r, RIGHT, COMPLETE, leftScore, -1);
                        // Right child.
                        double rightScore = outChart.scores[s][t][d][INCOMPLETE]
                                + inChart.scores[s][r][RIGHT][COMPLETE] + edgeScore;
                        outChart.updateCell(r + 1, t, LEFT, COMPLETE, rightScore, -1);
                    }
                }
            }
        }
    }
    
    /**
     * Computes the maximum projective vine-parse tree with multiple root nodes
     * using the algorithm of (Eisner, 1996). In the resulting tree, the root
     * node will be the wall node (denoted by index -1), and may have multiple
     * children.
     * 
     * @param fracRoot Input: The edge weights from the wall to each child.
     * @param fracChild Input: The edge weights from parent to child.
     * @param parents Output: The parent index of each node or -1 if its parent
     *            is the wall node.
     * @return The score of the parse.
     */
    public static double vineParse(double[] fracRoot, double[][] fracChild,
            int[] parents) {
        assert (parents.length == fracRoot.length);
        assert (fracChild.length == fracRoot.length);        
        
        int n = parents.length + 1;
        double[][] scores = new double[n][n];
        for (int p=0; p<n; p++) { 
            for (int c=0; c<n; c++) {
                if (c == 0) {
                    scores[p][c] = Double.NEGATIVE_INFINITY;
                } else if (p == 0 && c > 0) {
                    scores[p][c] = fracRoot[c-1];
                } else {
                    scores[p][c] = fracChild[p-1][c-1];
                }
            }
        }
        int[] ps = new int[n];
        
        double score = vineParse(scores, ps);
        
        for (int i=0; i<parents.length; i++) {
            parents[i] = ps[i+1] - 1;
        }
        
        return score;
    }

    /**
     * Computes the vine parse with the given scores. This method is similar to
     * maxProjSpanning tree except that it enforces that the single root be the
     * leftmost node, which (by construction in the calling method) will be the
     * wall node.
     */
    private static double vineParse(final double[][] scores, final int[] parents) {
        final int n = parents.length;
        final ProjTreeChart c = new ProjTreeChart(n, DepParseType.VITERBI);
        insideAlgorithm(scores, c);
        final double[][][][] chart = c.scores;
        final int[][][][] bps = c.bps;
        
        // Trace the backpointers to extract the parents.
        Arrays.fill(parents, -2);

        // Get the head of the sentence.
        int head = 0;
        // The score will always be chart[0][n-1][RIGHT][COMPLETE].
        double goalScore = chart[0][0][LEFT][COMPLETE] + chart[0][n-1][RIGHT][COMPLETE];
        parents[head] = -1; // The wall (-1) is THE parent.
        // Extract parents left of the head.
        extractParentsComp(0, head, LEFT, chart, bps, parents);
        // Extract parents right of the head.
        extractParentsComp(head, n-1, RIGHT, chart, bps, parents);
        
        return goalScore;
    }

    private static void extractParentsComp(int s, int t, int d, double[][][][] chart, int[][][][] bps, int[] parents) {
        if (s == t) {
            return;
        }
        
        if (d == LEFT) {
            int r = bps[s][t][d][COMPLETE];
            // Left side is complete (a triangle).
            extractParentsComp(s, r, d, chart, bps, parents);
            // Right side is incomplete (a trapezoid).
            extractParentsIncomp(r, t, d, chart, bps, parents);
        } else { // d == RIGHT
            int r = bps[s][t][d][COMPLETE];
            // Left side is incomplete (a trapezoid).
            extractParentsIncomp(s, r, d, chart, bps, parents);
            // Right side is complete (a triangle).
            extractParentsComp(r, t, d, chart, bps, parents);
        }
    }

    private static void extractParentsIncomp(int s, int t, int d, double[][][][] chart,
            int[][][][] bps, int[] parents) {
        if (s == t) {
            return;
        }
        
        if (d == LEFT) {
            parents[s] = t;
        } else { // d == RIGHT
            parents[t] = s;
        }
        int r = bps[s][t][d][INCOMPLETE];
        extractParentsComp(s, r, RIGHT, chart, bps, parents);
        extractParentsComp(r+1, t, LEFT, chart, bps, parents);
    }

}
