package edu.jhu.pacaya.autodiff.tensor;

import java.util.List;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.Identity;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Scalar;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.util.collections.QLists;

/**
 * Division of each entry in a tensor by a scalar from another tensor.
 * 
 * @author mgormley
 */
public class ScalarDivide extends AbstractModule<Tensor> implements Module<Tensor> {

    private Module<Tensor> modInX;
    private Module<Tensor> modInW;
    // The index in w, by which each x entry should be divided.
    private int k;
    private final List<Module<Tensor>> inputs;

    public ScalarDivide(Module<Tensor> modInX, double value) {
        super(modInX.getAlgebra());
        this.modInX = modInX;
        this.modInW = new Identity<Tensor>(Scalar.getInstance(s, value));
        this.k = 0;
        // Include only modInX in the inputs, since modInW is a constant.
        this.inputs = QLists.getList(modInX);
    }
    
    public ScalarDivide(Module<Tensor> modInX, Module<Tensor> modInW, int k) {
        super(modInX.getAlgebra());
        checkEqualAlgebras(this, modInX, modInW);
        this.modInX = modInX;
        this.modInW = modInW;
        this.k = k;
        // Include both modInX and modInW in the inputs, since modInW is not constant.
        this.inputs = QLists.getList(modInX, modInW);
    }
    
    /** Foward pass: y_i = x_i / w_k */
    @Override
    public Tensor forward() {
        Tensor x = modInX.getOutput();
        double w_k = modInW.getOutput().getValue(k);
        y = new Tensor(x); // copy
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
            Tensor tmp = new Tensor(yAdj); // copy
            tmp.divide(w_k);
            modInX.getOutputAdj().elemAdd(tmp);
        }
        {
            Tensor tmp = new Tensor(yAdj); // copy
            tmp.elemMultiply(x);
            tmp.divide(s.negate(s.times(w_k, w_k)));
            modInW.getOutputAdj().addValue(k, tmp.getSum());
        }
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return inputs;
    }

}
