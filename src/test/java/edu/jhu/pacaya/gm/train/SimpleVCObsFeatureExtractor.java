package edu.jhu.pacaya.gm.train;

import edu.jhu.pacaya.gm.feat.FactorTemplateList;
import edu.jhu.pacaya.gm.feat.FeatureVector;
import edu.jhu.pacaya.gm.feat.ObsFeExpFamFactor;
import edu.jhu.pacaya.gm.feat.SlowObsFeatureExtractor;
import edu.jhu.pacaya.util.FeatureNames;

/**
 * Constructs features for each factor graph configuration by creating a
 * sorted list of all the variable states and concatenating them together.
 * 
 * For testing only.
 * 
 * @author mgormley
 */
public class SimpleVCObsFeatureExtractor extends SlowObsFeatureExtractor {

    protected FactorTemplateList fts;

    public SimpleVCObsFeatureExtractor(FactorTemplateList fts) {
        super();
        this.fts = fts;
    }
    
    // Just concatenates all the state names together (in-order).
    @Override
    public FeatureVector calcObsFeatureVector(ObsFeExpFamFactor factor) {
        FeatureVector fv = new FeatureVector();
        FeatureNames alphabet = fts.getTemplate(factor).getAlphabet();            
        int featIdx = alphabet.lookupIndex("BIAS_FEATURE");
        assert featIdx != -1;
        alphabet.setIsBias(featIdx);
        fv.set(featIdx, 1.0);
        
        return fv;
    }
}