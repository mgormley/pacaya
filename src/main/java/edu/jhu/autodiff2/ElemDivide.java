package edu.jhu.autodiff2;

/**
 * Elementwise division of the entries in two tensors of identical size.
 * 
 * @author mgormley
 */
public class ElemDivide implements Module<Tensor> {

    private Tensor y;
    private Tensor yAdj;
    private Module<Tensor> modInX;
    private Module<Tensor> modInW;
    
    public ElemDivide(Module<Tensor> modInX, Module<Tensor> modInW) {
        this.modInX = modInX;
        this.modInW = modInW;
    }
    
    /** Foward pass: y_i = x_i / w_i */
    @Override
    public Tensor forward() {
        Tensor x = modInX.getOutput();
        Tensor w = modInW.getOutput();
        y = x.copy();
        y.elemDivide(w);
        return y;
    }

    /** 
     * Backward pass: 
     *    dG/dx_i += dG/dy_i dy_i/dx_i = dG/dy_i / w_i 
     *    dG/dw_i += dG/dy_i dy_i/dw_i = dG/dy_i * x_i * (- w_i^2)
     */
    @Override
    public void backward() {
        Tensor x = modInX.getOutput();
        Tensor w = modInW.getOutput();
        {
            Tensor tmp = yAdj.copy();
            tmp.elemDivide(w);
            modInX.getOutputAdj().elemAdd(tmp);
        }
        {
            Tensor tmp = w.copy();
            tmp.elemMultiply(w);
            tmp.multiply(-1);
            tmp.elemMultiply(yAdj);
            tmp.elemMultiply(x);            
            modInW.getOutputAdj().elemAdd(tmp);
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
