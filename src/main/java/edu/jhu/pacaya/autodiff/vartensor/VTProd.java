package edu.jhu.pacaya.autodiff.vartensor;

import java.util.List;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.MVec;
import edu.jhu.pacaya.autodiff.MVecArray;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.util.collections.Lists;

class VTProd extends AbstractModule<VarTensor> implements Module<VarTensor> {

    private Module<MVecArray<VarTensor>> modIn;

    public VTProd(Module<MVecArray<VarTensor>> modIn) {
        super(modIn.getAlgebra());
        this.modIn = modIn;
    }

    /** Forward pass: p(y) = \prod_a \psi(y_a) where y_a is a {@link VarTensor} and we use variable-tensor product. */
    @Override
    public VarTensor forward() {
        MVecArray<VarTensor> xs = modIn.getOutput();
        y = new VarTensor(s, new VarSet(), s.one());
        for (int a=0; a<xs.dim(); a++) {
            VarTensor x = xs.get(a);
            y.prod(x);
        }
        return y;
    }

    /** Backward pass: dG/d\psi(y_a) += dG/dp(y) dp(y)/d\psi(y_a)
     * 
     * dp(y)/d\psi(y_a) = \prod_{b \neq a} \psi_b(y_b)
     */
    @Override
    public void backward() {
        MVecArray<VarTensor> xs = modIn.getOutput();
        MVecArray<VarTensor> xsAdj = modIn.getOutputAdj();
        for (int a=0; a<xsAdj.dim(); a++) {
            VarTensor xAdj = xsAdj.get(a);
            // Compute the product of all the input factors except for a.
            // We compute this cavity by brute force, rather than dividing out from the joint.
            VarTensor prod = new VarTensor(yAdj);
            for (int b=0; b<xsAdj.dim(); b++) {
                if (a == b) { continue; }
                prod.prod(xs.get(b));
            }
            xAdj.add(prod.getMarginal(xAdj.getVars(), false));
        }
    }

    @Override
    public List<? extends Module<? extends MVec>> getInputs() {
        return Lists.getList(modIn);
    }
    
}