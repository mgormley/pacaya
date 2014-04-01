package edu.jhu.gm.maxent;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.prim.util.Lambda.FnIntDoubleToDouble;
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
    
    private final Alphabet<String> alphabet = new Alphabet<String>();
    private ArrayList<LogLinearExDesc> descList = new ArrayList<LogLinearExDesc>();

    public void addEx(int count, String... featNames) {
        FeatureVector features = new FeatureVector();
        for (String featName : featNames) {
            features.add(alphabet.lookupIndex(featName), 1.0);
        }
        LogLinearExDesc ex = new LogLinearExDesc(count, features);
        descList.add(ex);
    }
    
    public List<String> getStateNames() {
        List<String> names = new ArrayList<String>();
        for (LogLinearExDesc desc : descList) {
            final StringBuilder sb = new StringBuilder();
            desc.getFeatures().apply(new FnIntDoubleToDouble() {
                public double call(int idx, double val) {
                    sb.append(idx);
                    sb.append(":");
                    return val;
                }
            });
            names.add(sb.toString());
        }
        return names;
    }
    
    public LogLinearXYData getData() {
        int numYs = descList.size();
        LogLinearXYData data = new LogLinearXYData(numYs, alphabet);
        FeatureVector[] fvs = new FeatureVector[numYs];
        for (int y=0; y<numYs; y++) {
            fvs[y] = descList.get(y).getFeatures();            
        }
        for (int y=0; y<numYs; y++) {
            LogLinearExDesc desc = descList.get(y);
            data.addEx(desc.getCount(), "x=0", "y="+y, fvs);
        }
        return data;
    }

    public Alphabet<String> getAlphabet() {
        return alphabet;
    }
        
}