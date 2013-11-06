package edu.jhu.gm.model;

import java.io.IOException;
import java.io.Writer;

import edu.jhu.prim.util.Lambda.LambdaBinOpDouble;

/**
 * Model for a single template in a CRF/MRF.
 * @author mgormley
 */
// TODO: Use this interface.
public interface TemplateModel {

    void add(int config, int feat, double addend);
    
    double[] getParams(int config);
    
    void printModel(Writer writer) throws IOException; 
    
    int getNumParams();

    int getNumConfigs();

    int getNumFeats(int c);
    
    void apply(LambdaBinOpDouble lambda);
}