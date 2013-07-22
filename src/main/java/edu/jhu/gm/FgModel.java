package edu.jhu.gm;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.List;

import edu.jhu.util.Alphabet;

/**
 * A model in the exponential family for a factor graph .
 * 
 * @author mgormley
 *
 */
public class FgModel implements Serializable {

    private static final long serialVersionUID = 4477788767217412525L;
    /** The model parameters. */
    private double[] params;
    /** A mapping of feature objects to model parameter indices. */
    private Alphabet<Feature> alphabet;
    
    public FgModel(List<Feature> features) {
        this.alphabet = new Alphabet<Feature>();
        for (Feature feat : features) {
            this.alphabet.lookupIndex(feat);
        }
        this.params = new double[alphabet.size()];
    }
    
    public FgModel(Alphabet<Feature> alphabet) {
        this.alphabet = alphabet;
        this.params = new double[alphabet.size()];
    }

    /** Gets the number of parameters in this model. */
    public int getNumParams() {
        return params.length;
    }

    public void setParams(double[] params) {
        this.params = params;
    }
    
    public double[] getParams() {
        return params;
    }
    
    public Alphabet<Feature> getAlphabet() {
        return alphabet;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int k=0; k<params.length; k++) {
            sb.append(alphabet.lookupObject(k));
            sb.append("=");
            sb.append(params[k]);
            sb.append("\n");
        }
        return sb.toString();
    }
    
    public void printModel(Writer writer) throws IOException {
        for (int k=0; k<params.length; k++) {
            writer.write(alphabet.lookupObject(k).toString());
            writer.write("=");
            writer.write(String.format("%.13g", params[k]));
            writer.write("\n");
        }
    }
    
}
