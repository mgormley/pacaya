package edu.jhu.pacaya.autodiff.tensor;

import java.util.List;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.util.collections.QLists;

/**
 * Elementwise multiplication of the entries in two tensors of identical size.
 * 
 * @author mgormley
 */
public class ElemMultiply extends AbstractModule<Tensor> implements Module<Tensor> {

    private Module<Tensor> modInX;
    private Module<Tensor> modInW;
    
    public ElemMultiply(Module<Tensor> modInX, Module<Tensor> modInW) {
        super(modInX.getAlgebra());
        checkEqualAlgebras(this, modInX, modInW);
        this.modInX = modInX;
        this.modInW = modInW;
    }
    
    /** Foward pass: y_i = x_i * w_i */
    @Override
    public Tensor forward() {
        Tensor x = modInX.getOutput();
        Tensor w = modInW.getOutput();
        y = new Tensor(x); // copy
        y.elemMultiply(w);
        return y;
    }

    /** 
     * Backward pass: 
     *    dG/dx_i += dG/dy_i dy_i/dx_i = dG/dy_i w_i 
     *    dG/dw_i += dG/dy_i dy_i/dw_i = dG/dy_i x_i
     */
    @Override
    public void backward() {
        Tensor x = modInX.getOutput();
        Tensor w = modInW.getOutput();
        {
            Tensor tmp = new Tensor(yAdj); // copy
            tmp.elemMultiply(w);
            modInX.getOutputAdj().elemAdd(tmp);
        }
        {
            Tensor tmp = new Tensor(yAdj); // copy
            tmp.elemMultiply(x);
            modInW.getOutputAdj().elemAdd(tmp);
        }
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return QLists.getList(modInX, modInW);
    }

}
