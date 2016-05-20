package edu.jhu.pacaya.autodiff.tensor;

import java.util.List;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.util.collections.QLists;

/**
 * Combines two tensors into a single larger tensor by adding an additional dimension of size two.
 * 
 * @author mgormley
 */
public class Combine extends AbstractModule<Tensor> implements Module<Tensor> {

    private Module<Tensor> mod1;
    private Module<Tensor> mod2;

    public Combine(Module<Tensor> mod1, Module<Tensor> mod2) {
        super(mod1.getAlgebra());
        checkEqualAlgebras(mod1, mod2);
        this.mod1 = mod1;
        this.mod2 = mod2;
    }

    @Override
    public Tensor forward() {
        Tensor t1 = mod1.getOutput();
        Tensor t2 = mod2.getOutput();
        return y = Tensor.combine(t1, t2);
    }

    @Override
    public void backward() {
        Tensor t1Adj = mod1.getOutputAdj();
        Tensor t2Adj = mod2.getOutputAdj();
        t1Adj.elemAdd(yAdj.select(0, 0));
        t2Adj.elemAdd(yAdj.select(0, 1));
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return QLists.getList(mod1, mod2);
    }

}