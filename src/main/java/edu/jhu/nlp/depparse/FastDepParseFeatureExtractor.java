package edu.jhu.nlp.depparse;

import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.model.FeExpFamFactor;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor.LinkVar;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.FeTypedFactor;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.IntAnnoSentence;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder.DepParseFactorTemplate;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder.O2FeTypedFactor;
import edu.jhu.nlp.features.LocalObservations;
import edu.jhu.nlp.features.TemplateLanguage.FeatTemplate;
import edu.jhu.nlp.features.TemplateSets;
import edu.jhu.prim.list.LongArrayList;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.FeatureNames;
import edu.jhu.util.Prm;
import edu.jhu.util.hash.MurmurHash;

public class FastDepParseFeatureExtractor implements FeatureExtractor {

    private static final Logger log = Logger.getLogger(FastDepParseFeatureExtractor.class);     
    private static final FeatureVector biasFvEdgeOff;
    static {
        biasFvEdgeOff = new FeatureVector();
        biasFvEdgeOff.add(MurmurHash.hash32("BIAS_FEATURE_FOR_OFF_EDGE"), 1.0);
    }

    private int featureHashMod;
    private IntAnnoSentence isent;
    
    public FastDepParseFeatureExtractor(AnnoSentence sent, CorpusStatistics cs, int featureHashMod) {
        this.isent = new IntAnnoSentence(sent, cs.store);
        this.featureHashMod = featureHashMod;
    }

    @Override
    public void init(UFgExample ex) {
        if (ex.getObsConfig().size() > 0) {
            throw new IllegalStateException("This feature extractor does not support observed variables.");
        }
    }
    
    @Override
    public FeatureVector calcFeatureVector(FeExpFamFactor factor, int configId) {
        FeTypedFactor f = (FeTypedFactor) factor;
        Enum<?> ft = f.getFactorType();
        VarSet vars = f.getVars();
        
        int[] vc = vars.getVarConfigAsArray(configId);
        if (ArrayUtils.contains(vc, LinkVar.FALSE)) {
            return biasFvEdgeOff;
        }
        
        LongArrayList feats = new LongArrayList();
        // Get the features for an edge that is "on".
        if (ft == DepParseFactorTemplate.LINK_UNARY) {
            // Look at the variables to determine the parent and child.
            LinkVar var = (LinkVar) vars.get(0);
            int p = var.getParent();
            int c = var.getChild();
            FastDepParseFe.addArcFactoredMSTFeats(isent, p, c, feats, false);            
        } else if (ft == DepParseFactorTemplate.LINK_SIBLING) {
            O2FeTypedFactor f2 = (O2FeTypedFactor)f;
            FastDepParseFe.add2ndOrderSiblingFeats(isent, f2.i, f2.j, f2.k, feats);
        } else if (ft == DepParseFactorTemplate.LINK_GRANDPARENT) {
            O2FeTypedFactor f2 = (O2FeTypedFactor)f;
            FastDepParseFe.add2ndOrderGrandparentFeats(isent, f2.k, f2.i, f2.j, feats);
        } else {
            throw new RuntimeException("Unsupported template: " + ft);
        }
        
        // TODO: This should be stored as an IntArrayList and hashing should move inside the feature extractors.
        FeatureVector fv = new FeatureVector();
        long[] lfeats = feats.getInternalElements();
        for (int k=0; k<feats.size(); k++) {
            //int hash = (int) ((lfeats[k] ^ (lfeats[k] >>> 32)) & 0xffffffffl);
            int hash = MurmurHash.hash32(lfeats[k]);
            hash = FastMath.mod(hash, featureHashMod);
            fv.add(hash, 1.0);
        }
        return fv;
    }
        
}
