package edu.jhu.nlp.joint;


import edu.jhu.data.DepEdgeMask;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.gm.app.Decoder;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.decode.MbrDecoder;
import edu.jhu.gm.decode.MbrDecoder.MbrDecoderPrm;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.nlp.depparse.DepParseDecoder;
import edu.jhu.nlp.srl.SrlDecoder;

// TODO: This should modify a {@link AnnoSentence} rather than cache the parents and srlGraph explicitly.
public class JointNlpDecoder implements Decoder<AnnoSentence, AnnoSentence> {

    public static class JointNlpDecoderPrm {
        public MbrDecoderPrm mbrPrm = null;
        public double pruneMargProp = 0.0001;
        public int maxPrunedHeads = 10;
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

    public AnnoSentence decode(JointNlpFgModel model, UFgExample ex, AnnoSentence predSent) {
        decode(model, ex);        
        setAllPreds(predSent);        
        return predSent;
    }

    @Override
    public AnnoSentence decode(FgInferencer inf, UFgExample ex, AnnoSentence predSent) {
        decode(inf, ex);        
        setAllPreds(predSent);        
        return predSent;
    }

    private void setAllPreds(AnnoSentence predSent) {
        // Update SRL graph on the sentence. 
        if (srlGraph != null) {
            predSent.setSrlGraph(srlGraph);
        }
        // Update the dependency tree on the sentence.
        if (parents != null) {
            predSent.setParents(parents);
        }
        // Update the dependency mask on the sentence.
        if (depEdgeMask != null) {
            predSent.setDepEdgeMask(depEdgeMask);
        }
    }

    /**
     * Decodes the example using the given model.
     * 
     * @param model The model.
     * @param ex The example to decode.
     */
    public void decode(FgModel model, UFgExample ex) {
        MbrDecoder mbrDecoder = new MbrDecoder(prm.mbrPrm);
        mbrDecoder.decode(model, ex);
        decode(ex, mbrDecoder);
    }
    
    public void decode(FgInferencer infLatPred, UFgExample ex) {
        MbrDecoder mbrDecoder = new MbrDecoder(prm.mbrPrm);
        mbrDecoder.decode(infLatPred, ex);
        decode(ex, mbrDecoder);
    }

    private void decode(UFgExample ex, MbrDecoder mbrDecoder) {
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
        depEdgeMask = DepParseDecoder.getDepEdgeMask(mbrDecoder.getVarMarginals(), ex.getFgLatPred().getVars(), n,
                prm.pruneMargProp, prm.maxPrunedHeads);
    }

    public int[] getParents() {
        return parents;
    }
    
    public SrlGraph getSrlGraph() {
        return srlGraph;
    }

    public DepEdgeMask getDepEdgeMask() {
        return depEdgeMask;
    }
    
    public VarConfig getMbrVarConfig() {
        return mbrVarConfig;
    }
    
}
