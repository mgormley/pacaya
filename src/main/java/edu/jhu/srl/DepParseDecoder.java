package edu.jhu.srl;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.data.DepEdgeMask;
import edu.jhu.data.DepTree;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.parse.dep.ProjectiveDependencyParser;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.prim.util.math.FastMath;

public class DepParseDecoder {

    private static final Logger log = Logger.getLogger(DepParseDecoder.class);

    public static class EdgeScores {
        public double[] root;
        public double[][] child;
        public EdgeScores(int n, double value) {
            this.root = new double[n];
            this.child = new double[n][n];
            DoubleArrays.fill(root, value);
            DoubleArrays.fill(child, value);
        }
        public EdgeScores(double[] root, double[][] child) {
            this.root = root;
            this.child = child;
        }
        public double getScore(int p, int c) {
            return (p == -1) ? root[c] : child[p][c];
        }
    }
    
    public static int[] getParents(List<DenseFactor> margs, List<Var> vars, int n) {        
        // Build up the beliefs about the link variables (if present),
        // and compute the MBR dependency parse.
        Pair<EdgeScores, Integer> pair = getEdgeScores(margs, vars, n, true);
        EdgeScores scores = pair.get1();
        int linkVarCount = pair.get2();
        
        if (linkVarCount > 0) {
            int[] parents = new int[n];
            Arrays.fill(parents, DepTree.EMPTY_POSITION);
            ProjectiveDependencyParser.parse(scores.root, scores.child, parents);
            return parents;
        } else {
            return null;
        }
    }

    private static Pair<EdgeScores, Integer> getEdgeScores(List<DenseFactor> margs, List<Var> vars, int n, boolean logOdds) {
        int linkVarCount = 0;
        EdgeScores scores = new EdgeScores(n, Double.NEGATIVE_INFINITY);
        for (int varId = 0; varId < vars.size(); varId++) {
            Var var = vars.get(varId);
            DenseFactor marg = margs.get(varId);
            if (var instanceof LinkVar && (var.getType() == VarType.LATENT || var.getType() == VarType.PREDICTED)) {
                LinkVar link = ((LinkVar)var);
                int c = link.getChild();
                int p = link.getParent();
                // TODO: Check if this is the logDomain!!!
                assert SrlRunner.logDomain == true;
                double logBelief = marg.getValue(LinkVar.TRUE);
                if (logOdds) {
                    logBelief = marg.getValue(LinkVar.TRUE) - marg.getValue(LinkVar.FALSE);
                }
                if (p == -1) {
                    scores.root[c] = logBelief;
                } else {
                    scores.child[p][c] = logBelief;
                }
                linkVarCount++;
            }
        }
        return new Pair<EdgeScores, Integer>(scores, linkVarCount);
    }

    public static DepEdgeMask getDepEdgeMask(List<DenseFactor> margs, List<Var> vars, int n, double propMaxMarg) {
        Pair<EdgeScores, Integer> pair = getEdgeScores(margs, vars, n, false);
        EdgeScores scores = pair.get1();
        int linkVarCount = pair.get2();
        if (linkVarCount > 0) {
            // Get the max head marginal for each token.
            double[] maxMargForTok = new double[n];
            Arrays.fill(maxMargForTok, Double.NEGATIVE_INFINITY);
            for (int c=0; c<n; c++) {
                for (int p=-1; p<n; p++) {
                    if (p == c) { continue; }
                    double marg = scores.getScore(p, c);
                    if (marg > maxMargForTok[c]) {
                        maxMargForTok[c] = marg;                                
                    }
                }
            }
            // For each token, prune any heads for which the marginal
            // probability is less than propMaxMarg (e.g. 0.0001) times the
            // maximum head marginal for that token.
            double logPropMaxMarg = FastMath.log(propMaxMarg);
            DepEdgeMask mask = new DepEdgeMask(n, false);
            for (int p=-1; p<n; p++) {
                for (int c=0; c<n; c++) {
                    if (p == c) { continue; }
                    double marg = scores.getScore(p, c);
                    // In probability domain: marg < propMaxMarg * maxMargForTok[c];
                    if (log.isTraceEnabled()) {
                        log.trace(String.format("p=%d c=%d marg=%f maxMarg=%f thresh=%f", p, c, marg, maxMargForTok[c], logPropMaxMarg + maxMargForTok[c]));
                    }
                    if (marg < logPropMaxMarg + maxMargForTok[c]) {
                        mask.setIsKept(p, c, false);
                    } else {
                        mask.setIsKept(p, c, true);
                    }
                }
            }
            
            // Check that each child has at least one parent. This should always be the case.
            for (int c=0; c<n; c++) {
                if (mask.getParentCount(c) == 0) {
                    String msg = String.format("") + mask;
                    throw new IllegalStateException(msg);
                }
            }
            return mask;
        } else {
            return null;
        }
    }

}
