package edu.jhu.autodiff;

import java.util.List;

import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.collections.Lists;

/**
 * Combines two tensors into a single larger tensor by adding an additional dimension of size two.
 * 
 * @author mgormley
 */
public class Combine extends AbstractTensorModule implements Module<Tensor> {

    // Option 1.
    private Module<Pair<Tensor, Tensor>> modIn;
    // Option 2.
    private Module<Tensor> mod1;
    private Module<Tensor> mod2;

    public Combine(Module<Pair<Tensor, Tensor>> modIn) {
        this.modIn = modIn;
    }

    public Combine(Module<Tensor> mod1, Module<Tensor> mod2) {
        this.mod1 = mod1;
        this.mod2 = mod2;
    }

    @Override
    public Tensor forward() {
        Tensor t1, t2;
        if (modIn != null) {
            // Option 1.
            Pair<Tensor, Tensor> pair = modIn.getOutput();
            t1 = pair.get1();
            t2 = pair.get2();
        } else {
            // Option 2.
            t1 = mod1.getOutput();
            t2 = mod2.getOutput();
        }
        return y = Tensor.combine(t1, t2);
    }

    @Override
    public void backward() {
        Tensor t1Adj, t2Adj;
        if (modIn != null) {
            // Option 1.
            Pair<Tensor, Tensor> pair = modIn.getOutputAdj();
            t1Adj = pair.get1();
            t2Adj = pair.get2();
        } else {
            // Option 2.
            t1Adj = mod1.getOutputAdj();
            t2Adj = mod2.getOutputAdj();
        }
        t1Adj.elemAdd(yAdj.select(0, 0));
        t2Adj.elemAdd(yAdj.select(0, 1));
    }

    @Override
    public List<? extends Object> getInputs() {
        if (modIn != null) { 
            // Option 1.
            return Lists.getList(modIn);
        } else {
            // Option 2.
            return Lists.getList(mod1, mod2);
        }
    }

}