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
    private static final FeatureVector emptyFv;
    static {
        emptyFv = new FeatureVector();
    }

    public static int featureHashMod = -1;
    private IntAnnoSentence isent;
    
    public FastDepParseFeatureExtractor(AnnoSentence sent, CorpusStatistics cs, int featureHashMod, FeatureNames alphabet) {
        this.isent = new IntAnnoSentence(sent, cs.store);
        this.featureHashMod = featureHashMod;
        // TODO: Remove this, it's a huge waste of memory.
        int i=0;
        while (alphabet.size() < featureHashMod) {
            alphabet.lookupIndex(i++);
        }
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
        
        return feats;
    }
        
}
