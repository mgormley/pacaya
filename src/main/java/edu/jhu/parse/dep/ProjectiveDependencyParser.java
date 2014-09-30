package edu.jhu.parse.dep;

import java.util.Arrays;

import org.apache.log4j.Logger;

import edu.jhu.parse.dep.ProjTreeChart.DepParseType;


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

    static int LEFT = 0;
    static int RIGHT = 1;
    static int INCOMPLETE = 0;
    static int COMPLETE = 1;
    
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
        return parseSingleRoot(new double[scores.length], scores, parents);
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
    public static double parseSingleRoot(double[] fracRoot, double[][] fracChild, int[] parents) {
        assert (parents.length == fracRoot.length);
        assert (fracChild.length == fracRoot.length);    

        final int n = parents.length;
        final DepParseChart c = new DepParseChart(n, DepParseType.VITERBI);
        insideSingleRoot(fracRoot, fracChild, c);
        
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
    private static void insideSingleRoot(double[] fracRoot, double[][] fracChild, DepParseChart inChart) {
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
     * Runs the inside-outside algorithm for dependency parsing.
     * 
     * @param fracRoot Input: The edge weights from the wall to each child.
     * @param fracChild Input: The edge weights from parent to child.
     * @return The parse chart.
     */
    public static DepIoChart insideOutsideSingleRoot(double[] fracRoot, double[][] fracChild) {
        final int n = fracRoot.length;
        final DepParseChart inChart = new DepParseChart(n, DepParseType.INSIDE);
        final DepParseChart outChart = new DepParseChart(n, DepParseType.INSIDE);
        
        insideSingleRoot(fracRoot, fracChild, inChart);
        outsideSingleRoot(fracRoot, fracChild, inChart, outChart);
        
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
    private static void outsideSingleRoot(double[] fracRoot, double[][] fracChild, final DepParseChart inChart, final DepParseChart outChart) {
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
    public static double parseMultiRoot(double[] fracRoot, double[][] fracChild,
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
        
        double score = parseMultiRoot(scores, ps);
        
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
    private static double parseMultiRoot(final double[][] scores, final int[] parents) {
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
