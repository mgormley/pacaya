package edu.jhu.srl;

import java.util.HashSet;
import java.util.Set;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.featurize.SentFeatureExtractor;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FeatureExtractor;
import edu.jhu.gm.FeatureVector;
import edu.jhu.gm.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.Var;
import edu.jhu.gm.VarConfig;
import edu.jhu.gm.VarSet;
import edu.jhu.srl.CorpusStatistics.Normalize;
import edu.jhu.srl.SrlFactorGraph.RoleVar;
import edu.jhu.srl.SrlFactorGraph.SrlFactor;
import edu.jhu.srl.SrlFactorGraph.SrlFactorTemplate;
import edu.jhu.util.Alphabet;

/**
 * Feature extractor for SRL. All the "real" feature extraction is done in
 * SentFeatureExtraction which considers only the observations.
 * 
 * @author mgormley
 */
public class SrlFeatureExtractor implements FeatureExtractor {

    // Cache of observation features for each single positions in the sentence.
    private Set<String>[] obsFeatsSolo;
    // Cache of observation features for each pair of positions in the sentence.
    private Set<String>[][] obsFeatsPair;
    
    // -- Inputs --
    private SrlFactorGraph sfg;
    private final Alphabet<Feature> alphabet;
    private SentFeatureExtractor sentFeatExt;
    
    public SrlFeatureExtractor(SrlFactorGraph sfg, Alphabet<Feature> alphabet, SentFeatureExtractor sentFeatExt) {
        super();
        this.sfg = sfg;
        this.alphabet = alphabet;
        this.sentFeatExt = sentFeatExt;
        obsFeatsSolo = new Set[sentFeatExt.getSentSize()];
        obsFeatsPair = new Set[sentFeatExt.getSentSize()][sentFeatExt.getSentSize()];
    }
    
    @Override
    public FeatureVector calcFeatureVector(int factorId, VarConfig varConfig) {
        SrlFactor f = (SrlFactor) sfg.getFactor(factorId);
        SrlFactorTemplate ft = f.getFactorType();
        VarSet vars = varConfig.getVars();
        
        // Get the observation features.
        Set<String> obsFeats;
        if (ft == SrlFactorTemplate.LINK_ROLE || ft == SrlFactorTemplate.LINK_UNARY || ft == SrlFactorTemplate.ROLE_UNARY) {
            // Look at the variables to determine the parent and child.
            Var var = vars.iterator().next();
            int parent;
            int child;
            if (var instanceof LinkVar) {
                parent = ((LinkVar)var).getParent();
                child = ((LinkVar)var).getChild();
            } else {
                parent = ((RoleVar)var).getParent();
                child = ((RoleVar)var).getChild();
            }
            
            // IMPORTANT NOTE: We include the case where the parent is the Wall node (position -1).
            if (parent == -1) {
                obsFeats = fastGetObsFeats(child);
            } else {
                // Get features on the observations for a pair of words.
                obsFeats = fastGetObsFeats(parent, child);
            }
            // TODO: is it okay if this include the observed variables?                
        } else {
            throw new RuntimeException("Unsupported factor type: " + ft);
        }
        
        // Conjoin each observation feature with the string
        // representation of the given assignment to the given
        // variables.
        FeatureVector fv = new FeatureVector();
        String vcStr = varConfig.getStringName();
        for (String obsFeat : obsFeats) {
            String fname = vcStr + "_" + obsFeat;
            fv.add(alphabet.lookupIndex(new Feature(fname)), 1.0);
        }
        
        return fv;
    }

    private Set<String> fastGetObsFeats(int child) {
        if (obsFeatsSolo[child] == null) {
            // Lazily construct the observation features.
            obsFeatsSolo[child] = sentFeatExt.createFeatureSet(child);
        }
        return obsFeatsSolo[child];
    }

    private Set<String> fastGetObsFeats(int parent, int child) {
        if (obsFeatsPair[parent][child] == null) {
            // Lazily construct the observation features.
            obsFeatsPair[parent][child] = sentFeatExt.createFeatureSet(parent, child);
        }
        return obsFeatsPair[parent][child];
    }

}