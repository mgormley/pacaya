package edu.jhu.autodiff.tensor;

import java.util.List;

import edu.jhu.autodiff.AbstractModule;
import edu.jhu.autodiff.MVec;
import edu.jhu.autodiff.Module;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;

/**
 * Converts from the abstract algebra of the input to a given abstract algebra.
 * @author mgormley
 */
public class ConvertAlgebra<T extends MVec<T>> extends AbstractModule<T> implements Module<T> {

    private Module<T> modIn;
    
    public ConvertAlgebra(Module<T> modIn, Algebra s) {
        super(s);
        this.modIn = modIn;
    }
    
    /** 
     * Foward pass: y[i] = x[i]. 
     * x[i] is converted from a real to its semiring form.
     */
    @Override
    public T forward() {
        T x = modIn.getOutput();
        y = x.copyAndConvertAlgebra(s);
        return y;
    }

    /** 
     * Backward pass: dG/dx_i = dG/dy_i. 
     * dG/dy_i is converted to a real from the semiring form. 
     */
    @Override
    public void backward() {
        T xAdj = modIn.getOutputAdj();
        T tmp = yAdj.copyAndConvertAlgebra(modIn.getAlgebra());
        xAdj.elemAdd(tmp);
    }

    @Override
    public List<Module<T>> getInputs() {
        return Lists.getList(modIn);
    }
    
}
