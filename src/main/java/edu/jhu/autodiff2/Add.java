package edu.jhu.autodiff2;

/**
 * Addition of each entry in a tensor by a scalar from another tensor.
 * 
 * @author mgormley
 */
public class Add implements Module<Tensor> {

    private Tensor y;
    private Tensor yAdj;
    private Module<Tensor> modInX;
    private Module<Tensor> modInW;
    // The index in w, which should be multiplied each x entry.
    private int k;
    
    public Add(Module<Tensor> modInX, Module<Tensor> modInW, int k) {
        this.modInX = modInX;
        this.modInW = modInW;
        this.k = k;
    }
    
    /** Foward pass: y_i = x_i + w_k */
    @Override
    public Tensor forward() {
        Tensor x = modInX.getOutput();
        double w_k = modInW.getOutput().getValue(k);
        y = x.copy();
        y.add(w_k);
        return y;
    }

    /** 
     * Backward pass: 
     *    dG/dx_i += dG/dy_i dy_i/dx_i = dG/dy_i
     *    dG/dw_k += \sum_{i=1}^n dG/dy_i dy_i/dw_k = \sum_{i=1}^n dG/dy_i
     */
    @Override
    public void backward() {
        modInX.getOutputAdj().elemAdd(yAdj);
        modInW.getOutputAdj().addValue(k, yAdj.getSum());
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
