package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.AbstractTensorModule;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor.LinkVar;
import edu.jhu.parse.dep.EdgeScores;
import edu.jhu.util.collections.Lists;

/**
 * Conversion from Beliefs to a Tensor representation of the edge marginals.
 * 
 * @author mgormley
 */
public class SelectDepParseMarginals extends AbstractTensorModule implements Module<Tensor> {

    private Module<Beliefs> inf;
    private int n;
    
    public SelectDepParseMarginals(Module<Beliefs> inf, int n) {
        this.inf = inf;
        this.n = n;
    }
    
    @Override
    public Tensor forward() {
        y = new Tensor(n, n);
        Beliefs b = inf.getOutput();
        for (int v=0; v<b.varBeliefs.length; v++) {
            Var var = b.varBeliefs[v].getVars().get(0);
            if (var instanceof LinkVar) {
                LinkVar link = (LinkVar) var;
                int p = link.getParent();
                int c = link.getChild();
                int pp = EdgeScores.getTensorParent(p, c);
                assert p < n && c < n;
                
                // Set the marginal p(e_{p,c} = True).
                y.set(b.varBeliefs[v].getValue(LinkVar.TRUE), pp, c);
            }
        }
        return y;
    }

    @Override
    public void backward() {
        Beliefs bAdj = inf.getOutputAdj();
        for (int v=0; v<bAdj.varBeliefs.length; v++) {
            Var var = bAdj.varBeliefs[v].getVars().get(0);
            if (var instanceof LinkVar) {
                LinkVar link = (LinkVar) var;
                int p = link.getParent();
                int c = link.getChild();
                int pp = EdgeScores.getTensorParent(p, c);
                assert p < n && c < n;
                
                // Set the adjoint of p(e_{p,c} = True).
                bAdj.varBeliefs[v].addValue(LinkVar.TRUE, yAdj.get(pp, c));
            }
        }
    }

    @Override
    public List<? extends Object> getInputs() {
        return Lists.getList(inf);
    }    

}
