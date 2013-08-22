package edu.jhu.featurize;

import org.apache.log4j.Logger;

import edu.jhu.featurize.SentFeatureExtractor;
import edu.jhu.gm.BinaryStrFVBuilder;
import edu.jhu.gm.FactorGraph;
import edu.jhu.gm.ObsFeatureExtractor;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FeatureTemplateList;
import edu.jhu.gm.FeatureVector;
import edu.jhu.gm.FeatureVectorBuilder;
import edu.jhu.gm.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.Var;
import edu.jhu.gm.VarConfig;
import edu.jhu.gm.VarSet;
import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.srl.SrlFactorGraph.RoleVar;
import edu.jhu.srl.SrlFactorGraph.SrlFactor;
import edu.jhu.srl.SrlFactorGraph.SrlFactorTemplate;
import edu.jhu.util.Alphabet;

/**
 * Cache for SentFeatureExtractor.
 * 
 * @author mgormley
 */
public class SentFeatureExtractorCache {

    private static final Logger log = Logger.getLogger(SentFeatureExtractorCache.class); 

    // Cache of observation features for each single positions in the sentence.
    private BinaryStrFVBuilder[] obsFeatsSolo;
    // Cache of observation features for each pair of positions in the sentence.
    private BinaryStrFVBuilder[][] obsFeatsPair;

    private SentFeatureExtractor sentFeatExt;
    
    // A single alphabet for all the observed features.
    private Alphabet<String> obsAlphabet;
    
    public SentFeatureExtractorCache(SentFeatureExtractor sentFeatExt, Alphabet<String> obsAlphabet) {
        this.sentFeatExt = sentFeatExt;
        this.obsAlphabet = obsAlphabet;
        obsFeatsSolo = new BinaryStrFVBuilder[sentFeatExt.getSentSize()];
        obsFeatsPair = new BinaryStrFVBuilder[sentFeatExt.getSentSize()][sentFeatExt.getSentSize()];
    }
    
    public BinaryStrFVBuilder fastGetObsFeats(int child) {
        if (obsFeatsSolo[child] == null) {
            // Lazily construct the observation features.
            obsFeatsSolo[child] = sentFeatExt.createFeatureSet(child, obsAlphabet);
        }
        return obsFeatsSolo[child];
    }

    public BinaryStrFVBuilder fastGetObsFeats(int parent, int child) {
        if (obsFeatsPair[parent][child] == null) {
            // Lazily construct the observation features.
            obsFeatsPair[parent][child] = sentFeatExt.createFeatureSet(parent, child, obsAlphabet);
        }
        return obsFeatsPair[parent][child];
    }
    
}