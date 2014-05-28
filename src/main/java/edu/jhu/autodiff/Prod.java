package edu.jhu.autodiff;

import java.util.List;

import edu.jhu.util.collections.Lists;

/**
 * Sum of all the entries in the tensor.
 * @author mgormley
 */
public class Prod extends AbstractTensorModule implements Module<Tensor> {

    private Module<Tensor> modIn;
    
    public Prod(Module<Tensor> modIn) {
        this.modIn = modIn;
    }
    
    /** Foward pass: y = \prod_{i=1}^n x_i */
    @Override
    public Tensor forward() {
        Tensor x = modIn.getOutput();
        y = new Tensor(1);
        y.setValue(0, x.getProd());
        return y;
    }

    /** Backward pass: dG/dx_i += dG/dy dy/dx_i = dG/dy \prod_{j \neq i} x_j */
    @Override
    public void backward() {        
        // TODO: This is less numerically stable than the O(n^2) method of
        // multiplying \prod_{j=1}^{i-1} x_j \prod_{j+1}^n x_j  
        Tensor x = modIn.getOutput();
        Tensor xAdj = modIn.getOutputAdj();
        Tensor tmp = xAdj.copy();
        tmp.fill(yAdj.getValue(0));
        tmp.multiply(y.getValue(0));
        tmp.elemDivide(x);
        xAdj.elemAdd(tmp);
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return Lists.getList(modIn);
    }

}
