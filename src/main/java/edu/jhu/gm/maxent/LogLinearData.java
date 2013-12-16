package edu.jhu.gm.maxent;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.FgExampleMemoryStore;
import edu.jhu.gm.feat.Feature;
import edu.jhu.gm.feat.FactorTemplate;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.ExpFamFactor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.util.Alphabet;

/**
 * Factory for log-linear model instances, specifying binary features of the
 * observed variable, x, and a label, y.
 * 
 * @author mgormley
 */
public class LogLinearData {

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
    
    private static final Object TEMPLATE_KEY = "loglin";
    private final Alphabet<Feature> alphabet = new Alphabet<Feature>();
    private ArrayList<LogLinearExample> exList = new ArrayList<LogLinearExample>();

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
            features.put(alphabet.lookupIndex(new Feature(featName)), 1.0);
        }
        LogLinearExample ex = new LogLinearExample(weight, label, features);
        exList.add(ex);
    }
    
    public FgExampleList getData() {
        FactorTemplateList fts = new FactorTemplateList();
        List<String> stateNames = getStateNames(exList);
        {
            Var v0 = new Var(VarType.PREDICTED, exList.size(), "v0", stateNames);
            fts.add(new FactorTemplate(new VarSet(v0), alphabet, TEMPLATE_KEY));
        }
        
        FgExampleMemoryStore data = new FgExampleMemoryStore(fts);
        for (final LogLinearExample desc : exList) {
            for (int i=0; i<desc.getWeight(); i++) {
                Var v0 = new Var(VarType.PREDICTED, exList.size(), "v0", stateNames);
                final VarConfig trainConfig = new VarConfig();
                trainConfig.put(v0, desc.getLabel());
                
                FactorGraph fg = new FactorGraph();
                VarSet vars = new VarSet(v0);
                ExpFamFactor f0 = new ExpFamFactor(vars, TEMPLATE_KEY);
                fg.addFactor(f0);
                ObsFeatureExtractor featExtractor = new ObsFeatureExtractor() {
                    @Override
                    public FeatureVector calcObsFeatureVector(int factorId) {
                        return desc.getObsFeatures();
                    }
                    public void init(FactorGraph fg, FactorGraph fgLat, FactorGraph fgLatPred,
                            VarConfig goldConfig, FactorTemplateList fts) {             
                        // Do nothing.               
                    }
                    public void clear() {
                        // Do nothing.
                    }
                };
                data.add(new FgExample(fg, trainConfig, featExtractor, fts));
            }
        }
        return data;
    }
    
    private static List<String> getStateNames(List<LogLinearExample> exList) {
        List<String> names = new ArrayList<String>();
        for (LogLinearExample desc : exList) {
            names.add(desc.getLabel());
        }
        return names;
    }

    public Alphabet<Feature> getAlphabet() {
        return alphabet;
    }
        
}