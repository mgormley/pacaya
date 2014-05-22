package edu.jhu.srl;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.data.DepEdgeMask;
import edu.jhu.data.DepTree;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor.LinkVar;
import edu.jhu.parse.dep.EdgeScores;
import edu.jhu.parse.dep.ProjectiveDependencyParser;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.prim.util.math.FastMath;

public class DepParseDecoder {

    private static final Logger log = Logger.getLogger(DepParseDecoder.class);

    public static int[] getParents(List<DenseFactor> margs, List<Var> vars, int n) {        
        // Build up the beliefs about the link variables (if present),
        // and compute the MBR dependency parse.
        Pair<EdgeScores, Integer> pair = getEdgeScores(margs, vars, n, false);
        EdgeScores scores = pair.get1();
        int linkVarCount = pair.get2();
        
        if (linkVarCount > 0) {
            // Get MBR parse, by finding the argmax tree where we treat the
            // score of a tree as the sum of the edge scores.
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

                double belief;
                if (logOdds) {
                    // TODO: Using logOdds is the method of MBR decoding
                    // prescribed in Smith & Eisner (2008). However, this breaks the parser
                    // when the log-odds are positive infinity.
                    belief = FastMath.log(marg.getValue(LinkVar.TRUE) / marg.getValue(LinkVar.FALSE));
                } else {
                    belief = marg.getValue(LinkVar.TRUE);
                }
                if (p == -1) {
                    scores.root[c] = belief;
                } else {
                    scores.child[p][c] = belief;
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
            DepEdgeMask mask = new DepEdgeMask(n, false);
            for (int p=-1; p<n; p++) {
                for (int c=0; c<n; c++) {
                    if (p == c) { continue; }
                    double marg = scores.getScore(p, c);
                    // In probability domain: marg < propMaxMarg * maxMargForTok[c];
                    if (log.isTraceEnabled()) {
                        log.trace(String.format("p=%d c=%d marg=%f maxMarg=%f thresh=%f", p, c, marg, maxMargForTok[c], propMaxMarg * maxMargForTok[c]));
                    }
                    if (marg < propMaxMarg * maxMargForTok[c]) {
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
