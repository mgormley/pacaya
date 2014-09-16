package edu.jhu.gm.maxent;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.gm.feat.Feature;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.util.FeatureNames;

/**
 * Factory for log-linear model instances, specifying binary features of the
 * observed variable, x, and a label, y.
 * 
 * @author mgormley
 */
public class LogLinearObsFeatsData {

    /**
     * A description of a weighted example for a log-linear model.
     * 
     * @author mgormley
     * @author mmitchell
     */
    public static class LogLinearExample {
        private double weight;
        private String label;
        private FeatureVector obsFeatures;

        public LogLinearExample(double weight, String label, FeatureVector obsFeatures) {
            this.weight = weight;
            this.label = label;
            this.obsFeatures = obsFeatures;
        }

        public String getLabel() {
            return label;
        }

        public double getWeight() {
            return weight;
        }

        public FeatureVector getObsFeatures() {
            return obsFeatures;
        }
    }
    
    private final FeatureNames alphabet = new FeatureNames();
    private List<LogLinearExample> exList = new ArrayList<LogLinearExample>();

    public LogLinearObsFeatsData() {
    }
    
    /**
     * Adds a new log-linear model instance.
     * 
     * @param weight The weight of this example.
     * @param label The label, y.
     * @param featNames The binary features on the observations, x.
     */
    public void addEx(double weight, String label, List<? extends Object> featNames) {
        FeatureVector features = new FeatureVector();
        for (Object featName : featNames) {
            features.add(alphabet.lookupIndex(new Feature(featName)), 1.0);
        }
        LogLinearExample ex = new LogLinearExample(weight, label, features);
        exList.add(ex);
    }

    public FeatureNames getAlphabet() {
        return alphabet;
    }

    public List<LogLinearExample> getData() {
        return exList;
    }
    
    public void clear() {
        exList.clear();
    }
        
}