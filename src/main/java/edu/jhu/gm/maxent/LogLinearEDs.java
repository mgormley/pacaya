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
    
    private final Alphabet<Feature> alphabet = new Alphabet<Feature>();
    private ArrayList<LogLinearExDesc> descList = new ArrayList<LogLinearExDesc>();

    public void addEx(int count, String... featNames) {
        FeatureVector features = new FeatureVector();
        for (String featName : featNames) {
            features.add(alphabet.lookupIndex(new Feature(featName)), 1.0);
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
    
    public LogLinearXYData getData() {
        int numYs = descList.size();
        LogLinearXYData data = new LogLinearXYData(numYs);
        FeatureVector[] fvs = new FeatureVector[numYs];
        for (int y=0; y<numYs; y++) {
            fvs[y] = descList.get(y).getFeatures();            
        }
        for (int y=0; y<numYs; y++) {
            LogLinearExDesc desc = descList.get(y);
            data.addEx(desc.getCount(), 0, y, fvs);
        }
        return data;
    }

    public Alphabet<Feature> getAlphabet() {
        return alphabet;
    }
        
}