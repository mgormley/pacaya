package edu.jhu.autodiff.erma;

import java.util.List;

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
public class DepTensorToBeliefs extends AbstractBeliefsModule implements Module<Beliefs> {

    private Module<Tensor> depIn;
    private Module<Beliefs> inf;
    
    public DepTensorToBeliefs(Module<Tensor> dep, Module<Beliefs> inf) {
        this.depIn = dep;
        this.inf = inf;
    }
    
    @Override
    public Beliefs forward() {
        Tensor dep = depIn.getOutput();
        int n = dep.getDims()[1];
        Beliefs origB = inf.getOutput();
        b = new Beliefs();
        b.varBeliefs = new VarTensor[origB.varBeliefs.length];
        b.facBeliefs = null;
        
        for (int v=0; v<origB.varBeliefs.length; v++) {
            Var var = origB.varBeliefs[v].getVars().get(0);
            if (var instanceof LinkVar) {
                LinkVar link = (LinkVar) var;
                int p = link.getParent();
                int c = link.getChild();
                int pp = EdgeScores.getTensorParent(p, c);
                assert p < n && c < n;
                
                // Initialize the belief tensor.
                b.varBeliefs[v] = new VarTensor(origB.varBeliefs[v].getVars(), 0.0);
                // Set the marginal p(e_{p,c} = True) and p(e_{p,c} = False).
                b.varBeliefs[v].setValue(LinkVar.TRUE, dep.get(pp, c));
                b.varBeliefs[v].setValue(LinkVar.FALSE, 1.0 - dep.get(pp, c));
            }
        }
        return b;
    }

    @Override
    public void backward() {
        Tensor depAdj = depIn.getOutputAdj();
        int n = depAdj.getDims()[1];
        for (int v=0; v<bAdj.varBeliefs.length; v++) {
            if (bAdj.varBeliefs[v] != null) {
                Var var = b.varBeliefs[v].getVars().get(0);
                if (var instanceof LinkVar) {
                    LinkVar link = (LinkVar) var;
                    int p = link.getParent();
                    int c = link.getChild();
                    int pp = EdgeScores.getTensorParent(p, c);
                    assert p < n && c < n;
                    
                    // Add the adjoint of p(e_{p,c} = True).
                    depAdj.add(bAdj.varBeliefs[v].getValue(LinkVar.TRUE), pp, c);
                }
            }
        }
    }

    @Override
    public List<? extends Object> getInputs() {
        return Lists.getList(inf);
    }    

}
