package edu.jhu.pacaya.autodiff.tensor;

import java.util.List;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.Identity;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Scalar;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.util.collections.QLists;

/**
 * Multiplication of each entry in a tensor by a scalar from another tensor.
 * 
 * @author mgormley
 */
public class ScalarMultiply extends AbstractModule<Tensor> implements Module<Tensor> {

    private Module<Tensor> modInX;
    private Module<Tensor> modInW;
    // The index in w, which should be multiplied each x entry.
    private int k;
    
    public ScalarMultiply(Module<Tensor> modInX, double value) {
        super(modInX.getAlgebra());
        this.modInX = modInX;
        this.modInW = new Identity<Tensor>(Scalar.getInstance(s, value));
        this.k = 0;
    }
    
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
        y = new Tensor(x); // copy
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
            Tensor tmp = new Tensor(yAdj); // copy
            tmp.multiply(w_k);
            modInX.getOutputAdj().elemAdd(tmp);
        }
        {
            Tensor tmp = new Tensor(yAdj); // copy
            tmp.elemMultiply(x);
            modInW.getOutputAdj().addValue(k, tmp.getSum());
        }
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return QLists.getList(modInX, modInW);
    }
    
}
