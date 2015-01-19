package edu.jhu.nlp.depparse;

import edu.jhu.gm.app.Encoder;
import edu.jhu.gm.data.LFgExample;
import edu.jhu.gm.data.LabeledFgExample;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.data.UnlabeledFgExample;
import edu.jhu.gm.feat.FeatureCache;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.globalfac.LinkVar;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder.DepParseFactorGraphBuilderPrm;
import edu.jhu.nlp.depparse.DepParseFeatureExtractor.DepParseFeatureExtractorPrm;
import edu.jhu.util.FeatureNames;

/**
 * Encodes a dependency tree factor graph and variable assignment from the words and pruning mask
 * from an {@link AnnoSentence} and the gold parents array.
 * 
 * @author mgormley
 */
public class DepParseEncoder implements Encoder<AnnoSentence, int[]> {

    public static class DepParseEncoderPrm {
        // TODO: Fill w/non-null values.
        public DepParseFeatureExtractorPrm dpFePrm = null;
        public DepParseFactorGraphBuilderPrm dpPrm = null;
    }
    
    private DepParseEncoderPrm prm;
    private CorpusStatistics cs;
    private FeatureNames feAlphabet;
    
    public DepParseEncoder(DepParseEncoderPrm prm, CorpusStatistics cs, FeatureNames feAlphabet) {
        this.cs = cs;
        this.feAlphabet = feAlphabet;
        this.prm = prm;
    }

    @Override
    public LFgExample encode(AnnoSentence sent, int[] parents) {
        return getExample(sent, parents, true);
    }

    @Override
    public UFgExample encode(AnnoSentence sent) {
        return getExample(sent, null, false);
    }

    private LFgExample getExample(AnnoSentence sent, int[] parents, boolean labeledExample) {
        FeatureExtractor fe = prm.dpFePrm.onlyFast ?
                new BitshiftDepParseFeatureExtractor(sent, cs, prm.dpFePrm.featureHashMod, feAlphabet) :
                new DepParseFeatureExtractor(prm.dpFePrm, sent, cs, feAlphabet);
        fe = new FeatureCache(fe);
        
        FactorGraph fg = new FactorGraph();
        DepParseFactorGraphBuilder dp = new DepParseFactorGraphBuilder(prm.dpPrm);
        dp.build(sent.getWords(), sent.getDepEdgeMask(), fe, fg);
        
        VarConfig goldConfig = new VarConfig();
        addDepParseTrainAssignment(parents, dp, goldConfig);
        if (labeledExample) {
            return new LabeledFgExample(fg, goldConfig, fe);
        } else {
            return new UnlabeledFgExample(fg, goldConfig, fe);
        }
    }
    
    public static void addDepParseTrainAssignment(int[] parents, DepParseFactorGraphBuilder dp, VarConfig vc) {
        int n = parents.length;
        // LINK VARS
        // Add all the training data assignments to the link variables, if they are not latent.
        // IMPORTANT NOTE: We include the case where the parent is the Wall node (position -1).
        for (int p=-1; p<n; p++) {
            for (int c=0; c<n; c++) {
                if (c != p && dp.getLinkVar(p, c) != null) {
                    LinkVar linkVar = dp.getLinkVar(p, c);
                    if (linkVar.getType() != VarType.LATENT) {
                        // Syntactic head, from dependency parse.
                        int state;
                        if (parents[c] != p) {
                            state = LinkVar.FALSE;
                        } else {
                            state = LinkVar.TRUE;
                        }
                        vc.put(linkVar, state);
                    }
                }
            }
        }
    }

}
