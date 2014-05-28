package edu.jhu.autodiff;

import java.util.List;

import edu.jhu.util.collections.Lists;

/**
 * Sum of all the entries in the tensor.
 * @author mgormley
 */
public class Sum extends AbstractTensorModule implements Module<Tensor> {

    private Module<Tensor> modIn;
    
    public Sum(Module<Tensor> modIn) {
        this.modIn = modIn;
    }
    
    /** Foward pass: y = \sum_{i=1}^n x_i */
    @Override
    public Tensor forward() {
        Tensor x = modIn.getOutput();
        y = new Tensor(1);
        y.setValue(0, x.getSum());
        return y;
    }

    /** Backward pass: dG/dx_i = dG/dy dy/dx_i = dG/dy */
    @Override
    public void backward() {
        Tensor xAdj = modIn.getOutputAdj();
        xAdj.add(yAdj.getValue(0));
    }

    public List<Module<Tensor>> getInputs() {
        return Lists.getList(modIn);
    }

}
