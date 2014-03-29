package edu.jhu.srl;


import edu.jhu.data.DepEdgeMask;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.gm.data.UnlabeledFgExample;
import edu.jhu.gm.decode.MbrDecoder;
import edu.jhu.gm.decode.MbrDecoder.MbrDecoderPrm;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.VarConfig;

// TODO: This should modify a {@link SimpleAnnoSentence} rather than cache the parents and srlGraph explicitly.
public class JointNlpDecoder {

    public static class JointNlpDecoderPrm {
        public MbrDecoderPrm mbrPrm = null;
        public double pruneMargProp = 0.0001;
    }

    private JointNlpDecoderPrm prm;    
    // Cached MBR parents.
    private int[] parents;
    // Cached MBR SRL graph.
    private SrlGraph srlGraph;
    // Cached MBR variable assignment.
    private VarConfig mbrVarConfig;
    private DepEdgeMask depEdgeMask;

    public JointNlpDecoder(JointNlpDecoderPrm prm) {
        this.prm = prm;
    }

    /**
     * Decodes the example using the given model.
     * 
     * @param model The model.
     * @param ex The example to decode.
     */
    public void decode(FgModel model, UnlabeledFgExample ex) {
        MbrDecoder mbrDecoder = new MbrDecoder(prm.mbrPrm);
        mbrDecoder.decode(model, ex);
        JointNlpFactorGraph srlFg = (JointNlpFactorGraph) ex.getOriginalFactorGraph();
        int n = srlFg.getSentenceLength();
        mbrVarConfig = mbrDecoder.getMbrVarConfig();

        // Get the SRL graph.
        srlGraph = SrlDecoder.getSrlGraphFromVarConfig(mbrVarConfig, n);
        // Get the dependency tree.
        parents = DepParseDecoder.getParents(mbrDecoder.getVarMarginals(), ex.getFgLatPred().getVars(), n);
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
        depEdgeMask = DepParseDecoder.getDepEdgeMask(mbrDecoder.getVarMarginals(), ex.getFgLatPred().getVars(), n, prm.pruneMargProp);
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

    public DepEdgeMask getDepEdgeMask() {
        return depEdgeMask;
    }
}
