package edu.jhu.autodiff;

import java.util.List;

import edu.jhu.util.collections.Lists;

/**
 * Division of each entry in a tensor by a scalar from another tensor.
 * 
 * @author mgormley
 */
public class ScalarDivide extends AbstractTensorModule implements Module<Tensor> {

    private Module<Tensor> modInX;
    private Module<Tensor> modInW;
    // The index in w, by which each x entry should be divided.
    private int k;
    
    public ScalarDivide(Module<Tensor> modInX, Module<Tensor> modInW, int k) {
        this.modInX = modInX;
        this.modInW = modInW;
        this.k = k;
    }
    
    /** Foward pass: y_i = x_i / w_k */
    @Override
    public Tensor forward() {
        Tensor x = modInX.getOutput();
        double w_k = modInW.getOutput().getValue(k);
        y = x.copy();
        y.divide(w_k);
        return y;
    }

    /** 
     * Backward pass: 
     *    dG/dx_i += dG/dy_i dy_i/dx_i = dG/dy_i / w_k
     *    dG/dw_k += \sum_{i=1}^n dG/dy_i dy_i/dw_k = \sum_{i=1}^n dG/dy_i x_i / (- w_k^2)
     */
    @Override
    public void backward() {
        Tensor x = modInX.getOutput();
        double w_k = modInW.getOutput().getValue(k);
        {
            Tensor tmp = yAdj.copy();
            tmp.divide(w_k);
            modInX.getOutputAdj().elemAdd(tmp);
        }
        {
            Tensor tmp = yAdj.copy();
            tmp.elemMultiply(x);
            tmp.divide(- (w_k * w_k));
            modInW.getOutputAdj().addValue(k, tmp.getSum());
        }
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return Lists.getList(modInX, modInW);
    }

}
