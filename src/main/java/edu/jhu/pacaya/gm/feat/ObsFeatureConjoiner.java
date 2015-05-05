package edu.jhu.pacaya.gm.feat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

import edu.jhu.pacaya.gm.data.FgExampleList;
import edu.jhu.pacaya.gm.data.LFgExample;
import edu.jhu.pacaya.gm.inf.FgInferencer;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.IFgModel;
import edu.jhu.pacaya.gm.model.IndexForVc;
import edu.jhu.pacaya.gm.model.ObsFeatureCarrier;
import edu.jhu.pacaya.gm.model.TemplateFactor;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;
import edu.jhu.pacaya.gm.util.ArrayIter3D;
import edu.jhu.pacaya.util.FeatureNames;
import edu.jhu.pacaya.util.Prm;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.arrays.BoolArrays;
import edu.jhu.prim.iter.IntIter;
import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.prim.vector.IntIntDenseVector;

/**
 * Wrapper of ObsFeatureExtractor which returns feature vectors which conjoin
 * the observation function with an indicator of the predicted variables.
 * 
 * @author mgormley
 */
public class ObsFeatureConjoiner implements Serializable {

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
    
    private static final Logger log = LoggerFactory.getLogger(ObsFeatureConjoiner.class);
    private static final long serialVersionUID = 1L;
    
    /**
     * The model parameters indices. Indexed by feature template index, variable
     * assignment config index, and observation function feature index.
     */
    int[][][] indices;
    /**
     * Whether or not the correspondingly indexed model parameter is included in
     * this model.
     */
    boolean[][][] included;
    /** The number of feature templates. */
    private int numTemplates;
    /** The number of parameters in the model. */
    private int numParams;
    /** The feature templates. */
    private FactorTemplateList templates;
    /** Whether this object is initialized. */
    private boolean initialized;
    /** Alphabet for standard (non-observation function) features. */
    private FeatureNames feAlphabet;
    /** Maximum feature-hashing trick modulo value in use. */
    private int featureHashModMax = 0;
    
    private ObsFeatureConjoinerPrm prm;
    
    public ObsFeatureConjoiner(ObsFeatureConjoinerPrm prm, FactorTemplateList fts) {
        this.prm = prm;
        initialized = false;
        this.templates = fts;
        this.feAlphabet = new FeatureNames();
    }
    
    public void init() {
        if (!prm.includeUnsupportedFeatures) {
            log.warn("Enabling includeUnsupportedFeatures");
            prm.includeUnsupportedFeatures = true;
        }
        if (prm.featCountCutoff >= 1) {
            log.warn("Disabling featCountCutoff");
            prm.featCountCutoff = -1;
        }
        init(null);
    }
        
    public void init(FgExampleList data) {
        // Ensure the FactorTemplateList is initialized, and maybe count features along the way.
        if (templates.isGrowing() && data != null) {
            log.info("Growing feature template list by iterating over examples");
            extractAllFeats(data, templates);
            templates.stopGrowth();
            feAlphabet.stopGrowth();
        }        
        numTemplates = templates.size();
        
        // Apply a feature count cutoff.
        this.included = new boolean[numTemplates][][];
        for (int t=0; t<numTemplates; t++) {
            FactorTemplate template = templates.get(t);
            int numConfigs = template.getNumConfigs();
            int numFeats = template.getAlphabet().size();
            included[t] = new boolean[numConfigs][numFeats];
        }
        BoolArrays.fill(included, true);        
        if (!prm.includeUnsupportedFeatures) {
            prm.featCountCutoff = Math.max(prm.featCountCutoff, 1);
        }
        if (prm.featCountCutoff >= 1) {
            log.info("Applying feature count cutoff: " + prm.featCountCutoff);
            IntIntDenseVector[][] counts = countFeatures(data, templates);
            excludeByFeatCount(counts);
        }
        
      
        // Always include the bias features.
        for (int t=0; t<included.length; t++) {
            FactorTemplate template = templates.get(t);
            FeatureNames alphabet = template.getAlphabet();            
            for (int k = 0; k < alphabet.size(); k++) {
                if (alphabet.isBiasFeature(k)) {
                    for (int c = 0; c < included[t].length; c++) {
                        included[t][c][k] = true;
                    }
                }
            }
        }

        // Set the indices to track only the included parameters.
        // All other entries are set to -1.
        // Also: Count the number of parameters, accounting for excluded params.
        numParams = feAlphabet.size();
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
        // If we are using the feature hashing trick, we may want to further increase
        // the number of model parameters.
        numParams = Math.max(numParams, featureHashModMax);
        
        initialized = true;
    }

