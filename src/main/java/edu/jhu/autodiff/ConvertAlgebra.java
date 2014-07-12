package edu.jhu.autodiff;

import java.util.List;

import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;

/**
 * Converts from the abstract algebra of the input to a given abstract algebra.
 * @author mgormley
 */
public class ConvertAlgebra extends AbstractTensorModule implements Module<Tensor> {

    private Module<Tensor> modIn;
    
    public ConvertAlgebra(Module<Tensor> modIn, Algebra s) {
        super(s);
        this.modIn = modIn;
    }
    
    /** 
     * Foward pass: y[i] = x[i]. 
     * x[i] is converted from a real to its semiring form.
     */
    @Override
    public Tensor forward() {
        Tensor x = modIn.getOutput();
        y = x.copyAndConvertAlgebra(s);
        return y;
    }

    /** 
     * Backward pass: dG/dx_i = dG/dy_i. 
     * dG/dy_i is converted to a real from the semiring form. 
     */
    @Override
    public void backward() {
        Tensor xAdj = modIn.getOutputAdj();
        Tensor tmp = yAdj.copyAndConvertAlgebra(modIn.getAlgebra());
        xAdj.elemAdd(tmp);
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return Lists.getList(modIn);
    }
    
}
