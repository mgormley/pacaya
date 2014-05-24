package edu.jhu.autodiff2;

/**
 * Sum of all the entries in the tensor.
 * @author mgormley
 */
public class Prod implements Module<Tensor> {

    private Tensor y;
    private Tensor yAdj;
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
        Tensor tmp = yAdj.copy();
        tmp.multiply(y.getValue(0));
        tmp.elemDivide(x);
        xAdj.elemAdd(tmp);
    }

    @Override
    public Tensor getOutput() {
        return y;
    }

    @Override
    public Tensor getOutputAdj() {
        if (yAdj == null) {
            yAdj = y.copyAndFill(0);
        }
        return yAdj;
    }

}
