package edu.jhu.gm.feat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;

import org.apache.log4j.Logger;

import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.ExpFamFactor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.IndexForVc;
import edu.jhu.gm.model.ObsFeatureCarrier;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.util.IntIter;
import edu.jhu.prim.arrays.BoolArrays;
import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.prim.util.Lambda.FnIntDoubleToDouble;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Prm;

/**
 * Wrapper of ObsFeatureExtractor which returns feature vectors which conjoin
 * the observation function with an indicator of the predicted variables.
 * 
 * @author mgormley
 */
public class ObsFeatureConjoiner implements Serializable {

    /**
     * An exponential family factor which takes an ObsFeatureExtractor at
     * construction time and uses it to extract "observation features" which are
     * subsequently conjoined with an indicator on the predicted variables to
     * form the final features.
     * 
     * This class requires access to the internally stored indexing of
     * ObsFeatureConjoiner.
     * 
     * @author mgormley
     */
    public static abstract class ObsCjExpFamFactor extends ExpFamFactor implements ObsFeatureCarrier {
    
        private static final long serialVersionUID = 1L;
        private ObsFeatureConjoiner ofc;
        
        public ObsCjExpFamFactor(VarSet vars, Object templateKey, ObsFeatureConjoiner ofc) {
            super(vars, templateKey);
            this.ofc = ofc;
            // TODO: setTemplateId(ofc.getTemplates().getTemplateId(this));
        }
        
        @Override
        public abstract FeatureVector getObsFeatures();
        
        @Override
        public FeatureVector getFeatures(final int config) {
            if (!ofc.isInitialized()) {
                throw new IllegalStateException("ObsFeatureConjoiner not initialized");
            }
            Factor factor = this;
            final int ft = factor.getTemplateId();
            FeatureVector obsFv = ((ObsFeatureCarrier) factor).getObsFeatures();
            final FeatureVector fv = new FeatureVector(obsFv.size());
            obsFv.apply(new FnIntDoubleToDouble() {            
                @Override
                public double call(int feat, double val) {
                    if (ofc.included[ft][config][feat]) {
                        fv.add(ofc.indices[ft][config][feat], val);
                    }
                    return val;
                }
            });
            return fv;
        }

        @Override
        public ExpFamFactor getClamped(VarConfig clmpVarConfig) {
            DenseFactor df = super.getClamped(clmpVarConfig);
            return new ClampedObsCjExpFamFactor(df, templateKey, clmpVarConfig, this);
        }
        
        static class ClampedObsCjExpFamFactor extends ClampedExpFamFactor implements ObsFeatureCarrier {
            
            private static final long serialVersionUID = 1L;
            // The unclamped factor from which this one was derived
            private ObsCjExpFamFactor unclmpFactor;
            
            // Used only to create clamped factors.
            public ClampedObsCjExpFamFactor(DenseFactor other, Object templateKey, VarConfig clmpVarConfig, ObsCjExpFamFactor unclmpFactor) {
                super(other, templateKey, clmpVarConfig, unclmpFactor);
                this.unclmpFactor = unclmpFactor;                
            }

            @Override
            public FeatureVector getObsFeatures() {
                // Pass through to the unclamped factor.
                return unclmpFactor.getObsFeatures();
            }
            
        }
        
    }
    
    public static class ObsFeExpFamFactor extends ObsCjExpFamFactor implements ObsFeatureCarrier {
        
        private static final long serialVersionUID = 1L;
        private ObsFeatureExtractor obsFe;
        
        public ObsFeExpFamFactor(VarSet vars, Object templateKey, ObsFeatureConjoiner ofc, ObsFeatureExtractor obsFe) {
            super(vars, templateKey, ofc);
            this.obsFe = obsFe;
        }
        
        @Override
        public FeatureVector getObsFeatures() {
            return this.obsFe.calcObsFeatureVector(this);
        }
        
    }

    public static class ObsFeatureConjoinerPrm extends Prm {
        private static final long serialVersionUID = 1L;
        /** Whether to include unsupported features. */
        public boolean includeUnsupportedFeatures = false;
        /**
         * Minimum number of times (inclusive) a feature must occur in training
         * to be included in the model. Ignored if non-positive. (Using this
         * cutoff implies that unsupported features will not be included.)
         */
        public int featCountCutoff = -1;
    }
    
    private static final Logger log = Logger.getLogger(ObsFeatureConjoiner.class);
    private static final long serialVersionUID = 1L;
    
    /**
     * The model parameters indices. Indexed by feature template index, variable
     * assignment config index, and observation function feature index.
     */
    private int[][][] indices;
    /**
     * Whether or not the correspondingly indexed model parameter is included in
     * this model.
     */
    private boolean[][][] included;
    /** The number of feature templates. */
    private int numTemplates;
    /** The number of parameters in the model. */
    private int numParams;
    /** The feature templates. */
    private FactorTemplateList templates;
    /** Whether this object is initialized. */
    private boolean initialized;
    
    private ObsFeatureConjoinerPrm prm;
    
    public ObsFeatureConjoiner(ObsFeatureConjoinerPrm prm) {
        this.prm = prm;
        initialized = false;
    }
        
    public void init(FactorTemplateList templates) {
        init(null, templates);
    }
        
