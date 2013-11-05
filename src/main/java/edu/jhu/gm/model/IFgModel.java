package edu.jhu.gm.model;

import java.io.IOException;
import java.io.Writer;

import edu.jhu.gm.feat.FeatureTemplateList;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.prim.util.Lambda.LambdaUnaryOpDouble;

public interface IFgModel {

    void add(int ft, int config, int feat, double addend);

    void addIfParamExists(int ft, int config, int feat, double addend);

    void addIfParamExists(int t, int c, FeatureVector fv, double multiplier);
    
//    void updateModelFromDoubles(double[] inParams);
//
//    void updateDoublesFromModel(double[] outParams);
//
//    double[] getParams(int ft, int config);
//
//    int getNumParams();
//
//    int getNumTemplates();
//
//    int getNumConfigs(int ft);
//
//    int getNumFeats(int ft, int c);
//
//    void zero();
//
//    String toString();
//
//    void printModel(Writer writer) throws IOException;
//
//    FeatureTemplateList getTemplates();
//
//    /**
//     * Fill the model parameters with values randomly drawn from ~ Normal(0, 1).
//     */
//    void setRandomStandardNormal();
//
//    void fill(double value);
//
//    void apply(LambdaUnaryOpDouble lambda);
//
//    void scale(double multiplier);

}