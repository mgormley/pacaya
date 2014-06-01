package edu.jhu.autodiff;

import java.util.List;

import edu.jhu.util.collections.Lists;

/**
 * Multiplication of each entry in a tensor by a scalar from another tensor.
 * 
 * @author mgormley
 */
public class ScalarMultiply extends AbstractTensorModule implements Module<Tensor> {

    private Module<Tensor> modInX;
    private Module<Tensor> modInW;
    // The index in w, which should be multiplied each x entry.
    private int k;
    
    public ScalarMultiply(Module<Tensor> modInX, Module<Tensor> modInW, int k) {
        super(modInX.getAlgebra());
        checkEqualAlgebras(this, modInX, modInW);
        this.modInX = modInX;
        this.modInW = modInW;
        this.k = k;
    }
    
    /** Foward pass: y_i = x_i * w_k */
    @Override
    public Tensor forward() {
        Tensor x = modInX.getOutput();
        double w_k = modInW.getOutput().getValue(k);
        y = x.copy();
        y.multiply(w_k);
        return y;
    }

    /** 
     * Backward pass: 
     *    dG/dx_i += dG/dy_i dy_i/dx_i = dG/dy_i w_k
     *    dG/dw_k += \sum_{i=1}^n dG/dy_i dy_i/dw_k = \sum_{i=1}^n dG/dy_i x_i
     */
    @Override
    public void backward() {
        Tensor x = modInX.getOutput();
        double w_k = modInW.getOutput().getValue(k);
        {
            Tensor tmp = yAdj.copy();
            tmp.multiply(w_k);
            modInX.getOutputAdj().elemAdd(tmp);
        }
        {
            Tensor tmp = yAdj.copy();
            tmp.elemMultiply(x);
            modInW.getOutputAdj().addValue(k, tmp.getSum());
        }
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return Lists.getList(modInX, modInW);
    }
    
}
