package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.AbstractModule;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor.LinkVar;
import edu.jhu.parse.dep.EdgeScores;
import edu.jhu.util.collections.Lists;

/**
 * Conversion from Beliefs to a Tensor representation of the edge marginals.
 * 
 * @author mgormley
 */
public class DepTensorToBeliefs extends AbstractModule<Beliefs> implements Module<Beliefs> {

    private Module<Tensor> depIn;
    private Module<Beliefs> inf;
    
    public DepTensorToBeliefs(Module<Tensor> dep, Module<Beliefs> inf) {
        super(dep.getAlgebra());
        this.depIn = dep;
        this.inf = inf;
    }
    
    @Override
    public Beliefs forward() {
        Tensor dep = depIn.getOutput();
        int n = dep.getDims()[1];
        Beliefs origB = inf.getOutput();
        y = new Beliefs(s);
        y.varBeliefs = new VarTensor[origB.varBeliefs.length];
        y.facBeliefs = null;
        
        for (int v=0; v<origB.varBeliefs.length; v++) {
            Var var = origB.varBeliefs[v].getVars().get(0);
            if (var instanceof LinkVar) {
                LinkVar link = (LinkVar) var;
                int p = link.getParent();
                int c = link.getChild();
                int pp = EdgeScores.getTensorParent(p, c);
                assert p < n && c < n;
                
                // Initialize the belief tensor.
                y.varBeliefs[v] = new VarTensor(s, origB.varBeliefs[v].getVars(), 0.0);
                // Set the marginal p(e_{p,c} = True) and p(e_{p,c} = False).
                y.varBeliefs[v].setValue(LinkVar.TRUE, dep.get(pp, c));
                y.varBeliefs[v].setValue(LinkVar.FALSE, 1.0 - dep.get(pp, c));
            }
        }
        return y;
    }

    @Override
    public void backward() {
        Tensor depAdj = depIn.getOutputAdj();
        int n = depAdj.getDims()[1];
        for (int v=0; v<yAdj.varBeliefs.length; v++) {
            if (yAdj.varBeliefs[v] != null) {
                Var var = y.varBeliefs[v].getVars().get(0);
                if (var instanceof LinkVar) {
                    LinkVar link = (LinkVar) var;
                    int p = link.getParent();
                    int c = link.getChild();
                    int pp = EdgeScores.getTensorParent(p, c);
                    assert p < n && c < n;
                    
                    // Add the adjoint of p(e_{p,c} = True).
                    depAdj.add(yAdj.varBeliefs[v].getValue(LinkVar.TRUE), pp, c);
                }
            }
        }
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return Lists.getList(depIn);
    }    

}
