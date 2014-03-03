package edu.jhu.srl;

import java.util.Arrays;
import java.util.List;

import edu.jhu.data.DepTree;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.parse.dep.ProjectiveDependencyParser;
import edu.jhu.prim.arrays.DoubleArrays;

public class DepParseDecoder {

    public static int[] getParents(List<DenseFactor> margs, List<Var> vars, int n) {
        int linkVarCount = 0;
        
        // Build up the beliefs about the link variables (if present),
        // and compute the MBR dependency parse.
        double[] root = new double[n];
        double[][] child = new double[n][n];
        DoubleArrays.fill(root, Double.NEGATIVE_INFINITY);
        DoubleArrays.fill(child, Double.NEGATIVE_INFINITY);
        for (int varId = 0; varId < vars.size(); varId++) {
            Var var = vars.get(varId);
            DenseFactor marg = margs.get(varId);
            if (var instanceof LinkVar && (var.getType() == VarType.LATENT || var.getType() == VarType.PREDICTED)) {
                LinkVar link = ((LinkVar)var);
                int c = link.getChild();
                int p = link.getParent();
                double logBelief =  marg.getValue(LinkVar.TRUE) - marg.getValue(LinkVar.FALSE);
                if (p == -1) {
                    root[c] = logBelief;
                } else {
                    child[p][c] = logBelief;
                }
                linkVarCount++;
            }
        }
        
        if (linkVarCount > 0) {
            int[] parents = new int[n];
            Arrays.fill(parents, DepTree.EMPTY_POSITION);
            ProjectiveDependencyParser.parse(root, child, parents);
            return parents;
        } else {
            return null;
        }        
    }

}
