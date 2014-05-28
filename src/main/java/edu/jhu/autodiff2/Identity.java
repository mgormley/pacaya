package edu.jhu.autodiff2;

/**
 * This module is simply the identity function. 
 * @author mgormley
 */
public class Identity implements Module<Tensor> {

    private Tensor y;
    private Tensor yAdj;
    
    public Identity(Tensor y) {
        this.y = y;
    }
    
    @Override
    public Tensor forward() {
        // No-op.
        return y;
    }

    @Override
    public void backward() {
        // No-op.
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
