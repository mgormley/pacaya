package edu.jhu.pacaya.autodiff.tensor;

import java.util.List;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.Identity;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Scalar;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.util.collections.QLists;

/**
 * Fills each entry in a tensor with a scalar from another tensor.
 * 
 * @author mgormley
 */
public class ScalarFill extends AbstractModule<Tensor> implements Module<Tensor> {

    private Module<Tensor> modInX;
    private Module<Tensor> modInW;
    // The index in w, which should be multiplied each x entry.
    private int k;
    private final List<Module<Tensor>> inputs;

    public ScalarFill(Module<Tensor> modInX, double value) {
        super(modInX.getAlgebra());
        this.modInX = modInX;
        this.modInW = new Identity<Tensor>(Scalar.getInstance(s, value));
        this.k = 0;
        // Include only modInX in the inputs, since modInW is a constant.
        this.inputs = QLists.getList(modInX);
    }
    
    public ScalarFill(Module<Tensor> modInX, Module<Tensor> modInW, int k) {
        super(modInX.getAlgebra());
        checkEqualAlgebras(this, modInX, modInW);
        this.modInX = modInX;
        this.modInW = modInW;
        this.k = k;
        // Include both modInX and modInW in the inputs, since modInW is not constant.
        this.inputs = QLists.getList(modInX, modInW);
    }
    
    /** Foward pass: y_i = w_k */
    @Override
    public Tensor forward() {
        Tensor x = modInX.getOutput();
        double w_k = modInW.getOutput().getValue(k);
        y = new Tensor(x); // copy
        y.fill(w_k);
        return y;
    }

    /** 
     * Backward pass: 
     *    dG/dx_i += dG/dy_i dy_i/dx_i = 0
     *    dG/dw_k += \sum_{i=1}^n dG/dy_i dy_i/dw_k = \sum_{i=1}^n dG/dy_i
     */
    @Override
    public void backward() {
        modInX.getOutputAdj();
        modInW.getOutputAdj().addValue(k, yAdj.getSum());
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return inputs;
    }

}
