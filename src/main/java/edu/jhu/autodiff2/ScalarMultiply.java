package edu.jhu.autodiff2;

/**
 * Multiplication of each entry in a tensor by a scalar from another tensor.
 * 
 * @author mgormley
 */
public class ScalarMultiply implements Module<Tensor> {

    private Tensor y;
    private Tensor yAdj;
    private Module<Tensor> modInX;
    private Module<Tensor> modInW;
    // The index in w, which should be multiplied each x entry.
    private int k;
    
    public ScalarMultiply(Module<Tensor> modInX, Module<Tensor> modInW, int k) {
        this.modInX = modInX;
        this.modInW = modInW;
        this.k = k;
    }
    
    /** Foward pass: y_i = x_i * w_k */
    @Override
    public Tensor forward() {
        Tensor x = modInX.getOutput();
        double w_k = modInW.getOutput().getValue(k);
        y = x.copy();
        y.multiply(w_k);
        return y;
    }

    /** 
     * Backward pass: 
     *    dG/dx_i += dG/dy_i dy_i/dx_i = dG/dy_i w_k
     *    dG/dw_k += \sum_{i=1}^n dG/dy_i dy_i/dw_k = \sum_{i=1}^n dG/dy_i x_i
     */
    @Override
    public void backward() {
        Tensor x = modInX.getOutput();
        double w_k = modInW.getOutput().getValue(k);
        {
            Tensor tmp = yAdj.copy();
            tmp.multiply(w_k);
            modInX.getOutputAdj().elemAdd(tmp);
        }
        {
            Tensor tmp = yAdj.copy();
            tmp.elemMultiply(x);
            modInW.getOutputAdj().addValue(k, tmp.getSum());
        }
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
