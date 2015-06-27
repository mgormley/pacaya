package edu.jhu.pacaya.hypergraph;

import java.util.ArrayList;

import org.junit.Test;

import edu.jhu.prim.tuple.IntTuple;
import edu.jhu.prim.util.Timer;

public class InsideAlgorithmMemoryTest {

    private static int LEFT = 0;
    private static int RIGHT = 1;
    private static int INCOMPLETE = 0;
    private static int COMPLETE = 1;
    
    @Test
    public void test() {
        Timer timer = new Timer();
        timer.start();
        ArrayList<IntTuple> edges = insideAlgorithm(new double[20][20]);
        timer.stop();
        System.out.println(edges.size());
        System.out.println(timer.totMs());
    }
    

    /**
     * Runs the parsing algorithm of (Eisner, 1996) as described in McDonald (2006).
     * 
     * @param scores Input: The edge weights.
     * @param inChart Output: The parse chart.
     * @return 
     */
    private static ArrayList<IntTuple> insideAlgorithm(final double[][] scores) {   
        ArrayList<IntTuple> edges = new ArrayList<IntTuple>();
        
        final int n = scores.length;        

        // Initialize.
        for (int s = 0; s < n; s++) {
            edges.add(new IntTuple(s, s, RIGHT, COMPLETE));
            edges.add(new IntTuple(s, s, LEFT, COMPLETE));
        }
                
        // Parse.
        for (int width = 1; width < n; width++) {
            for (int s = 0; s < n - width; s++) {
                int t = s + width;
                
                // First create incomplete items.
                for (int r=s; r<t; r++) {
                    for (int d=0; d<2; d++) {
                        edges.add(new IntTuple(s,t,d));
//                        double edgeScore = (d==LEFT) ? scores[t][s] : scores[s][t];
//                        double score = inChart.scores[s][r][RIGHT][COMPLETE] +
//                                       inChart.scores[r+1][t][LEFT][COMPLETE] +  
//                                       edgeScore;
//                        inChart.updateCell(s, t, d, INCOMPLETE, score, r);
                    }
                }
                
                // Second create complete items.
                // -- Left side.
                for (int r=s; r<t; r++) {
                    final int d = LEFT;
                    edges.add(new IntTuple(s,t,d));
//                    double score = inChart.scores[s][r][d][COMPLETE] +
//                                inChart.scores[r][t][d][INCOMPLETE];
//                    inChart.updateCell(s, t, d, COMPLETE, score, r);
                }
                // -- Right side.
                for (int r=s+1; r<=t; r++) {                    
                    final int d = RIGHT;
                    edges.add(new IntTuple(s,t,d));
//                    double score = inChart.scores[s][r][d][INCOMPLETE] +
//                                   inChart.scores[r][t][d][COMPLETE];
//                    inChart.updateCell(s, t, d, COMPLETE, score, r);
                }                
            }
        }
        return edges;
    }
    
    public void main(String[] args) {
        
    }
    
}
