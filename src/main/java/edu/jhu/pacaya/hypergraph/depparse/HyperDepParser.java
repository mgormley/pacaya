package edu.jhu.pacaya.hypergraph.depparse;

import edu.jhu.pacaya.hypergraph.Hyperalgo;
import edu.jhu.pacaya.hypergraph.Hyperedge;
import edu.jhu.pacaya.hypergraph.Hypernode;
import edu.jhu.pacaya.hypergraph.Hyperpotential;
import edu.jhu.pacaya.hypergraph.HyperpotentialFoe;
import edu.jhu.pacaya.hypergraph.Hyperalgo.Scores;
import edu.jhu.pacaya.parse.dep.DepIoChart;
import edu.jhu.pacaya.parse.dep.EdgeScores;
import edu.jhu.pacaya.parse.dep.ProjTreeChart;
import edu.jhu.pacaya.parse.dep.ProjectiveDependencyParser;
import edu.jhu.pacaya.parse.dep.ProjTreeChart.DepParseType;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.Algebras;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.LogSignAlgebra;
import edu.jhu.pacaya.util.semiring.Semiring;
import edu.jhu.prim.tuple.Pair;

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
        
        O1DpHypergraph graph = new O1DpHypergraph(fracRoot, fracChild, semiring, singleRoot);
        Scores scores = new Scores();
        Hyperalgo.insideAlgorithm(graph, graph.getPotentials(), semiring, scores);
        Hyperalgo.outsideAlgorithm(graph, graph.getPotentials(), semiring, scores);

        return getDepIoChart(graph, scores);
    }
    
    public static DepIoChart getDepIoChart(O1DpHypergraph graph, Scores scores) {
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
    public static Pair<O1DpHypergraph, Scores> insideSingleRootEntropyFoe(double[] fracRoot, double[][] fracChild) {
        final Algebra semiring = new LogSignAlgebra();         
        return insideSingleRootEntropyFoe(fracRoot, fracChild, semiring);
    }

    public static Pair<O1DpHypergraph, Scores> insideSingleRootEntropyFoe(double[] fracRoot,
            double[][] fracChild, final Algebra semiring) {
        boolean singleRoot = true;
        return insideEntropyFoe(fracRoot, fracChild, semiring, singleRoot);
    }

    public static Pair<O1DpHypergraph, Scores> insideEntropyFoe(double[] fracRoot, double[][] fracChild,
            final Algebra semiring, boolean singleRoot) {
        Algebras.fromLogProb(fracRoot, semiring);
        Algebras.fromLogProb(fracChild, semiring);
        
        O1DpHypergraph graph = new O1DpHypergraph(fracRoot, fracChild, semiring, singleRoot);
        Scores scores = new Scores();
        final Hyperpotential w = graph.getPotentials();
        HyperpotentialFoe wFoe = new EntropyHyperpotentialFoe(w);
        Hyperalgo.insideAlgorithmFirstOrderExpect(graph, wFoe, semiring, scores);

        return new Pair<O1DpHypergraph, Scores>(graph, scores);
    }
    
    /** Runs inside outside on an all-grandparents hypergraph and returns the edge marginals in the real semiring. */
    public static EdgeScores insideOutsideO2AllGraSingleRoot(DependencyScorer scorer, Algebra s) {
        return insideOutside02AllGra(scorer, s, true);
    }
    
    /** Runs inside outside on an all-grandparents hypergraph and returns the edge marginals in the real semiring. */
    public static EdgeScores insideOutsideO2AllGraMultiRoot(DependencyScorer scorer, Algebra s) {
        return insideOutside02AllGra(scorer, s, false);
    }

    protected static EdgeScores insideOutside02AllGra(DependencyScorer scorer, Algebra s, boolean singleRoot) {
        O2AllGraDpHypergraph graph = new O2AllGraDpHypergraph(scorer, s, singleRoot);
        Scores sc = new Scores();
        Hyperalgo.forward(graph, graph.getPotentials(), s, sc);
        return getEdgeMarginalsRealSemiring(graph, sc);
    }

    /** Gets the edge marginals in the real semiring from an all-grandparents hypergraph and its marginals in scores. */
    public static EdgeScores getEdgeMarginalsRealSemiring(O2AllGraDpHypergraph graph, Scores sc) {
        Algebra s = graph.getAlgebra();
        int nplus = graph.getNumTokens()+1;

        Hypernode[][][][] c = graph.getChart();
        EdgeScores marg = new EdgeScores(graph.getNumTokens(), 0.0);
        for (int width = 1; width < nplus; width++) {
            for (int i = 0; i < nplus - width; i++) {
                int j = i + width;
                for (int g=0; g<nplus; g++) {
                    if (i <= g && g <= j && !(i==0 && g==O2AllGraDpHypergraph.NIL)) { continue; }
                    if (j > 0) {
                        marg.incrScore(i-1, j-1, s.toReal(sc.marginal[c[i][j][g][O2AllGraDpHypergraph.INCOMPLETE].getId()]));
                    } 
                    if (i > 0) {
                        marg.incrScore(j-1, i-1, s.toReal(sc.marginal[c[j][i][g][O2AllGraDpHypergraph.INCOMPLETE].getId()]));
                    }
                }
            }
        }
        return marg;
    }
    
}
