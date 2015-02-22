package edu.jhu.nlp.depparse;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.autodiff.erma.AbstractFgInferencer;
import edu.jhu.autodiff.erma.InsideOutsideDepParse;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.inf.FgInferencerFactory;
import edu.jhu.gm.model.ClampFactor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.LinkVar;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor;
import edu.jhu.gm.model.globalfac.SimpleProjDepTreeFactor;
import edu.jhu.hypergraph.Hyperalgo;
import edu.jhu.hypergraph.Hyperalgo.Scores;
import edu.jhu.hypergraph.Hypernode;
import edu.jhu.hypergraph.depparse.O2AllGraDpHypergraph;
import edu.jhu.hypergraph.depparse.O2AllGraDpHypergraph.DependencyScorer;
import edu.jhu.hypergraph.depparse.O2AllGraDpHypergraphTest.ExplicitDependencyScorer;
import edu.jhu.nlp.FeTypedFactor;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder.DepParseFactorTemplate;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder.GraFeTypedFactor;
import edu.jhu.parse.dep.EdgeScores;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Algebras;

public class O2AllGraFgInferencer extends AbstractFgInferencer implements FgInferencer {

    public static class O2AllGraFgInferencerFactory implements FgInferencerFactory {

        private Algebra s;
        public O2AllGraFgInferencerFactory(Algebra s) {
            this.s = s;
        }
        
        @Override
        public FgInferencer getInferencer(FactorGraph fg) { 
            return new O2AllGraFgInferencer(fg, s);
        }

        @Override
        public Algebra getAlgebra() {
            return s;
        }
        
    }
    
    private static final Logger log = LoggerFactory.getLogger(O2AllGraFgInferencer.class);

    private final Algebra s;
    private FactorGraph fg;
    private int n;
    private EdgeScores edgeMarg;
    private O2AllGraDpHypergraph graph;
    private Scores sc;
    
    public O2AllGraFgInferencer(FactorGraph fg, Algebra s) {
        this.s = s;
        this.fg = fg;
        // Guess the length of the sentence.
        // TODO: Pass this in to the constructor.
        n = -1;
        for (Var v : fg.getVars()) {
            LinkVar lv = (LinkVar) v;
            n = Math.max(n, lv.getChild()+1);
            n = Math.max(n, lv.getParent()+1);
        }
    }

    @Override
    public void run() {
        DependencyScorer scorer = getDepScorerFromFg(fg, n);
        graph = new O2AllGraDpHypergraph(scorer, s, InsideOutsideDepParse.singleRoot);
        sc = new Scores();
        Hyperalgo.forward(graph, graph.getPotentials(), s, sc);
        if (sc.beta[graph.getRoot().getId()] == s.zero()) {
            int i;
            i=0;
            for (Factor f : fg.getFactors()) {
                if (f.getVars().size() == 1) {
                    LinkVar lv = (LinkVar) f.getVars().get(0);
                    int p = lv.getParent();
                    int c = lv.getChild();
                    if (f.getLogUnormalizedScore(LinkVar.TRUE) == 0.0) {
                        System.out.printf("p=%d c=%d lv=TRUE sc=%f f=%d\n", p, c, f.getLogUnormalizedScore(LinkVar.TRUE), i);
                    }
                }
                i++;
            }
            i=0;
            for (Factor f : fg.getFactors()) {
                if (f.getVars().size() == 1) {
                    LinkVar lv = (LinkVar) f.getVars().get(0);
                    int p = lv.getParent();
                    int c = lv.getChild();
                    if (f.getLogUnormalizedScore(LinkVar.TRUE) != 0.0) {
                        System.out.printf("p=%d c=%d lv=FALSE sc=%f f=%d \n", p, c, f.getLogUnormalizedScore(LinkVar.TRUE), i);
                    }
                }
                i++;
            }
            throw new IllegalStateException("Scores disallowed all possible parses.");
        }
        if (log.isTraceEnabled()) { sc.prettyPrint(graph); }
        edgeMarg = getEdgeMarginals(graph, sc);
    }
    
