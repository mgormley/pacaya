package edu.jhu.gm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;

import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.srl.MutableInt;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Lambda.LambdaBinOpDouble;
import edu.jhu.util.Lambda.LambdaUnaryOpDouble;
import edu.jhu.util.Utilities;
import edu.jhu.util.dist.Gaussian;

/**
 * A model in the exponential family for a factor graph .
 * 
 * @author mgormley
 *
 */
// TODO: rename to CrfModel.
// TODO: Internally we could store the parameters of a particular feature
// template/config as a SparseVector so that the footprint of this object
// (particularly when serialized) is smaller.
public class FgModel implements Serializable {

    public interface TemplateModel {

        void add(int config, int feat, double addend);
        
        double[] getParams(int config);
        
        void printModel(Writer writer) throws IOException; 
        
        int getNumParams();

        int getNumConfigs();

        int getNumFeats(int c);
        
        void apply(LambdaBinOpDouble lambda);
    }
    
    private static final long serialVersionUID = 4477788767217412525L;
    /**
     * The model parameters. Indexed by feature template index, variable
     * assignment config index, and observation function feature index.
     */
    private double[][][] params;
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
    private FeatureTemplateList templates;
    
    public FgModel(FgExamples data, boolean includeUnsupportedFeatures) {
        this(data, data.getTemplates(), includeUnsupportedFeatures);
    }
    
    public FgModel(FeatureTemplateList templates) {
        this(null, templates, true);
    }
    
    private FgModel(FgExamples data, FeatureTemplateList templates, boolean includeUnsupportedFeatures) {
        this.templates = templates;
        numTemplates = templates.size();
        
        this.params = new double[numTemplates][][];
        this.included = new boolean[numTemplates][][];
        for (int t=0; t<numTemplates; t++) {
            FeatureTemplate template = templates.get(t);
            int numConfigs = template.getNumConfigs();
            Alphabet<Feature> alphabet = template.getAlphabet();
            params[t] = new double[numConfigs][alphabet.size()];
            included[t] = new boolean[numConfigs][alphabet.size()];
        }
        
        if (!includeUnsupportedFeatures) {
            includeSupportedFeatures(data, templates);
        } else {
            Utilities.fill(included, true);
        }
      
        // Always include the bias features.
        for (int t=0; t<params.length; t++) {
            FeatureTemplate template = templates.get(t);
            Alphabet<Feature> alphabet = template.getAlphabet();            
            for (int k = 0; k < alphabet.size(); k++) {
                if (alphabet.lookupObject(k).isBiasFeature()) {
                    for (int c = 0; c < params[t].length; c++) {
                        included[t][c][k] = true;
                    }        
                }
            }
        }
        
        // Count the number of parameters, accounting for excluded params.
        final MutableInt count = new MutableInt(0);
        apply(new LambdaUnaryOpDouble() {
            public double call(double v) { count.increment(); return v; }
        });
        numParams = count.get();
    }
    
    /** Copy constructor. */
    public FgModel(FgModel other) {
        this.params = Utilities.copyOf(other.params);
        this.included = Utilities.copyOf(other.included);
        this.numParams = other.numParams;
        this.numTemplates = other.numTemplates;
        // We only do a shallow copy of the templates.
        this.templates = other.templates;
    }

