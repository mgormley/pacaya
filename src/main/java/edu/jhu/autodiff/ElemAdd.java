package edu.jhu.autodiff;

import java.util.List;

import edu.jhu.util.collections.Lists;

/**
 * Elementwise addition of the entries in two tensors of identical size.
 * 
 * @author mgormley
 */
public class ElemAdd extends AbstractTensorModule implements Module<Tensor> {

    private Module<Tensor> modInX;
    private Module<Tensor> modInW;
    
    public ElemAdd(Module<Tensor> modInX, Module<Tensor> modInW) {
        this.modInX = modInX;
        this.modInW = modInW;
    }
    
    /** Foward pass: y_i = x_i + w_i */
    @Override
    public Tensor forward() {
        Tensor x = modInX.getOutput();
        Tensor w = modInW.getOutput();
        y = x.copy();
        y.elemAdd(w);
        return y;
    }

    /** 
     * Backward pass: 
     *    dG/dx_i += dG/dy_i dy_i/dx_i = dG/dy_i 
     *    dG/dw_i += dG/dy_i dy_i/dw_i = dG/dy_i 
     */
    @Override
    public void backward() {
        modInX.getOutputAdj().elemAdd(yAdj);
        modInW.getOutputAdj().elemAdd(yAdj);
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return Lists.getList(modInX, modInW);
    }

}
