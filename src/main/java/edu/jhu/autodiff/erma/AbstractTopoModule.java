package edu.jhu.autodiff.erma;

import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TopoOrder;
import edu.jhu.util.semiring.Algebra;

public abstract class AbstractTopoModule implements Module<Tensor> {

    protected TopoOrder topo;
    protected Algebra outS;

    public AbstractTopoModule(Algebra s) {
        this.topo = new TopoOrder();
        this.outS = s;
    }

    @Override
    public Tensor getOutput() {
        return topo.getOutput();
    }

    @Override
    public Tensor getOutputAdj() {
        return topo.getOutputAdj();
    }

    @Override
    public void zeroOutputAdj() {
        topo.zeroOutputAdj();
    }

    @Override
    public Algebra getAlgebra() {
        return outS;
    }

}