    /**
     * For each factor in the data, lookup its configId. Set all the
     * observed features for that configuration to true.
     */
    private void includeSupportedFeatures(FgExamples data, FeatureTemplateList templates) {
        for (int i=0; i<data.size(); i++) {
            FgExample ex = data.get(i);
            for (int a=0; a<ex.getOriginalFactorGraph().getNumFactors(); a++) {
                Factor f = ex.getFgLat().getFactor(a);
                if (f instanceof GlobalFactor) {
                    continue;
                } else if (f instanceof ExpFamFactor) {
                    int t = templates.getTemplateId(f);
                    if (t != -1) {
                        FeatureVector fv = ex.getObservationFeatures(a);
                        if (f.getVars().size() == 0) {
                            int predConfig = ex.getGoldConfigIdxPred(a);
                            for (IntDoubleEntry entry : fv) {
                                //System.out.println(predConfig);
                                included[t][predConfig][entry.index()] = true;
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
                                    included[t][config][entry.index()] = true;
                                }
                            }
                        }
                    }
                } else {
                    throw new UnsupportedFactorTypeException(f);
                }
            }
        }
    }
    
    public void updateModelFromDoubles(double[] inParams) {
        assert (numParams == inParams.length);
        int t = 0; 
        int c = 0;
        int k = -1;
        for (int i=0; i<inParams.length; i++) {
            // Increment t,c,k indices.
            do {
                k++;
                if (k >= params[t][c].length) {
                    k = 0;
                    c++;
                }
                if (c >= params[t].length) {
                    c = 0;
                    t++;
                }
            } while(!included[t][c][k]);
            
            // Update the model.
            params[t][c][k] = inParams[i];
        }
    }
    
    public void updateDoublesFromModel(double[] outParams) {
        assert (numParams == outParams.length);
        int i=0;
        for (int t=0; t<params.length; t++) {
            for (int c = 0; c < params[t].length; c++) {
                for (int k = 0; k < params[t][c].length; k++) {
                    if (included[t][c][k]) {
                        // Update the doubles.
                        outParams[i++] = params[t][c][k];
                    }
                }
            }
        }
    }
    public void add(int ft, int config, int feat, double addend) {
      if (!included[ft][config][feat]) {
          throw new IllegalArgumentException("The specified parameter is not included in this model");
      }
      params[ft][config][feat] += addend;
    }

    public void addIfParamExists(int ft, int config, int feat, double addend) {
        if (included[ft][config][feat]) {
            params[ft][config][feat] += addend;
        }
    }
    
    public double[] getParams(int ft, int config) {
        return params[ft][config];
    }
    
    public int getNumParams() {
        return numParams;
    }

    public int getNumTemplates() {
        return params.length;
    }

    public int getNumConfigs(int ft) {
        return params[ft].length;
    }

    public int getNumFeats(int ft, int c) {
        return params[ft][c].length;
    }
    
    public void zero() {
        Utilities.fill(params, 0.0);
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
            FeatureTemplate template = templates.get(t);
            int numConfigs = template.getNumConfigs();
            Alphabet<Feature> alphabet = template.getAlphabet();
            for (int c = 0; c < numConfigs; c++) {
                //VarConfig vc = vars.getVarConfig(c);
                for (int k = 0; k < params[t][c].length; k++) {
                    if (included[t][c][k]) {
                        writer.write(template.getKey().toString());
                        writer.write("_");
                        writer.write(template.getStateNamesStr(c));
                        writer.write("_");
                        writer.write(alphabet.lookupObject(k).toString());
                        writer.write("\t");
                        writer.write(String.format("%.13g", params[t][c][k]));
                        writer.write("\n");
                    }
                }
            }
        }
        writer.flush();
    }

    public FeatureTemplateList getTemplates() {
        return templates;
    }

    /**
     * Fill the model parameters with values randomly drawn from ~ Normal(0, 1).
     */
    public void setRandomStandardNormal() {
        LambdaUnaryOpDouble lambda = new LambdaUnaryOpDouble() {
            @Override
            public double call(double obj) {
                return Gaussian.nextDouble(0.0, 1.0);
            }
        };
        apply(lambda);
    }

    public void fill(final double value) {
        apply(new LambdaUnaryOpDouble() {
            @Override
            public double call(double obj) {
                return value;
            }
        });
    }
    
    public void apply(LambdaUnaryOpDouble lambda) {
        for (int t=0; t<params.length; t++) {
            for (int c = 0; c < params[t].length; c++) {
                for (int k = 0; k < params[t][c].length; k++) {
                    if (included[t][c][k]) {
                        params[t][c][k] = lambda.call(params[t][c][k]);
                    }
                }
            }
        }
    }     
        
}