    private DependencyScorer getDepScorerFromFg(FactorGraph fg, int n) {
        // Create the scores in the log semiring.
        double[][][] scores = new double[n+1][n+1][n+1];
        DoubleArrays.fill(scores, 0.0);
        boolean containsProjDepTreeConstraint = false;
        for (Factor f : fg.getFactors()) {
            if (f instanceof ProjDepTreeFactor || f instanceof SimpleProjDepTreeFactor) {
                containsProjDepTreeConstraint = true;
            } else if (f instanceof GraFeTypedFactor && ((GraFeTypedFactor) f).getFactorType() == DepParseFactorTemplate.GRANDPARENT) {
                GraFeTypedFactor ff = (GraFeTypedFactor) f;
                scores[ff.p+1][ff.c+1][ff.g+1] += ff.getLogUnormalizedScore(LinkVar.TRUE_TRUE);
            } else if (f.getVars().size() == 1 && f.getVars().get(0) instanceof LinkVar) {
                LinkVar lv = (LinkVar) f.getVars().get(0);
                int p = lv.getParent() + 1;
                int c = lv.getChild() + 1;
                for (int g=0; g<n+1; g++) {
                    if (p <= g && g <= c && !(p==0 && g == O2AllGraDpHypergraph.NIL)) { continue; }
                    scores[p][c][g] += f.getLogUnormalizedScore(LinkVar.TRUE);
                }
            } else if (f.getVars().size() == 0) {
                // Ignore clamped factor.
            } else {
                throw new RuntimeException("Unsupported factor type: " + f.getClass());
            }
        }
        if (!containsProjDepTreeConstraint) {
            throw new IllegalStateException("This inference method is only applicable to factor graphs containing "
                        + " a factor constraining to a projective dependency tree.");
        }
        
        // Convert the scores to the semiring used by this inference method.
        Algebras.convertAlgebra(scores, Algebras.LOG_SEMIRING, s);
        
        if (log.isTraceEnabled()) { log.trace("scores: " + Arrays.deepToString(scores)); }
        return new ExplicitDependencyScorer(scores, n);
    }

    /** Gets the edge marginals in the real semiring from an all-grandparents hypergraph and its marginals in scores. */
    private static EdgeScores getEdgeMarginals(O2AllGraDpHypergraph graph, Scores sc) {
        Algebra s = graph.getAlgebra();
        int nplus = graph.getNumTokens()+1;

        Hypernode[][][][] c = graph.getChart();
        EdgeScores edgeMarg = new EdgeScores(graph.getNumTokens(), s.zero());
        for (int width = 1; width < nplus; width++) {
            for (int i = 0; i < nplus - width; i++) {
                int j = i + width;
                for (int g=0; g<nplus; g++) {
                    if (i <= g && g <= j && !(i==0 && g==O2AllGraDpHypergraph.NIL)) { continue; }
                    if (j > 0) {
                        double m = sc.marginal[c[i][j][g][O2AllGraDpHypergraph.INCOMPLETE].getId()];
                        edgeMarg.setScore(i-1, j-1, s.plus(edgeMarg.getScore(i-1, j-1), m));
                    }
                    if (i > 0) {
                        double m = sc.marginal[c[j][i][g][O2AllGraDpHypergraph.INCOMPLETE].getId()];
                        edgeMarg.setScore(j-1, i-1, s.plus(edgeMarg.getScore(j-1, i-1), m));
                    }
                }
            }
        }
        return edgeMarg;
    }

    @Override
    protected VarTensor getVarBeliefs(Var var) {
        LinkVar lv = (LinkVar)var;
        int p = lv.getParent();
        int c = lv.getChild();
        double lv1 = edgeMarg.getScore(p, c);
        double lv0 = s.minus(s.one(), lv1);
        VarTensor b = new VarTensor(s, new VarSet(var));
        b.setValue(LinkVar.TRUE, lv1);
        b.setValue(LinkVar.FALSE, lv0);
        return b;
    }

    @Override
    protected VarTensor getFactorBeliefs(Factor f) {
        if (f instanceof GraFeTypedFactor && ((GraFeTypedFactor) f).getFactorType() == DepParseFactorTemplate.GRANDPARENT) {
            GraFeTypedFactor ff = (GraFeTypedFactor) f;
            int g = ff.g+1;
            int p = ff.p+1;
            int c = ff.c+1;                        
            VarTensor b = new VarTensor(s, f.getVars());
            b.fill(s.zero());
            int id = graph.getChart()[p][c][g][O2AllGraDpHypergraph.INCOMPLETE].getId();            
            b.setValue(LinkVar.TRUE_TRUE, sc.marginal[id]);
            log.trace(String.format("p=%d c=%d g=%d b=%s", p, c, g, b.toString()));
            return b;
        } else if (f.getVars().size() == 1 && f.getVars().get(0) instanceof LinkVar) {
            return getVarBeliefs(f.getVars().get(0));
        } else if (f.getVars().size() == 0) {
            return new VarTensor(s, new VarSet());
        //} else if (f instanceof ProjDepTreeFactor || f instanceof SimpleProjDepTreeFactor) {
        } else {
            throw new RuntimeException("Unsupported factor type: " + f.getClass());
        }
    }

    @Override
    public double getPartitionBelief() {
        return sc.beta[graph.getRoot().getId()];
    }

    @Override
    public FactorGraph getFactorGraph() {
        return fg;
    }

    @Override
    public Algebra getAlgebra() {
        return s;
    }

    public EdgeScores getEdgeMarginals() {
        return edgeMarg;
    }

}
