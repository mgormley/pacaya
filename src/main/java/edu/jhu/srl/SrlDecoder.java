package edu.jhu.srl;

import java.util.Arrays;
import java.util.List;

import edu.jhu.data.DepTree;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.data.conll.SrlGraph.SrlArg;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.data.conll.SrlGraph.SrlPred;
import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.decode.MbrDecoder;
import edu.jhu.gm.decode.MbrDecoder.MbrDecoderPrm;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.parse.dep.ProjectiveDependencyParser;
import edu.jhu.srl.SrlFactorGraph.RoleVar;
import edu.jhu.srl.SrlFactorGraph.SenseVar;
import edu.jhu.util.Utilities;

public class SrlDecoder {

    public static class SrlDecoderPrm {
        public MbrDecoderPrm mbrPrm = null;
    }

    private SrlDecoderPrm prm;    
    // Cached MBR parents.
    private int[] parents;
    // Cached MBR SRL graph.
    private SrlGraph srlGraph;
    // Cached MBR variable assignment.
    private VarConfig mbrVarConfig;

    public SrlDecoder(SrlDecoderPrm prm) {
        this.prm = prm;
    }

    /**
     * Decodes the example using the given model.
     * 
     * @param model The model.
     * @param ex The example to decode.
     */
    public void decode(FgModel model, FgExample ex) {
        MbrDecoder mbrDecoder = new MbrDecoder(prm.mbrPrm);
        mbrDecoder.decode(model, ex);
        SrlFactorGraph srlFg = (SrlFactorGraph) ex.getOriginalFactorGraph();
        int n = srlFg.getSentenceLength();
        mbrVarConfig = mbrDecoder.getMbrVarConfig();

        // Get the SRL graph.
        srlGraph = getSrlGraphFromVarConfig(mbrVarConfig, n);
        // Get the dependency tree.
        parents = getParents(mbrDecoder.getVarMarginals(), ex.getFgLatPred().getVars(), n);
        if (parents != null) {
            // Update predictions with parse.
            for (int p=-1; p<n; p++) {
                for (int c=0; c<n; c++) {
                    int state = (parents[c] == p) ? LinkVar.TRUE : LinkVar.FALSE;
                    if (srlFg.getLinkVar(p, c) != null) {
                        mbrVarConfig.put(srlFg.getLinkVar(p, c), state);
                    }
                }
            }
        }
    }

    public static SrlGraph getSrlGraphFromVarConfig(VarConfig vc, int n) {
        SrlGraph srlGraph = new SrlGraph(n);
        for (Var v : vc.getVars()) {
            if (v instanceof RoleVar && v.getType() != VarType.LATENT) {
                // Decode the Role var.
                RoleVar role = (RoleVar) v;
                SrlPred pred = srlGraph.getPredAt(role.getParent());
                if (pred == null) {
                    // We need some predicate variable here. If necessary, the
                    // state will be updated below.
                    String sense = "NO.SENSE.PREDICTED";
                    pred = new SrlPred(role.getParent(), sense);
                }
                SrlArg arg = srlGraph.getArgAt(role.getChild());
                if (arg == null) {
                    arg = new SrlArg(role.getChild());
                }
                SrlEdge edge = new SrlEdge(pred, arg, vc.getStateName(role));
                srlGraph.addEdge(edge);
            } else if (v instanceof SenseVar && v.getType() == VarType.PREDICTED) {
                // Decode the Sense var.
                SenseVar sense = (SenseVar) v;
                SrlPred pred = srlGraph.getPredAt(sense.getParent());
                if (pred == null) {
                    pred = new SrlPred(sense.getParent(), vc.getStateName(sense));
                    srlGraph.addPred(pred);
                } else {
                    pred.setLabel(vc.getStateName(sense));
                }
            }
        }
        return srlGraph;
    }
    
    public static int[] getParents(List<DenseFactor> margs, List<Var> vars, int n) {
        int linkVarCount = 0;
        
        // Build up the beliefs about the link variables (if present),
        // and compute the MBR dependency parse.
        double[] root = new double[n];
        double[][] child = new double[n][n];
        Utilities.fill(root, Double.NEGATIVE_INFINITY);
        Utilities.fill(child, Double.NEGATIVE_INFINITY);
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

    public int[] getParents() {
        return parents;
    }
    
    public SrlGraph getSrlGraph() {
        return srlGraph;
    }
    
    public VarConfig getMbrVarConfig() {
        return mbrVarConfig;
    }
}
