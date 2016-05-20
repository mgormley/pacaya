package edu.jhu.pacaya.autodiff.tensor;

import java.util.List;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.util.collections.QLists;

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
        y = new Tensor(x); // copy
        y.multiply(weightX);
        Tensor tmp = new Tensor(w); // copy
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
        Tensor tmp1 = new Tensor(yAdj); // copy
        tmp1.multiply(weightX);
        modInX.getOutputAdj().elemAdd(tmp1);

        Tensor tmp2 = new Tensor(yAdj); // copy
        tmp2.multiply(weightW);
        modInW.getOutputAdj().elemAdd(tmp2);
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return QLists.getList(modInX, modInW);
    }

}