    public void init(FgExampleList data, FactorTemplateList templates) {
        this.templates = templates;
        numTemplates = templates.size();
        
        this.included = new boolean[numTemplates][][];
        for (int t=0; t<numTemplates; t++) {
            FactorTemplate template = templates.get(t);
            int numConfigs = template.getNumConfigs();
            int numFeats = template.getAlphabet().size();
            included[t] = new boolean[numConfigs][numFeats];
        }
        
        // Apply a feature count cutoff.
        if (!prm.includeUnsupportedFeatures) {
            prm.featCountCutoff = Math.max(prm.featCountCutoff, 1);
        }
        BoolArrays.fill(included, true);
        if (prm.featCountCutoff >= 1) {
            log.info("Applying feature count cutoff: " + prm.featCountCutoff);
            int[][][] counts = countFeatures(data, templates);
            excludeByFeatCount(counts);
        }
      
        // Always include the bias features.
        for (int t=0; t<included.length; t++) {
            FactorTemplate template = templates.get(t);
            Alphabet<Feature> alphabet = template.getAlphabet();            
            for (int k = 0; k < alphabet.size(); k++) {
                if (alphabet.lookupObject(k).isBiasFeature()) {
                    for (int c = 0; c < included[t].length; c++) {
                        included[t][c][k] = true;
                    }
                }
            }
        }

        // Set the indices to track only the included parameters.
        // All other entries are set to -1.
        // Also: Count the number of parameters, accounting for excluded params.
        this.indices = new int[numTemplates][][];
        for (int t=0; t<indices.length; t++) {
            FactorTemplate template = templates.get(t);
            int numConfigs = template.getNumConfigs();
            int numFeats = template.getAlphabet().size();
            indices[t] = new int[numConfigs][numFeats];
            for (int c = 0; c < indices[t].length; c++) {
                for (int k = 0; k < indices[t][c].length; k++) {
                    indices[t][c][k] = included[t][c][k] ? numParams++ : -1;
                }
            }
        }
        
        initialized = true;
    }
    
    /**
     * Counts the number of times each feature appears in the gold training data.
     */
    private int[][][] countFeatures(FgExampleList data, FactorTemplateList templates) {
        int[][][] counts = new int[numTemplates][][];
        for (int t=0; t<numTemplates; t++) {
            FactorTemplate template = templates.get(t);
            int numConfigs = template.getNumConfigs();
            int numFeats = template.getAlphabet().size();
            counts[t] = new int[numConfigs][numFeats];
        }        
        for (int i=0; i<data.size(); i++) {
            FgExample ex = data.get(i);
            for (int a=0; a<ex.getOriginalFactorGraph().getNumFactors(); a++) {
                Factor f = ex.getFgLat().getFactor(a);
                if (f instanceof ObsFeatureCarrier) {
                    int t = templates.getTemplateId(f);
                    if (t != -1) {
                        FeatureVector fv = ((ObsFeatureCarrier) f).getObsFeatures();                            
                        if (f.getVars().size() == 0) {
                            int predConfig = ex.getGoldConfigIdxPred(a);
                            for (IntDoubleEntry entry : fv) {
                                counts[t][predConfig][entry.index()]++;
                            }
                        } else {
                            // We must clamp the predicted variables and loop over the latent ones.
                            VarConfig predVc = ex.getGoldConfigPred(a);
                            IntIter iter = IndexForVc.getConfigIter(ex.getFgLatPred().getFactor(a).getVars(), predVc);
                            
                            int numConfigs = f.getVars().calcNumConfigs();
                            for (int c=0; c<numConfigs; c++) {            
                                // The configuration of all the latent/predicted variables,
                                // where the predicted variables have been clamped.
                                int config = iter.next();
                                for (IntDoubleEntry entry : fv) {
                                    counts[t][config][entry.index()]++;
                                }
                            }
                        }
                    }
                }
            }
        }
        return counts;
    }

    /**
     * Exclude those features which do not pass the feature count cutoff
     * threshold (bias features will be kept separately).
     */
    private void excludeByFeatCount(int[][][] counts) {
        for (int t=0; t<included.length; t++) {
            for (int c = 0; c < included[t].length; c++) {
                for (int k = 0; k < included[t][c].length; k++) {
                    boolean exclude = (counts[t][c][k] < prm.featCountCutoff);
                    if (exclude) {
                        included[t][c][k] = false;
                    }
                }
            }
        }
    }
        
    public int getNumParams() {
        return numParams;
    }

    public int getNumTemplates() {
        return indices.length;
    }

    public int getNumConfigs(int ft) {
        return indices[ft].length;
    }

    public int getNumFeats(int ft, int c) {
        return indices[ft][c].length;
    }

    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            printModel(new OutputStreamWriter(baos));
            return baos.toString("UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void printModel(Writer writer) throws IOException {
        for (int t=0; t<numTemplates; t++) {
            FactorTemplate template = templates.get(t);
            int numConfigs = template.getNumConfigs();
            Alphabet<Feature> alphabet = template.getAlphabet();
            for (int c = 0; c < numConfigs; c++) {
                for (int k = 0; k < indices[t][c].length; k++) {
                    writer.write(template.getKey().toString());
                    writer.write("\t");
                    writer.write(template.getStateNamesStr(c));
                    writer.write("\t");
                    writer.write(alphabet.lookupObject(k).toString());
                    writer.write("\t");
                    writer.write(String.format("%d", indices[t][c][k]));
                    if (!included[t][c][k]) {
                        writer.write("\tEXCLUDED");
                    }
                    writer.write("\n");
                }
                writer.write("\n");
            }
            writer.write("\n");
        }
        writer.flush();
    }

    public FactorTemplateList getTemplates() {
        return templates;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
            
}
