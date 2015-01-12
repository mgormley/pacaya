package edu.jhu.autodiff.tensor;

import java.util.List;

import edu.jhu.autodiff.AbstractModule;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.util.collections.Lists;

/**
 * Elementwise linear combination of the entries in two tensors of identical size.
 * The weights are treated as constants.
 * 
 * @author mgormley
 */
public class ElemLinear extends AbstractModule<Tensor> implements Module<Tensor> {

    private Module<Tensor> modInX;
    private Module<Tensor> modInW;
    private double weightX;
    private double weightW;
    
    /**
     * Constructs a linear combination of two tensors.
     * @param modInX The tensor x.
     * @param modInW The tensor w.
     * @param weightX The weight of the elements in x. Assumed to be in the real semiring.
     * @param weightW The weight of the elements in w. Assumed to be in the real semiring.
     */
    public ElemLinear(Module<Tensor> modInX, Module<Tensor> modInW, double weightX, double weightW) {
        super(modInX.getAlgebra());
        checkEqualAlgebras(this, modInX, modInW);
        this.modInX = modInX;
        this.modInW = modInW;
        // Convert from the real semiring to this module's semiring.
        this.weightX = s.fromReal(weightX);
        this.weightW = s.fromReal(weightW);
    }
    
    /** Foward pass: y_i = \lambda x_i + \gamma w_i */
    @Override
    public Tensor forward() {
        Tensor x = modInX.getOutput();
        Tensor w = modInW.getOutput();
        y = x.copy();
        y.multiply(weightX);
        Tensor tmp = w.copy();
        tmp.multiply(weightW);
        y.elemAdd(tmp);
        return y;
    }

    /** 
     * Backward pass: 
     *    dG/dx_i += dG/dy_i dy_i/dx_i = dG/dy_i \lambda
     *    dG/dw_i += dG/dy_i dy_i/dw_i = dG/dy_i \gamma
     */
    @Override
    public void backward() {
        Tensor tmp1 = yAdj.copy();
        tmp1.multiply(weightX);
        modInX.getOutputAdj().elemAdd(tmp1);

        Tensor tmp2 = yAdj.copy();
        tmp2.multiply(weightW);
        modInW.getOutputAdj().elemAdd(tmp2);
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return Lists.getList(modInX, modInW);
    }

}
