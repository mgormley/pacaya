package edu.jhu.pacaya.autodiff.tensor;

import java.util.List;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.util.collections.Lists;

/**
 * Takes the log of each entry.
 * 
 * @author mgormley
 */
public class Log extends AbstractModule<Tensor> implements Module<Tensor> {

    private Module<Tensor> modInX;
    
    public Log(Module<Tensor> modInX) {        
        super(modInX.getAlgebra());
        this.modInX = modInX;
    }
    
    /** Foward pass: y_i = log(x_i) */
    @Override
    public Tensor forward() {
        Tensor x = modInX.getOutput();
        y = new Tensor(x); // copy
        y.log();
        return y;
    }

    /** 
     * Backward pass: 
     *    dG/dx_i += dG/dy_i dy_i/dx_i = dG/dy_i (1 / x_i)
     */
    @Override
    public void backward() {
        Tensor x = modInX.getOutput();
        Tensor tmp = new Tensor(yAdj); // copy
        tmp.elemDivide(x);
        modInX.getOutputAdj().elemAdd(tmp);
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return Lists.getList(modInX);
    }

}
