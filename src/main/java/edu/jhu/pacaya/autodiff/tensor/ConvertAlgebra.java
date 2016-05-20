package edu.jhu.pacaya.autodiff.tensor;

import java.util.List;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.MVec;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.util.collections.QLists;
import edu.jhu.pacaya.util.semiring.Algebra;

/**
 * Converts from the abstract algebra of the input to a given abstract algebra.
 * @author mgormley
 */
public class ConvertAlgebra<T extends MVec> extends AbstractModule<T> implements Module<T> {

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
    @SuppressWarnings("unchecked")
    public T forward() {
        T x = modIn.getOutput();
        y = (T) x.copyAndConvertAlgebra(s);
        return y;
    }

    /** 
     * Backward pass: dG/dx_i = dG/dy_i. 
     * dG/dy_i is converted to a real from the semiring form. 
     */
    @Override
    @SuppressWarnings("unchecked")
    public void backward() {
        T xAdj = modIn.getOutputAdj();
        T tmp = (T) yAdj.copyAndConvertAlgebra(modIn.getAlgebra());
        xAdj.elemAdd(tmp);
    }

    @Override
    public List<Module<T>> getInputs() {
        return QLists.getList(modIn);
    }
    
}
