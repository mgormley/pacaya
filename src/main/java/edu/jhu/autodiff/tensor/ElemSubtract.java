package edu.jhu.autodiff.tensor;

import java.util.List;

import edu.jhu.autodiff.AbstractModule;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.util.collections.Lists;

/**
 * Elementwise subtraction of the entries in two tensors of identical size.
 * 
 * @author mgormley
 */
public class ElemSubtract extends AbstractModule<Tensor> implements Module<Tensor> {

    private Module<Tensor> modInX;
    private Module<Tensor> modInW;
    
    public ElemSubtract(Module<Tensor> modInX, Module<Tensor> modInW) {
        super(modInX.getAlgebra());
        checkEqualAlgebras(this, modInX, modInW);
        this.modInX = modInX;
        this.modInW = modInW;
    }
    
    /** Foward pass: y_i = x_i - w_i */
    @Override
    public Tensor forward() {
        Tensor x = modInX.getOutput();
        Tensor w = modInW.getOutput();
        y = x.copy();
        y.elemSubtract(w);
        return y;
    }

    /** 
     * Backward pass: 
     *    dG/dx_i += dG/dy_i dy_i/dx_i = dG/dy_i 
     *    dG/dw_i += dG/dy_i dy_i/dw_i = - dG/dy_i 
     */
    @Override
    public void backward() {
        modInX.getOutputAdj().elemAdd(yAdj);
        modInW.getOutputAdj().elemSubtract(yAdj);
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return Lists.getList(modInX, modInW);
    }

}
