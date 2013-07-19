package edu.jhu.featurize;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.gm.FactorGraph;
import edu.jhu.gm.FeatureExtractor;
import edu.jhu.gm.FeatureVector;
import edu.jhu.gm.FgExample;
import edu.jhu.gm.VarConfig;

public class SrlFeaturizer {
    private int count;
    private FeatureVector features;
    private FactorGraph fg;
    //public SrlFeaturizer(int count, FeatureVector features) {
    //    this.count = count;
    //    this.features = features;
    //}
    public SrlFeaturizer(CoNLL09Sentence sent) {
        
    }
    
    public FgExample getFGExample(CoNLL09Sentence sent) {
        new FeatureExtractor featExtractor;
        VarConfig goldConfig;
        FgExample fge = new FgExample(fg, goldConfig, featExtractor);
        
    }

}
