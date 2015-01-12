package edu.jhu.autodiff.tensor;

import java.util.List;

import edu.jhu.autodiff.AbstractModule;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.util.collections.Lists;

/**
 * Sum of all the entries in the tensor.
 * @author mgormley
 */
public class Sum extends AbstractModule<Tensor> implements Module<Tensor> {

    private Module<Tensor> modIn;
    
    public Sum(Module<Tensor> modIn) {
        super(modIn.getAlgebra());
        checkEqualAlgebras(this, modIn);
        this.modIn = modIn;
    }
    
    /** Foward pass: y = \sum_{i=1}^n x_i */
    @Override
    public Tensor forward() {
        Tensor x = modIn.getOutput();
        y = new Tensor(s, 1);
        y.setValue(0, x.getSum());
        return y;
    }

    /** Backward pass: dG/dx_i = dG/dy dy/dx_i = dG/dy */
    @Override
    public void backward() {
        Tensor xAdj = modIn.getOutputAdj();
        xAdj.add(yAdj.getValue(0));
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return Lists.getList(modIn);
    }

}
