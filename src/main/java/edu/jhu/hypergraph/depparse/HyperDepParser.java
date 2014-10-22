package edu.jhu.hypergraph.depparse;

import edu.jhu.hypergraph.Hyperalgo;
import edu.jhu.hypergraph.Hyperalgo.Scores;
import edu.jhu.hypergraph.Hyperedge;
import edu.jhu.hypergraph.Hypernode;
import edu.jhu.hypergraph.Hyperpotential;
import edu.jhu.hypergraph.HyperpotentialFoe;
import edu.jhu.parse.dep.DepIoChart;
import edu.jhu.parse.dep.ProjTreeChart;
import edu.jhu.parse.dep.ProjTreeChart.DepParseType;
import edu.jhu.parse.dep.ProjectiveDependencyParser;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Algebras;
import edu.jhu.util.semiring.LogSemiring;
import edu.jhu.util.semiring.LogSignAlgebra;
import edu.jhu.util.semiring.Semiring;

public class HyperDepParser {

    /**
     * First-order expectation semiring potential function used to compute
     * entropy. See section 6.1 of Li & Eisner (2009).
     * 
     * @author mgormley
     */
    public static final class EntropyHyperpotentialFoe implements HyperpotentialFoe {
        private final Hyperpotential w;

        private EntropyHyperpotentialFoe(Hyperpotential w) {
            this.w = w;
        }

        @Override
        public double getScore(Hyperedge e, Semiring s) {
            return w.getScore(e, s);
        }

        @Override
        public double getScoreFoe(Hyperedge e, Algebra s) {
            double p_e = w.getScore(e, s);
            if (p_e == s.zero()) {
                return p_e;
            }
            double log_p_e = s.times(p_e, s.log(p_e));
            assert !Double.isNaN(log_p_e);
            return log_p_e;
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
        boolean singleRoot = true;
        return insideOutside(fracRoot, fracChild, singleRoot);
    }
    
    /**
     * Runs the inside-outside algorithm for dependency parsing.
     * 
     * @param fracRoot Input: The edge weights from the wall to each child.
     * @param fracChild Input: The edge weights from parent to child.
     * @return The parse chart.
     */
    public static DepIoChart insideOutsideMultiRoot(double[] fracRoot, double[][] fracChild) {
        boolean singleRoot = false;
        return insideOutside(fracRoot, fracChild, singleRoot);
    }

    private static DepIoChart insideOutside(double[] fracRoot, double[][] fracChild, boolean singleRoot) {
        // Currently we only support this semiring since DepParseChart assumes log probs.
        LogSemiring semiring = new LogSemiring();
        //LogPosNegSemiring semiring = new LogPosNegSemiring();
        //Semirings.fromLogProb(fracRoot, semiring);
        //Semirings.fromLogProb(fracChild, semiring);
        
        DepParseHypergraph graph = new DepParseHypergraph(fracRoot, fracChild, semiring, singleRoot);
        Scores scores = new Scores();
        Hyperalgo.insideAlgorithm(graph, graph.getPotentials(), semiring, scores);
        Hyperalgo.outsideAlgorithm(graph, graph.getPotentials(), semiring, scores);

        return getDepIoChart(graph, scores);
    }
    
    public static DepIoChart getDepIoChart(DepParseHypergraph graph, Scores scores) {
        final int nplus = graph.getNumTokens() + 1;        
        final ProjTreeChart inChart = new ProjTreeChart(nplus, DepParseType.INSIDE);
        final ProjTreeChart outChart = new ProjTreeChart(nplus, DepParseType.INSIDE);
        Hypernode[][][][] chart = graph.getChart();
        for (int width = 0; width < nplus; width++) {
            for (int s = 0; s < nplus - width; s++) {
                int t = s + width;                
                for (int d=0; d<2; d++) {
                    for (int c=0; c<2; c++) { 
                        if (width == 0 && c == ProjectiveDependencyParser.INCOMPLETE) { continue; }
                        int id = chart[s][t][d][c].getId();
                        inChart.updateCell(s, t, d, c, scores.beta[id], -1);
                        outChart.updateCell(s, t, d, c, scores.alpha[id], -1);
                    }
                }
            }
        }
        return new DepIoChart(inChart, outChart);
    }
    
    /**
     * Runs the inside-outside algorithm for dependency parsing.
     * 
     * @param fracRoot Input: The edge weights from the wall to each child.
     * @param fracChild Input: The edge weights from parent to child.
     * @return The parse chart.
     */
    public static Pair<DepParseHypergraph, Scores> insideSingleRootEntropyFoe(double[] fracRoot, double[][] fracChild) {
        final Algebra semiring = new LogSignAlgebra();         
        return insideSingleRootEntropyFoe(fracRoot, fracChild, semiring);
    }

    public static Pair<DepParseHypergraph, Scores> insideSingleRootEntropyFoe(double[] fracRoot,
            double[][] fracChild, final Algebra semiring) {
        boolean singleRoot = true;
        return insideEntropyFoe(fracRoot, fracChild, semiring, singleRoot);
    }

    public static Pair<DepParseHypergraph, Scores> insideEntropyFoe(double[] fracRoot, double[][] fracChild,
            final Algebra semiring, boolean singleRoot) {
        Algebras.fromLogProb(fracRoot, semiring);
        Algebras.fromLogProb(fracChild, semiring);
        
        DepParseHypergraph graph = new DepParseHypergraph(fracRoot, fracChild, semiring, singleRoot);
        Scores scores = new Scores();
        final Hyperpotential w = graph.getPotentials();
        HyperpotentialFoe wFoe = new EntropyHyperpotentialFoe(w);
        Hyperalgo.insideAlgorithmFirstOrderExpect(graph, wFoe, semiring, scores);

        return new Pair<DepParseHypergraph, Scores>(graph, scores);
    }
    
}
