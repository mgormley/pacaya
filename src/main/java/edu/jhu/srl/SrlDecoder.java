package edu.jhu.srl;

import edu.jhu.data.conll.SrlGraph;
import edu.jhu.data.conll.SrlGraph.SrlArg;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.data.conll.SrlGraph.SrlPred;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.srl.SrlFactorGraphBuilder.RoleVar;
import edu.jhu.srl.SrlFactorGraphBuilder.SenseVar;

public class SrlDecoder {

    public static SrlGraph getSrlGraphFromVarConfig(VarConfig vc, int n) {
        int srlVarCount = 0;
        
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
                srlVarCount++;
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
                srlVarCount++;
            }
        }
        if (srlVarCount > 0) {
            return srlGraph;
        } else {
            return null;
        }
    }

}
