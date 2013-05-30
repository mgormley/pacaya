package edu.jhu.hltcoe.parse.dep;

import java.util.Arrays;

/**
 * Projective dependency parser.
 * 
 * @author mgormley
 *
 */
public class ProjectiveDependencyParser {

    private static int LEFT = 0;
    private static int RIGHT = 1;
    private static int INCOMPLETE = 0;
    private static int COMPLETE = 1;
    
    private ProjectiveDependencyParser() {
        // Private constructor.
    }

    /**
     * This gives the maximum projective spanning tree with the left-most node as the root
     * using the algorithm of (Eisner, 1996) as described in McDonald (2006).
     * 
     * @param fracRoot Input: The edge weights from the root to each child.
     * @param fracChild Input: The edge weights from parent to child.
     * @param parents Output: The parent index of each node or -1 if it has no parent.
     * @return The score of the maximum projective spanning tree.
     */
    public static double parse(double[] fracRoot, double[][] fracChild,
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
        double score = parse(scores, ps);
        
        for (int i=0; i<parents.length; i++) {
            parents[i] = ps[i+1] - 1;
        }
        
        // The score will always be chart[0][n-1][RIGHT][COMPLETE].
        return score;
    }

    /**
     * Computes the maximum projective spanning tree with a single root node
     * that is either the left-most or right-most node using the algorithm of
     * (Eisner, 1996) as described in McDonald (2006).
     * 
     * @param scores Input: The edge weights.
     * @param parents Output: The parent index of each node or -1 if it has no parent.
     * @return The score of the maximum projective spanning tree.
     */
    static double parse(double[][] scores, int[] parents) {
        Arrays.fill(parents, -1);
                
        final int n = parents.length;
        // Indexed by left position (s), right position (t), direction of dependency (d),
        // and whether or not the constituent is complete (c).
        //
        // The value at chart[s][t][d][COMPLETE] will be the weight of the
        // maximum projective spanning tree rooted at s (if d == RIGHT) or
        // rooted at t (if d == LEFT). 
        double[][][][] chart = new double[n][n][2][2];
        // Backpointers, indexed just like the chart.
        //
        // The value at bps[s][t][d][c] will be the split point (r) for the
        // maximum chart entry.
        int[][][][] bps = new int[n][n][2][2];

        // Parse.
        for (int width = 1; width < n; width++) {
            for (int s = 0; s < n - width; s++) {
                int t = s + width;
                
                // First create incomplete items.
                for (int r=s; r<t; r++) {
                    for (int d=0; d<2; d++) {
                        double edgeScore = (d==LEFT) ? scores[t][s] : scores[s][t];
                        double score = chart[s][r][RIGHT][COMPLETE] +
                                       chart[r+1][t][LEFT][COMPLETE] +  
                                       edgeScore;
                        if (score > chart[s][t][d][INCOMPLETE]) {
                            chart[s][t][d][INCOMPLETE] = score;
                            bps[s][t][d][INCOMPLETE] = r;
                        }
                    }
                }
                
                // Second create complete items.
                // -- Left side.
                for (int r=s; r<t; r++) {
                    final int d = LEFT;
                    double score = chart[s][r][d][COMPLETE] +
                                chart[r][t][d][INCOMPLETE];
                    if (score > chart[s][t][d][COMPLETE]) {
                        chart[s][t][d][COMPLETE] = score;
                        bps[s][t][d][COMPLETE] = r;
                    }
                }
                // -- Right side.
                for (int r=s+1; r<=t; r++) {
                    final int d = RIGHT;
                    double score = chart[s][r][d][INCOMPLETE] +
                                   chart[r][t][d][COMPLETE];
                    if (score > chart[s][t][d][COMPLETE]) {
                        chart[s][t][d][COMPLETE] = score;
                        bps[s][t][d][COMPLETE] = r;
                    }
                }
                
            }
        }
        
        // Trace the backpointers to extract the parents.
        double leftScore = chart[0][n-1][LEFT][COMPLETE];
        double rightScore = chart[0][n-1][RIGHT][COMPLETE];
        if (leftScore > rightScore) {
            extractParentsComp(0, n-1, LEFT, chart, bps, parents);
            return leftScore;
        } else {
            extractParentsComp(0, n-1, RIGHT, chart, bps, parents);
            return rightScore;
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