    private static class NoOpInferencer implements FgInferencer {

        private FactorGraph fg;
        
        private NoOpInferencer(FactorGraph fg) {
            this.fg = fg;
        }
        
        @Override
        public VarTensor getMarginalsForFactorId(int factorId) { 
            return new VarTensor(RealAlgebra.REAL_ALGEBRA, fg.getFactor(factorId).getVars()); 
        }
        
        @Override
        public void run() { throw new RuntimeException("This method should never be called."); }
                
        @Override
        public double getPartition() { throw new RuntimeException("This method should never be called."); }
        
        @Override
        public VarTensor getMarginalsForVarId(int varId) { throw new RuntimeException("This method should never be called."); }
        
        @Override
        public VarTensor getMarginals(Factor factor) { throw new RuntimeException("This method should never be called."); }
        
        @Override
        public VarTensor getMarginals(Var var) { throw new RuntimeException("This method should never be called."); }
        
        @Override
        public double getLogPartition() { throw new RuntimeException("This method should never be called."); }
        
        @Override
        public VarTensor getLogMarginalsForVarId(int varId) { throw new RuntimeException("This method should never be called."); }
        
        @Override
        public VarTensor getLogMarginals(Factor factor) { throw new RuntimeException("This method should never be called."); }
        
        @Override
        public VarTensor getLogMarginals(Var var) { throw new RuntimeException("This method should never be called."); }

        @Override
        public VarTensor getLogMarginalsForFactorId(int factorId) { throw new RuntimeException("This method should never be called."); }
            
    }
    
    /**
     * Loops through all examples to create the features, thereby ensuring that the FTS are initialized.
     */
    private void extractAllFeats(FgExampleList data, FactorTemplateList templates) {
        // Create a "no-op" counter.
        IFgModel counts = new IFgModel() {                        
            @Override
            public void addAfterScaling(FeatureVector fv, double multiplier) { }
            @Override
            public void add(int feat, double addend) { }
        };  
        
        // Loop over all factors in the dataset. 
        for (int i=0; i<data.size(); i++) {
            if (i % 1000 == 0) {
                log.debug("Processing example: " + i);
            }
            LFgExample ex = data.get(i);
            // Create a "no-op" inferencer, which returns arbitrary marginals.
            NoOpInferencer inferencer = new NoOpInferencer(ex.getFgLatPred());   
            for (int a=0; a<ex.getOriginalFactorGraph().getNumFactors(); a++) {
                Factor f = ex.getFgLat().getFactor(a);
                if (f instanceof ObsFeatureCarrier && f instanceof TemplateFactor) {
                    // For each observation function extractor.
                    int t = templates.getTemplateId((TemplateFactor) f);
                    if (t != -1) {
                        ((ObsFeatureCarrier) f).getObsFeatures();                                      
                    }
                } else {
                    // For each standard factor.  
                    f = ex.getFgLatPred().getFactor(a);
                    if (f instanceof GlobalFactor) {
                        ((GlobalFactor) f).addExpectedFeatureCounts(counts, 0, inferencer, a);
                    } else {
                        VarTensor marg = inferencer.getMarginalsForFactorId(a);
                        f.addExpectedFeatureCounts(counts, marg, 0);
                    }
                    
                }
            }
        }
    }
    
