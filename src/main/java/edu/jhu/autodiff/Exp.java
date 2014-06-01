package edu.jhu.autodiff;

import java.util.List;

import edu.jhu.util.collections.Lists;

/**
 * Takes the exp of each entry.
 * 
 * @author mgormley
 */
public class Exp extends AbstractTensorModule implements Module<Tensor> {

    private Module<Tensor> modInX;
    
    public Exp(Module<Tensor> modInX) {        
        super(modInX.getAlgebra());
        this.modInX = modInX;
    }
    
    /** Foward pass: y_i = exp(x_i) */
    @Override
    public Tensor forward() {
        Tensor x = modInX.getOutput();
        y = x.copy();
        y.exp();
        return y;
    }

    /** 
     * Backward pass: 
     *    dG/dx_i += dG/dy_i dy_i/dx_i = dG/dy_i exp(x_i)
     */
    @Override
    public void backward() {
        Tensor tmp = yAdj.copy();
        tmp.elemMultiply(y);
        modInX.getOutputAdj().elemAdd(tmp);
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return Lists.getList(modInX);
    }

}
