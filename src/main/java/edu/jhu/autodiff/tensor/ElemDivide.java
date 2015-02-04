package edu.jhu.autodiff.tensor;

import java.util.List;

import edu.jhu.autodiff.AbstractModule;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.prim.util.Lambda.FnIntDoubleToDouble;
import edu.jhu.util.collections.Lists;

/**
 * Elementwise division of the entries in two tensors of identical size.
 * 
 * @author mgormley
 */
public class ElemDivide extends AbstractModule<Tensor> implements Module<Tensor> {

    private Module<Tensor> modInX;
    private Module<Tensor> modInW;
    
    public ElemDivide(Module<Tensor> modInX, Module<Tensor> modInW) {
        super(modInX.getAlgebra());
        checkEqualAlgebras(this, modInX, modInW);
        this.modInX = modInX;
        this.modInW = modInW;
    }

    /** Foward pass: y_i = x_i / w_i */
    @Override
    public Tensor forward() {
        Tensor x = modInX.getOutput();
        Tensor w = modInW.getOutput();
        y = new Tensor(x); // copy
        y.elemDivide(w);
        return y;
    }

    /** 
     * Backward pass: 
     *    dG/dx_i += dG/dy_i dy_i/dx_i = dG/dy_i / w_i 
     *    dG/dw_i += dG/dy_i dy_i/dw_i = dG/dy_i * x_i / (- w_i^2)
     */
    @Override
    public void backward() {
        Tensor x = modInX.getOutput();
        Tensor w = modInW.getOutput();
        {
            Tensor tmp = new Tensor(yAdj); // copy
            tmp.elemDivide(w);
            correctForZeros(tmp);
            modInX.getOutputAdj().elemAdd(tmp);
        }
        {
            Tensor tmp = new Tensor(w); // copy
            tmp.fill(s.one());
            tmp.elemDivide(w);
            tmp.elemDivide(w);
            tmp.multiply(s.fromReal(-1));
            tmp.elemMultiply(yAdj);
            tmp.elemMultiply(x); 
            correctForZeros(tmp);
            modInW.getOutputAdj().elemAdd(tmp);
        }
    }

    private void correctForZeros(Tensor tmp) {
        // If the adjoint of y is zero, then zero the result.
        // This allows us to correct for NaNs introduced by division by zero.
        final Tensor yAdjFinal = yAdj;
        tmp.elemApply(new FnIntDoubleToDouble() {
            @Override
            public double call(int idx, double val) {
                if (yAdjFinal.getValue(idx) == s.zero()) {
                    return s.zero();
                } else {
                    return val;
                }
            }
        } );
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return Lists.getList(modInX, modInW);
    }

}
