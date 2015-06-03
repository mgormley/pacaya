package edu.jhu.pacaya.gm.train;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import edu.jhu.pacaya.gm.feat.FeatureVector;
import edu.jhu.pacaya.gm.feat.SlowFeatureExtractor;
import edu.jhu.pacaya.gm.model.FeExpFamFactor;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.util.FeatureNames;

/**
 * Constructs features for each factor graph configuration by creating a
 * sorted list of all the variable states and concatenating them together.
 * 
 * For testing only.
 * 
 * @author mgormley
 */
public class SimpleVCFeatureExtractor extends SlowFeatureExtractor {

    protected FeatureNames alphabet;

    public SimpleVCFeatureExtractor(FeatureNames alphabet) {
        super();
        this.alphabet = alphabet;          
    }
    
    // Just concatenates all the state names together (in-order).
    @Override
    public FeatureVector calcFeatureVector(FeExpFamFactor factor, VarConfig varConfig) {
        FeatureVector fv = new FeatureVector();

        if (varConfig.size() > 0) {
            String[] strs = new String[varConfig.getVars().size()];
            int i=0;
            for (Var v : varConfig.getVars()) {
                strs[i] = varConfig.getStateName(v);
                i++;
            }
            Arrays.sort(strs);
            int featIdx = alphabet.lookupIndex(StringUtils.join(strs, ":"));
            fv.set(featIdx, 1.0);
        }
        
        int featIdx = alphabet.lookupIndex("BIAS_FEATURE");
        alphabet.setIsBias(featIdx);
        fv.set(featIdx, 1.0);
        
        return fv;
    }
}