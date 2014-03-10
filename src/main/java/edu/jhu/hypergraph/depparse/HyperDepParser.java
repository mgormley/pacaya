package edu.jhu.hypergraph.depparse;

import edu.jhu.hypergraph.Hyperalgo;
import edu.jhu.hypergraph.Hyperalgo.Scores;
import edu.jhu.hypergraph.Hypernode;
import edu.jhu.parse.dep.ProjectiveDependencyParser.DepIoChart;
import edu.jhu.parse.dep.ProjectiveDependencyParser.DepParseChart;
import edu.jhu.parse.dep.ProjectiveDependencyParser.DepParseType;
import edu.jhu.util.Timer;
import edu.jhu.util.semiring.LogSemiring;
import edu.jhu.util.semiring.Semiring;

public class HyperDepParser {

    /**
     * Runs the inside-outside algorithm for dependency parsing.
     * 
     * @param fracRoot Input: The edge weights from the wall to each child.
     * @param fracChild Input: The edge weights from parent to child.
     * @return The parse chart.
     */
    public static DepIoChart insideOutsideAlgorithm(double[] fracRoot, double[][] fracChild) {
        // Currently we only support this semiring since DepParseChart assumes log probs.
        Semiring semiring = new LogSemiring();
        
        FirstOrderDepParseHypergraph graph = new FirstOrderDepParseHypergraph(fracRoot, fracChild, semiring);
        Scores scores = new Scores();
        Hyperalgo.insideAlgorithm(graph, graph.getPotentials(), semiring, scores);
        Hyperalgo.outsideAlgorithm(graph, graph.getPotentials(), semiring, scores);

        return getDepIoChart(graph, scores);
    }

    private static DepIoChart getDepIoChart(FirstOrderDepParseHypergraph graph, Scores scores) {
        final int n = graph.getNumTokens();        
        final DepParseChart inChart = new DepParseChart(n, DepParseType.INSIDE);
        final DepParseChart outChart = new DepParseChart(n, DepParseType.INSIDE);
        Hypernode[] wallChart = graph.getWallChart();
        Hypernode[][][][] childChart = graph.getChildChart();
        for (int width = 1; width < n; width++) {
            for (int s = 0; s < n - width; s++) {
                int t = s + width;                
                for (int d=0; d<2; d++) {
                    for (int c=0; c<2; c++) {                        
                        int id = childChart[s][t][d][c].getId();
                        inChart.updateCell(s, t, d, c, scores.beta[id], -1);
                        outChart.updateCell(s, t, d, c, scores.alpha[id], -1);
                    }
                }
            }
        }
        for (int s=0; s<n; s++) {
            int id = wallChart[s].getId();
            inChart.updateGoalCell(s, scores.beta[id]);
            outChart.updateGoalCell(s, scores.alpha[id]);
        }
        // Root is automatically updated by updateGoalCell() calls above.        
        return new DepIoChart(inChart, outChart);
    }
    
}