    /**
     * Counts the number of times each feature appears in the gold training data.
     */
    private IntIntDenseVector[][] countFeatures(FgExampleList data, FactorTemplateList templates) {
        IntIntDenseVector[][] counts = new IntIntDenseVector[numTemplates][];
        for (int t=0; t<numTemplates; t++) {
            FactorTemplate template = templates.get(t);
            int numConfigs = template.getNumConfigs();
            int numFeats = template.getAlphabet().size();
            counts[t] = new IntIntDenseVector[numConfigs];
            for (int c=0; c<numConfigs; c++) {
                counts[t][c] = new IntIntDenseVector(numFeats);
            }
        }
        for (int i=0; i<data.size(); i++) {
            LFgExample ex = data.get(i);
            for (int a=0; a<ex.getOriginalFactorGraph().getNumFactors(); a++) {
                Factor f = ex.getFgLat().getFactor(a);
                if (f instanceof ObsFeatureCarrier && f instanceof TemplateFactor) {
                    int t = templates.getTemplateId((TemplateFactor) f);
                    if (t != -1) {
                        FeatureVector fv = ((ObsFeatureCarrier) f).getObsFeatures();                            
                        if (f.getVars().size() == 0) {
                            int predConfig = ex.getGoldConfigIdxPred(a);
                            for (IntDoubleEntry entry : fv) {
                                counts[t][predConfig].add(entry.index(), 1);
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
                                    counts[t][config].add(entry.index(), 1);
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
    private void excludeByFeatCount(IntIntDenseVector[][] counts) {
        for (int t=0; t<included.length; t++) {
            for (int c = 0; c < included[t].length; c++) {
                for (int k = 0; k < included[t][c].length; k++) {
                    boolean exclude = (counts[t][c].get(k) < prm.featCountCutoff);
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
    
    public FeatureNames getFeAlphabet() {
        return feAlphabet;
    }
    
    public int getFeatureHashModMax() {
        return featureHashModMax;
    }

    public void takeNoteOfFeatureHashMod(int featureHashMod) {
        this.featureHashModMax = Math.max(featureHashModMax, featureHashMod);
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
            FeatureNames alphabet = template.getAlphabet();
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

    public Iterable<String> getParamNames() {
        return Iterables.concat(new StringIterable(feAlphabet.getObjects()), new ParamNames());        
    }
    
    private class ParamNames implements Iterable<String>, Iterator<String>, Serializable {

        private static final long serialVersionUID = 1L;
        private ArrayIter3D iter;
        private boolean hasNext;
        
        @Override
        public Iterator<String> iterator() {
            iter = new ArrayIter3D(indices);
            hasNext = iter.next();
            return this;
        }
        
        @Override
        public boolean hasNext() {
            skipNonIncluded();
            return hasNext;
        }
        
        @Override
        public String next() {
            int t = iter.i;
            int c = iter.j;
            int k = iter.k;
            
            FactorTemplate template = templates.get(t);
            FeatureNames alphabet = template.getAlphabet();
            
            StringBuilder name = new StringBuilder();
            name.append(template.getKey().toString());
            name.append("_");
            name.append(template.getStateNamesStr(c));
            name.append("_");
            name.append(alphabet.lookupObject(k).toString());
            name.append("_");
            name.append(String.format("%d", indices[t][c][k]));
            hasNext = iter.next();
            return name.toString(); 
        }

        private void skipNonIncluded() {
            while (!included[iter.i][iter.j][iter.k]) {
                hasNext = iter.next();
            }
        }

        @Override
        public void remove() {
            throw new RuntimeException("not supported");
        }
        
    }

    public FactorTemplateList getTemplates() {
        return templates;
    }
    
    public boolean isInitialized() {
        return initialized;
    }

    public int getFeatIndex(int t, int c, int feat) {
        return indices[t][c][feat];
    }
    
}
