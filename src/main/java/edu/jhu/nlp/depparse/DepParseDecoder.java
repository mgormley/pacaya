package edu.jhu.nlp.depparse;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.autodiff.erma.InsideOutsideDepParse;
import edu.jhu.gm.app.Decoder;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor.LinkVar;
import edu.jhu.nlp.data.DepTree;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.parse.dep.EdgeScores;
import edu.jhu.parse.dep.ProjectiveDependencyParser;
import edu.jhu.prim.tuple.Pair;

/**
 * Decodes from the marginals of a factor graph for dependency parsing to an int[] representing
 * the parent for each token.
 * 
 * This computes the MBR tree under an accuracy loss function. Note that this is similar to Smith &
 * Eisner (2008)'s proposed method of decoding, but corrects a bug in their paper.
 * 
 * @author mgormley
 */
public class DepParseDecoder implements Decoder<AnnoSentence, int[]> {

    private static final Logger log = Logger.getLogger(DepParseDecoder.class);

    /**
     * Decodes by computing the MBR tree under an accuracy loss function.
     */
    @Override
    public int[] decode(FgInferencer inf, UFgExample ex, AnnoSentence sent) {
        FactorGraph fg = ex.getFgLatPred();
        int n = sent.size();
        
        // Build up the beliefs about the link variables (if present),
        // and compute the MBR dependency parse.
        Pair<EdgeScores, Integer> pair = getEdgeScores(inf, fg, n);
        EdgeScores scores = pair.get1();
        int linkVarCount = pair.get2();
        
        if (linkVarCount > 0) {
            return getParents(scores);
        } else {
            return null;
        }
    }

    public static int[] getParents(EdgeScores scores) {
        // Get MBR parse, by finding the argmax tree where we treat the
        // score of a tree as the sum of the edge scores.
        int n = scores.root.length;
        int[] parents = new int[n];
        Arrays.fill(parents, DepTree.EMPTY_POSITION);
        if (InsideOutsideDepParse.singleRoot) {
            ProjectiveDependencyParser.parseSingleRoot(scores.root, scores.child, parents);
        } else {
            ProjectiveDependencyParser.parseMultiRoot(scores.root, scores.child, parents);
        }
        return parents;
    }

    // Package-private for DepEdgeMaskDecoder.
    static Pair<EdgeScores, Integer> getEdgeScores(FgInferencer inf, FactorGraph fg, int n) {
        List<Var> vars = fg.getVars();
        int linkVarCount = 0;
        EdgeScores scores = new EdgeScores(n, Double.NEGATIVE_INFINITY);
        for (int varId = 0; varId < vars.size(); varId++) {
            Var var = vars.get(varId);
            VarTensor marg = inf.getMarginals(var);
            if (var instanceof LinkVar && (var.getType() == VarType.LATENT || var.getType() == VarType.PREDICTED)) {
                LinkVar link = ((LinkVar)var);
                int c = link.getChild();
                int p = link.getParent();

                // Using logOdds is the method of MBR decoding prescribed in Smith &
                // Eisner (2008), but that's a bug in their paper. This breaks the parser
                // when the log-odds are positive infinity.
                // INCORRECT: belief = FastMath.log(marg.getValue(LinkVar.TRUE) / marg.getValue(LinkVar.FALSE));
                double belief = marg.getValue(LinkVar.TRUE);

                if (p == -1) {
                    scores.root[c] = belief;
                } else {
                    scores.child[p][c] = belief;
                }
                linkVarCount++;
            }
        }
        if (linkVarCount > 0 && n*n != linkVarCount) {
            throw new RuntimeException("Currently, EdgeScores only supports decoding all the LinkVars, not a subset.");
        }
        return new Pair<EdgeScores, Integer>(scores, linkVarCount);
    }

    /** This method is identical to its counterpart in DepParseEncoder except that it does not ignore LATENT vars. */
    public static void addDepParseAssignment(int[] parents, DepParseFactorGraphBuilder fg, VarConfig vc) {
        int n = parents.length;
        // Update predictions with parse.
        for (int p=-1; p<n; p++) {
            for (int c=0; c<n; c++) {
                if (p == c) { continue; }
                int state = (parents[c] == p) ? LinkVar.TRUE : LinkVar.FALSE;
                if (fg.getLinkVar(p, c) != null) {
                    vc.put(fg.getLinkVar(p, c), state);
                }
            }
        }
    }

}
