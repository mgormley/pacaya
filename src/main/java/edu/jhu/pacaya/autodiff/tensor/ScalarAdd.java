package edu.jhu.pacaya.autodiff.tensor;

import java.util.List;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.util.collections.Lists;

/**
 * Addition of each entry in a tensor by a scalar from another tensor.
 * 
 * @author mgormley
 */
public class ScalarAdd extends AbstractModule<Tensor> implements Module<Tensor> {

    private Module<Tensor> modInX;
    private Module<Tensor> modInW;
    // The index in w, which should be multiplied each x entry.
    private int k;
    
    public ScalarAdd(Module<Tensor> modInX, Module<Tensor> modInW, int k) {
        super(modInX.getAlgebra());
        checkEqualAlgebras(this, modInX, modInW);
        this.modInX = modInX;
        this.modInW = modInW;
        this.k = k;
    }
    
    /** Foward pass: y_i = x_i + w_k */
    @Override
    public Tensor forward() {
        Tensor x = modInX.getOutput();
        double w_k = modInW.getOutput().getValue(k);
        y = new Tensor(x); // copy
        y.add(w_k);
        return y;
    }

    /** 
     * Backward pass: 
     *    dG/dx_i += dG/dy_i dy_i/dx_i = dG/dy_i
     *    dG/dw_k += \sum_{i=1}^n dG/dy_i dy_i/dw_k = \sum_{i=1}^n dG/dy_i
     */
    @Override
    public void backward() {
        modInX.getOutputAdj().elemAdd(yAdj);
        modInW.getOutputAdj().addValue(k, yAdj.getSum());
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return Lists.getList(modInX, modInW);
    }

}
