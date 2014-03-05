package edu.jhu.gm.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.Iterator;

import org.apache.commons.lang.mutable.MutableDouble;
import org.apache.log4j.Logger;

import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.prim.map.IntDoubleMap;
import edu.jhu.prim.map.IntDoubleSortedMap;
import edu.jhu.prim.util.Lambda.FnIntDoubleToDouble;
import edu.jhu.prim.util.Lambda.LambdaUnaryOpDouble;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.prim.vector.IntDoubleHashVector;
import edu.jhu.prim.vector.IntDoubleVector;
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
public class FgModel implements Serializable, IFgModel {

    private static final Logger log = Logger.getLogger(FgModel.class);

    private static final long serialVersionUID = 4477788767217412525L;
    /** The model parameters. */
    private final IntDoubleVector params;
    /** The number of model parameters. */
    private int numParams;
    /** Provides iteration of the model parameter names. */
    private Iterable<String> paramNames;
    
    public FgModel(int numParams) {
        this(numParams, null);
    }
    
    public FgModel(int numParams, Iterable<String> paramNames) {
        this.numParams = numParams;
        this.params = new IntDoubleDenseVector(numParams);
        for (int i=0; i<numParams; i++) {
            params.set(i, 0.0);
        }
        this.paramNames = paramNames;
    }
    
    /** Shallow copy constructor which also sets params. */
    private FgModel(FgModel other, IntDoubleVector params) {
        this.params = params;
        this.numParams = other.numParams;
    }
    
    /** Copy constructor. */
    public FgModel getDenseCopy() {
        return new FgModel(this, new IntDoubleDenseVector(params));
    }
    
    /** Copy constructor, which initializes the parameter vector to all zeros. */
    public FgModel getSparseZeroedCopy() {
        return new FgModel(this, new IntDoubleHashVector(10000));
    }

    public void updateModelFromDoubles(double[] inParams) {
        assert numParams == inParams.length : String.format("numParams=%d inParams.length=%d", numParams, inParams.length);
        for (int i=0; i<numParams; i++) {
            this.params.set(i, inParams[i]);
        }
    }
    
    public void updateDoublesFromModel(double[] outParams) {
        assert (numParams == outParams.length);
        for (int i=0; i<numParams; i++) {
            outParams[i] = this.params.get(i);
        }
    }
    
    public void add(int feat, double addend) {
      if (feat < 0 || numParams <= feat) {
          throw new IllegalArgumentException("The specified parameter is not included in this model");
      }
      params.add(feat, addend);
    }

    public void addAfterScaling(FeatureVector fv, double multiplier) {
        int[] fvInd = fv.getInternalIndices();
        double[] fvVal = fv.getInternalValues();
        for (int i=0; i<fvInd.length; i++) {
            add(fvInd[i], multiplier * fvVal[i]);
        }
    }
    
    private boolean shouldLogNumExplicitParams = true;
    
    public void add(FgModel other) {
        if (other.numParams != this.numParams) {
            throw new IllegalStateException("Only copies of this model can be added to it.");
        }
        if (shouldLogNumExplicitParams && other.params instanceof IntDoubleMap) {
            // Only log this once for posterity.
            log.debug(String.format("Adding %d explicit params to model from a %s.",
                    ((IntDoubleMap) other.params).size(), other.params.getClass()));
            shouldLogNumExplicitParams = false;
        }
        this.params.add(other.params);
    }
    
    public double dot(FeatureVector fv) {     
        return params.dot(fv);
    }
    
    public int getNumParams() {
        return numParams;
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
        Iterator<String> iter = paramNames.iterator();
        for (int i=0; i<numParams; i++) {
            if (paramNames == null) {
                writer.write(String.format("%d", i));
            } else {
                writer.write(iter.next());
            }
            writer.write("\t");
            writer.write(String.format("%.13g", params.get(i)));
            writer.write("\n");
        }
        writer.flush();
    }
    
    public void apply(FnIntDoubleToDouble lambda) {
        params.apply(lambda);
    }

    /** ONLY FOR TESTING. */
    void apply(final LambdaUnaryOpDouble lambda) {
        params.apply(new FnIntDoubleToDouble() {
            @Override
            public double call(int idx, double val) {
                return lambda.call(val);
            }
        });
    }

    /**
     * Fill the model parameters with values randomly drawn from ~ Normal(0, 1).
     */
    public void setRandomStandardNormal() {
        FnIntDoubleToDouble lambda = new FnIntDoubleToDouble() {
            @Override
            public double call(int idx, double val) {
                return Gaussian.nextDouble(0.0, 1.0);
            }
        };
        apply(lambda);
    }

    public void fill(final double value) {
        apply(new FnIntDoubleToDouble() {
            @Override
            public double call(int idx, double ignored) {
                return value;
            }
        });
    }
    
    public void zero() {
        fill(0.0);
    }

    public void scale(final double multiplier) {
        apply(new FnIntDoubleToDouble() {
            @Override
            public double call(int idx, double val) {
                return multiplier * val;
            }
        });
    }

    public double l2Norm() {
        final MutableDouble l2Norm = new MutableDouble(0);
        apply(new FnIntDoubleToDouble() {            
            @Override
            public double call(int idx, double val) {
                l2Norm.add(val*val);
                return val;
            }
        });
        return l2Norm.doubleValue();
    }
        
}
