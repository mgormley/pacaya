package edu.jhu.gm.maxent;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.FgExampleMemoryStore;
import edu.jhu.gm.feat.Feature;
import edu.jhu.gm.feat.FeatureCache;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.model.ExpFamFactor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FeExpFamFactor;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.util.Alphabet;

/**
 * A factor for FgExamples constructed from LogLinearExDesc objects.
 * @author mgormley
 */
public class LogLinearEDs {

    /**
     * A description of a collection of identical log-linear model examples.
     * 
     * @author mgormley
     * @author mmitchell
     */
    public static class LogLinearExDesc {
        private int count;
        private FeatureVector features;
        public LogLinearExDesc(int count, FeatureVector features) {
            this.count = count;
            this.features = features;
        }
        public int getCount() {
            return count;
        }
        public FeatureVector getFeatures() {
            return features;
        }
    }
    
    private static final Object TEMPLATE_KEY = "loglin";
    private final Alphabet<Feature> alphabet = new Alphabet<Feature>();
    private ArrayList<LogLinearExDesc> descList = new ArrayList<LogLinearExDesc>();

    public void addEx(int count, String... featNames) {
        FeatureVector features = new FeatureVector();
        for (String featName : featNames) {
            features.put(alphabet.lookupIndex(new Feature(featName)), 1.0);
        }
        LogLinearExDesc ex = new LogLinearExDesc(count, features);
        descList.add(ex);
    }
    
    public List<String> getStateNames() {
        List<String> names = new ArrayList<String>();
        for (LogLinearExDesc desc : descList) {
            StringBuilder sb = new StringBuilder();
            for (IntDoubleEntry entry : desc.getFeatures()) {
                sb.append(entry.index());
                sb.append(":");
            }
            names.add(sb.toString());
        }
        return names;
    }
    
    public FgExampleList getData() {        
        FgExampleMemoryStore data = new FgExampleMemoryStore();
        int state=0;
        for (final LogLinearExDesc desc : descList) {
            for (int i=0; i<desc.getCount(); i++) {
                final Var v0 = new Var(VarType.PREDICTED, descList.size(), "v0", getStateNames());
                final VarConfig trainConfig = new VarConfig();
                trainConfig.put(v0, state);
                
                FactorGraph fg = new FactorGraph();
                final VarSet varSet = new VarSet(v0);
                // This FeatureExtractor was restored from commit [31ea8a7] which followed a commit
                // with description "Updating code to cache only observed features 
                // and implicitly construct features with the predicted variables."
                FeatureExtractor fe = new FeatureExtractor() {
                    @Override
                    public FeatureVector calcFeatureVector(FeExpFamFactor factor, int configId) {
                        VarConfig varConfig = varSet.getVarConfig(configId);
                        return descList.get(varConfig.getState(v0)).getFeatures();
                    }
                    @Override
                    public void init(FgExample ex) {
                    }
                };
                //TODO: fe = new FeatureCache(fe);
                ExpFamFactor f0 = new FeExpFamFactor(varSet, TEMPLATE_KEY, fe);
                fg.addFactor(f0);
                data.add(new FgExample(fg, trainConfig));
            }
            state++;
        }
        return data;
    }

    public Alphabet<Feature> getAlphabet() {
        return alphabet;
    }
        
}