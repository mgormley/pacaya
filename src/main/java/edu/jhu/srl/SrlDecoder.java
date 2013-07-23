package edu.jhu.srl;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.data.conll.SrlGraph.SrlArg;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.data.conll.SrlGraph.SrlPred;
import edu.jhu.gm.Var;
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.VarConfig;
import edu.jhu.srl.SrlFactorGraph.RoleVar;

public class SrlDecoder {

    public static SrlGraph getSrlGraphFromVarConfig(VarConfig vc, CoNLL09Sentence sent) {
        SrlGraph srlGraph = new SrlGraph(sent.size());
        for (Var v : vc.getVars()) {
            if (v instanceof RoleVar && v.getType() != VarType.LATENT) {
                RoleVar role = (RoleVar) v;
                SrlPred pred = srlGraph.getPredAt(role.getParent());
                if (pred == null) {
                    // TODO: also decode the Sense of the predicate.
                    String sense = "NO.SENSE.PREDICTED";
                    pred = new SrlPred(role.getParent(), sense);
                }
                SrlArg arg = srlGraph.getArgAt(role.getChild());
                if (arg == null) {
                    arg = new SrlArg(role.getChild());
                }
                SrlEdge edge = new SrlEdge(pred, arg, vc.getStateName(role));
                srlGraph.addEdge(edge);
            }
        }
        return srlGraph;
    }

}
