package edu.jhu.nlp.srl;

import edu.jhu.gm.app.Decoder;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.decode.MbrDecoder;
import edu.jhu.gm.decode.MbrDecoder.MbrDecoderPrm;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.nlp.data.conll.SrlGraph;
import edu.jhu.nlp.data.conll.SrlGraph.SrlArg;
import edu.jhu.nlp.data.conll.SrlGraph.SrlEdge;
import edu.jhu.nlp.data.conll.SrlGraph.SrlPred;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.srl.SrlFactorGraphBuilder.RoleVar;
import edu.jhu.nlp.srl.SrlFactorGraphBuilder.SenseVar;

/**
 * Decoder from the marginals of a semantic role labeling to an {@link SrlGraph}.
 *  
 * @author mgormley
 */
public class SrlDecoder implements Decoder<AnnoSentence, SrlGraph> {

    public static class SrlDecoderPrm {
        public MbrDecoderPrm mbrPrm = new MbrDecoderPrm();
    }
    
    private SrlDecoderPrm prm;
    
    public SrlDecoder(SrlDecoderPrm prm) {
        this.prm = prm;
    }
    
    @Override
    public SrlGraph decode(FgInferencer inf, UFgExample ex, AnnoSentence sent) {
        MbrDecoder mbrDecoder = new MbrDecoder(prm.mbrPrm);
        mbrDecoder.decode(inf, ex);
        VarConfig mbrVarConfig = mbrDecoder.getMbrVarConfig();
        // Get the SRL graph.
        return SrlDecoder.getSrlGraphFromVarConfig(mbrVarConfig, sent.size());
    }
    
    public static SrlGraph getSrlGraphFromVarConfig(VarConfig vc, int n) {
        int srlVarCount = 0;
        
        SrlGraph srlGraph = new SrlGraph(n);
        for (Var v : vc.getVars()) {
            if (v instanceof RoleVar && v.getType() != VarType.LATENT) {
                // Decode the Role var.
                RoleVar role = (RoleVar) v;
                String stateName = vc.getStateName(role);
                if (!"_".equals(stateName)) {
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
                    SrlEdge edge = new SrlEdge(pred, arg, stateName);
                    srlGraph.addEdge(edge);
                }
                srlVarCount++;
            } else if (v instanceof SenseVar && v.getType() == VarType.PREDICTED) {
                // Decode the Sense var.
                SenseVar sense = (SenseVar) v;
                String predLabel = vc.getStateName(sense);
                if ("_".equals(predLabel)) {
                    // Predicate ID said there's no predicate here.
                } else {
                    // Adding the identified predicate.
                    SrlPred pred = srlGraph.getPredAt(sense.getParent());
                    if (pred == null) {
                        pred = new SrlPred(sense.getParent(), predLabel);
                        srlGraph.addPred(pred);
                    } else {
                        pred.setLabel(predLabel);
                    }
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
