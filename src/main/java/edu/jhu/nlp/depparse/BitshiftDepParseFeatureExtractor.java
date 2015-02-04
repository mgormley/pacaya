package edu.jhu.nlp.depparse;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.model.FeExpFamFactor;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.globalfac.LinkVar;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.FeTypedFactor;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.IntAnnoSentence;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder.DepParseFactorTemplate;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder.GraFeTypedFactor;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder.SibFeTypedFactor;
import edu.jhu.util.FeatureNames;
import edu.jhu.util.Prm;
import edu.jhu.util.cli.Opt;

public class BitshiftDepParseFeatureExtractor implements FeatureExtractor {

    public static class BitshiftDepParseFeatureExtractorPrm extends Prm {
        private static final long serialVersionUID = 1L;
        public int featureHashMod = 10000000;
        @Opt(description = "Whether to use MST style word-pair features")
        public boolean useMstFeats = false;
        @Opt(description = "Whether to use Carerras style 2nd-order features")
        public boolean useCarerrasFeats = false;
        @Opt(description = "Whether to use coarse POS tag features (TurboParser and MST features)")
        public boolean useCoarseTags = true;
        /* Options for TurboParser style features only. */
        @Opt(description = "Maximum token context for which to extract features (TurboParser style only)")
        public int maxTokenContext = 2;
        @Opt(description = "Whether to extract features for labeled parsing (TurboParser style only)")
        public boolean isLabeledParsing = false;
        @Opt(description = "Whether to use lemma features (TurboParser style only)")
        public boolean useLemmaFeats = true;
        @Opt(description = "Whether to use mophology features (TurboParser style only)")
        public boolean useMorphologicalFeats = true;
        @Opt(description = "Whether to use extra features (TurboParser style only)")
        public boolean useNonTurboFeats = false;
        @Opt(description = "Whether to use word-pair features for 2nd-order features (TurboParser style only)")
        public boolean usePairFor2ndOrder = true;
        @Opt(description = "Whether to use word-pair features for arbitrary sibling features (TurboParser style only)")
        public boolean usePairFor2ndOrderArbiSibl = true; // TODO: Switch this off once we add consecutive.
        @Opt(description = "Whether to use word-pair features for the grandparent and head (TurboParser style only)")
        public boolean useUpperGrandDepFeats = false;
        @Opt(description = "Whether to use word-pair features for the head and modifier when non-projective in grandparent feats (TurboParser style only)")
        public boolean useNonprojGrandDepFeats = false;
        @Opt(description = "Whether to use trilexical features (TurboParser style only)")
        public boolean useTrilexicalFeats = false;
    }
    
    private static final Logger log = LoggerFactory.getLogger(BitshiftDepParseFeatureExtractor.class);     
    private static final FeatureVector emptyFv;
    static {
        emptyFv = new FeatureVector();
    }

    private BitshiftDepParseFeatureExtractorPrm prm;
    private IntAnnoSentence isent;
    private FeatureNames alphabet;
    
    public BitshiftDepParseFeatureExtractor(BitshiftDepParseFeatureExtractorPrm prm, AnnoSentence sent, CorpusStatistics cs, FeatureNames alphabet) {
        this.prm = prm;
        this.isent = new IntAnnoSentence(sent, cs.store);
        this.alphabet = alphabet;
    }

    @Override
    public void init(UFgExample ex) {
        if (ex.getObsConfig().size() > 0) {
            throw new IllegalStateException("This feature extractor does not support observed variables.");
        }
    }
    
    @Override
    public FeatureVector calcFeatureVector(FeExpFamFactor factor, int configId) {
        // Expand the alphabet to include every feature up to the hash mod.
        // TODO: Remove this, it's a huge waste of memory.
        int i=0;
        while (alphabet.size() < prm.featureHashMod) {
            alphabet.lookupIndex(i++);
        }
        
        FeTypedFactor f = (FeTypedFactor) factor;
        Enum<?> ft = f.getFactorType();
        VarSet vars = f.getVars();
        
        int[] vc = vars.getVarConfigAsArray(configId);
        if (ArrayUtils.contains(vc, LinkVar.FALSE)) {
            return emptyFv;
        }
        
        //LongArrayList feats = new LongArrayList();
        FeatureVector feats = new FeatureVector();

        // Get the features for an edge that is "on".
        if (ft == DepParseFactorTemplate.LINK_UNARY) {
            // Look at the variables to determine the parent and child.
            LinkVar var = (LinkVar) vars.get(0);
            int p = var.getParent();
            int c = var.getChild();
            BitshiftDepParseFeatures.addArcFeats(isent, p, c, prm, feats);
        } else if (ft == DepParseFactorTemplate.LINK_SIBLING) {
            SibFeTypedFactor f2 = (SibFeTypedFactor)f;
            BitshiftDepParseFeatures.addArbitrarySiblingFeats(isent, f2.p, f2.c, f2.s, feats, prm);
        } else if (ft == DepParseFactorTemplate.LINK_GRANDPARENT) {
            GraFeTypedFactor f2 = (GraFeTypedFactor)f;
            BitshiftDepParseFeatures.addGrandparentFeats(isent, f2.g, f2.p, f2.c, feats, prm);
        } else {
            throw new RuntimeException("Unsupported template: " + ft);
        }
        
        return feats;
    }
        
}
