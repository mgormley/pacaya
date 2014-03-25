package edu.jhu.gm.maxent;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.gm.feat.Feature;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.util.Alphabet;

/**
 * Factory for log-linear model instances, specifying binary features of the
 * observed variable, x, and a label, y.
 * 
 * @author mgormley
 */
public class LogLinearXYData {

    /**
     * A description of a weighted example for a log-linear model.
     * 
     * @author mgormley
     * @author mmitchell
     */
    public static class LogLinearExample {
        private double weight;
        private int x;
        private int y;
        private FeatureVector[] fvs;

        public LogLinearExample(double weight, int x, int y, FeatureVector[] fvs) {
            this.weight = weight;
            this.x = x;
            this.y = y;
            this.fvs = fvs;
        }

        public double getWeight() {
            return weight;
        }
        
        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public FeatureVector getFeatures(int y) {
            if (y >= fvs.length) {
                throw new IllegalStateException("Invalid value for y. Must be less than " + fvs.length);
            }
            return fvs[y];
        }
        
    }
    
    private int numYs;
    private final Alphabet<String> featAlphabet;
    private List<LogLinearExample> exList;

    public LogLinearXYData(int numYs) {
        this(numYs, new Alphabet<String>());
    }
    
    public LogLinearXYData(int numYs, Alphabet<String> featAlphabet) {
        this.numYs = numYs;
        this.featAlphabet = featAlphabet;
        this.exList = new ArrayList<LogLinearExample>();
    }
    
    /**
     * Adds a new log-linear model instance.
     * 
     * @param weight The weight of this example.
     * @param x The observation, x.
     * @param y The prediction, y.
     * @param fvs The binary features on the observations, x, for all possible labels, y'. Indexed by y'.
     */
    public void addEx(double weight, int x, int y, FeatureVector[] fvs) {
        if (y >= numYs) {
            throw new IllegalArgumentException("Invalid y: " + y);
        } else if (fvs.length != numYs) {
            throw new IllegalArgumentException("Features must be given for all labels y");
        }        
        LogLinearExample ex = new LogLinearExample(weight, x, y, fvs);
        exList.add(ex);
    }

    public Alphabet<String> getAlphabet() {
        return featAlphabet;
    }

    public List<LogLinearExample> getData() {
        return exList;
    }
    
    public void clear() {
        exList.clear();
    }
        
}