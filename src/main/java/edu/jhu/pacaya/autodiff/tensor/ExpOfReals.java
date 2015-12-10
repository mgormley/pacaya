package edu.jhu.pacaya.autodiff.tensor;

import java.util.List;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.util.collections.QLists;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

/**
 * Takes the exp of each entry. Unlike {@link Exp}, this module assumes its input module is in the
 * reals, and converts to a new algebra.
 * 
 * @author mgormley
 */
// TODO: Test this.
public class ExpOfReals extends AbstractModule<VarTensor> implements Module<VarTensor> {

    private Module<VarTensor> modInX;
    
    public ExpOfReals(Module<VarTensor> modInX, Algebra s) {        
        super(s);
        if (!RealAlgebra.getInstance().equals(modInX.getAlgebra())) {
            throw new IllegalArgumentException("Input module's algebra must be " + RealAlgebra.class);
        }
        this.modInX = modInX;
    }
    
    /** Foward pass: y_i = exp(x_i) */
    @Override
    public VarTensor forward() {
        VarTensor x = modInX.getOutput();
        y = new VarTensor(s, x.getVars());
        for (int c = 0; c < y.size(); c++) {
            y.setValue(c, s.fromLogProb(x.getValue(c)));
        }
        return y;
    }

    /** 
     * Backward pass: 
     *    dG/dx_i += dG/dy_i dy_i/dx_i = dG/dy_i exp(x_i)
     */
    @Override
    public void backward() {
        VarTensor tmp = new VarTensor(yAdj); // copy
        tmp.elemMultiply(y);
        tmp = tmp.copyAndConvertAlgebra(RealAlgebra.getInstance());
        VarTensor xAdj = modInX.getOutputAdj();
        xAdj.elemAdd(tmp);
    }

    @Override
    public List<Module<VarTensor>> getInputs() {
        return QLists.getList(modInX);
    }

}
