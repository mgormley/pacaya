package edu.jhu.pacaya.parse.dep;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.parse.dep.ProjTreeChart.DepParseType;


/**
 * Edge-factored projective dependency parser.
 * 
 * TODO: break ties randomly.
 * 
 * @author mgormley
 *
 */
public class ProjectiveDependencyParser {

    private static final Logger log = LoggerFactory.getLogger(ProjectiveDependencyParser.class);

    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int INCOMPLETE = 0;
    public static final int COMPLETE = 1;
    
    private ProjectiveDependencyParser() {
        // Private constructor.
    }

    /**
     * Computes the maximum projective spanning tree with a single root node using the algorithm of
     * (Eisner, 2000) as described in McDonald (2006).  In the resulting tree,
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
     * (Eisner, 2000) as described in McDonald (2006). In the resulting tree,
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
        final ProjTreeChart c = new ProjTreeChart(n+1, DepParseType.VITERBI);
        insideAlgorithm(EdgeScores.combine(fracRoot, fracChild), c, true);
        
        // Trace the backpointers to extract the parents.        
        Arrays.fill(parents, -2);
        // Get the head of the sentence.
        int head = c.bps[0][n][RIGHT][COMPLETE];
        parents[head-1] = -1; // The wall (-1) is its parent.
        // Extract parents left of the head.
        extractParentsComp(1, head, LEFT, c.scores, c.bps, parents);
        // Extract parents right of the head.
        extractParentsComp(head, n, RIGHT, c.scores, c.bps, parents);
        
        return c.scores[0][n][RIGHT][COMPLETE];
    }
    
    /**
     * Computes the maximum projective vine-parse tree with multiple root nodes
     * using the algorithm of (Eisner, 2000). In the resulting tree, the root
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

        final int n = parents.length;
        final ProjTreeChart c = new ProjTreeChart(n+1, DepParseType.VITERBI);
        insideAlgorithm(EdgeScores.combine(fracRoot, fracChild), c, false);
        
        // Trace the backpointers to extract the parents.        
        Arrays.fill(parents, -2);
        // Extract parents right of the wall.
        extractParentsComp(0, n, RIGHT, c.scores, c.bps, parents);
        return c.scores[0][n][RIGHT][COMPLETE];
    }
    
    /**
     * Runs the inside-outside algorithm for dependency parsing.
     * 
     * @param fracRoot Input: The edge weights from the wall to each child.
     * @param fracChild Input: The edge weights from parent to child.
     * @return The parse chart.
     */
    public static DepIoChart insideOutsideSingleRoot(double[] fracRoot, double[][] fracChild) {
        final boolean singleRoot = true;        
        return insideOutside(fracRoot, fracChild, singleRoot);
    }
    
    public static DepIoChart insideOutsideMultiRoot(double[] fracRoot, double[][] fracChild) {
        final boolean singleRoot = false;        
        return insideOutside(fracRoot, fracChild, singleRoot);
    }

    private static DepIoChart insideOutside(double[] fracRoot, double[][] fracChild, final boolean singleRoot) {
        final int n = fracRoot.length;
        final ProjTreeChart inChart = new ProjTreeChart(n+1, DepParseType.INSIDE);
        final ProjTreeChart outChart = new ProjTreeChart(n+1, DepParseType.INSIDE);
        
        double[][] scores = EdgeScores.combine(fracRoot, fracChild);
        log.debug("Inside:");
        insideAlgorithm(scores, inChart, singleRoot);
        log.debug("Outside:");
        outsideAlgorithm(scores, inChart, outChart, singleRoot);
        
        return new DepIoChart(inChart, outChart);
    }
    
    /**
     * Runs the parsing algorithm of (Eisner, 2000) as described in McDonald (2006).
     * 
     * @param scores Input: The edge weights.
     * @param inChart Output: The parse chart.
     */
    private static void insideAlgorithm(final double[][] scores, final ProjTreeChart inChart, boolean singleRoot) {
        final int n = scores.length;        
        final int startIdx = singleRoot ? 1 : 0;         

        // Initialize.
        for (int s = 0; s < n; s++) {
            inChart.scores[s][s][RIGHT][COMPLETE] = 0.0;
            inChart.scores[s][s][LEFT][COMPLETE] = 0.0;
        }
                
        // Parse.
        for (int width = 1; width < n; width++) {
            for (int s = startIdx; s < n - width; s++) {
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
        
        if (singleRoot) {
            // Build goal constituents by combining left and right complete
            // constituents, on the left and right respectively. This corresponds to
            // left and right triangles. (Note: this is the opposite of how we
            // build an incomplete constituent.)
            for (int r=1; r<n; r++) {
                double score = inChart.scores[1][r][LEFT][COMPLETE] +
                               inChart.scores[r][n-1][RIGHT][COMPLETE] + 
                               scores[0][r];
                inChart.updateCell(0, r, RIGHT, INCOMPLETE, score, r);
                inChart.updateCell(0, n-1, RIGHT, COMPLETE, score, r);
            }
        }
    }

    /**
     * Runs the outside-algorithm for the parsing algorithm of (Eisner, 2000).
     * 
     * @param scores Input: The edge weights.
     * @param inChart Input: The inside parse chart.
     * @param outChart Output: The outside parse chart.
     */
    private static void outsideAlgorithm(final double[][] scores, final ProjTreeChart inChart, final ProjTreeChart outChart, 
            boolean singleRoot) {             
        final int n = scores.length;
        final int startIdx = singleRoot ? 1 : 0;         

        if (singleRoot) {
            // Initialize.
            double goalScore = 0.0;
            outChart.updateCell(0, n-1, RIGHT, COMPLETE, goalScore, -1);
            
            // The inside algorithm is effectively doing this...
            //
            // wallScore[r] log+=  inChart.scores[0][r][LEFT][COMPLETE] +
            //                     inChart.scores[r][n-1][RIGHT][COMPLETE] + 
            //                     fracRoot[r];
            //
            // goalScore log+= wallScore[r];            
            for (int r=1; r<n; r++) {
                outChart.updateCell(0, r, RIGHT, INCOMPLETE, goalScore, -1);
            }
            
            // Un-build goal constituents by combining left and right complete
            // constituents, on the left and right respectively. This corresponds to
            // left and right triangles. (Note: this is the opposite of how we
            // build an incomplete constituent.)
            for (int r=1; r<n; r++) {
                // Left child.
                double leftScore = outChart.scores[0][r][RIGHT][INCOMPLETE] + 
                                   inChart.scores[r][n - 1][RIGHT][COMPLETE] + 
                                   scores[0][r];
                outChart.updateCell(1, r, LEFT, COMPLETE, leftScore, -1);
                // Right child.
                double rightScore = outChart.scores[0][r][RIGHT][INCOMPLETE] + 
                                    inChart.scores[1][r][LEFT][COMPLETE] + 
                                    scores[0][r];
                outChart.updateCell(r, n - 1, RIGHT, COMPLETE, rightScore, -1);
            }
        } else  {
            // Base case.
            for (int d=0; d<2; d++) {
                outChart.updateCell(0, n-1, d, COMPLETE, 0.0, -1);
            }
        }
        
        // Parse.
        for (int width = n - 1; width >= 1; width--) {
            for (int s = startIdx; s < n - width; s++) {
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
            parents[s-1] = t-1;
        } else { // d == RIGHT
            parents[t-1] = s-1;
        }
        int r = bps[s][t][d][INCOMPLETE];
        extractParentsComp(s, r, RIGHT, chart, bps, parents);
        extractParentsComp(r+1, t, LEFT, chart, bps, parents);
    }

}
