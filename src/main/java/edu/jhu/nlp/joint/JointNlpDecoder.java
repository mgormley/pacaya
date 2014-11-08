package edu.jhu.nlp.joint;


import java.util.List;

import edu.jhu.gm.app.Decoder;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.decode.MbrDecoder;
import edu.jhu.gm.decode.MbrDecoder.MbrDecoderPrm;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.nlp.data.DepEdgeMask;
import edu.jhu.nlp.data.conll.SrlGraph;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.depparse.DepEdgeMaskDecoder;
import edu.jhu.nlp.depparse.DepEdgeMaskDecoder.DepEdgeMaskDecoderPrm;
import edu.jhu.nlp.depparse.DepParseDecoder;
import edu.jhu.nlp.relations.RelationsDecoder;
import edu.jhu.nlp.srl.SrlDecoder;

/**
 * Decodes from the marginals for a joint NLP factor graph to a new {@link AnnoSentence} with the
 * predictions.
 * 
 * @author mgormley
 */
public class JointNlpDecoder implements Decoder<AnnoSentence, AnnoSentence> {

    public static class JointNlpDecoderPrm {
        public MbrDecoderPrm mbrPrm = null;
        public DepEdgeMaskDecoderPrm maskPrm = new DepEdgeMaskDecoderPrm();
    }

    private JointNlpDecoderPrm prm;

    public JointNlpDecoder(JointNlpDecoderPrm prm) {
        this.prm = prm;
    }

    @Override
    public AnnoSentence decode(FgInferencer inf, UFgExample ex, AnnoSentence sent) {
        MbrDecoder mbrDecoder = new MbrDecoder(prm.mbrPrm);
        mbrDecoder.decode(inf, ex);
        return decode(ex, mbrDecoder, inf, sent);
    }

    public AnnoSentence decode(JointNlpFgModel model, UFgExample ex, AnnoSentence sent) {
        MbrDecoder mbrDecoder = new MbrDecoder(prm.mbrPrm);
        FgInferencer infLatPred = mbrDecoder.decode(model, ex);
        return decode(ex, mbrDecoder, infLatPred, sent);
    }

    private AnnoSentence decode(UFgExample ex, MbrDecoder mbrDecoder, FgInferencer inf, AnnoSentence sent) {
        JointNlpFactorGraph fg = (JointNlpFactorGraph) ex.getOriginalFactorGraph();
        int n = fg.getSentenceLength();
        VarConfig mbrVarConfig = mbrDecoder.getMbrVarConfig();

        AnnoSentence predSent = sent.getShallowCopy();

        // Get the SRL graph.
        SrlGraph srlGraph = SrlDecoder.getSrlGraphFromVarConfig(mbrVarConfig, n);
        if (srlGraph != null) {
            predSent.setSrlGraph(srlGraph);
        }
        // Get the dependency tree.
        int[] parents = (new DepParseDecoder()).decode(inf, ex, sent);
        if (parents != null) {
            DepParseDecoder.addDepParseAssignment(parents, fg.getDpBuilder(), mbrVarConfig);
            predSent.setParents(parents);
        }
        // Get the relations.
        List<String> rels = RelationsDecoder.getRelLabelsFromVarConfig(mbrVarConfig);
        if (rels != null) {
            predSent.setRelLabels(rels);
        }
        
        return predSent;
    }
    
}
