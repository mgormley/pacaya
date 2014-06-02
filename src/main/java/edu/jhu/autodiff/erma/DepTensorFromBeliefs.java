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
public class DepTensorFromBeliefs extends AbstractTensorModule implements Module<Tensor> {

    private Module<Beliefs> inf;
    private int n = -1;
    
    public DepTensorFromBeliefs(Module<Beliefs> inf) {
        super(inf.getAlgebra());
        this.inf = inf;
    }
    
    @Override
    public Tensor forward() {
        Beliefs b = inf.getOutput();
        n = guessNumWords(b);
        y = new Tensor(s, n, n);
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
                
                // Add the adjoint of p(e_{p,c} = True).
                bAdj.varBeliefs[v].addValue(LinkVar.TRUE, yAdj.get(pp, c));
            }
        }
    }

    @Override
    public List<? extends Object> getInputs() {
        return Lists.getList(inf);
    }    

    /** Gets the maximum index of a parent or child in a LinkVar, plus one. */ 
    static int guessNumWords(Beliefs b) {
        int n = -1;
        for (int v=0; v<b.varBeliefs.length; v++) {
            Var var = b.varBeliefs[v].getVars().get(0);
            if (var instanceof LinkVar) {
                LinkVar link = (LinkVar) var;
                int p = link.getParent();
                int c = link.getChild();
                n = Math.max(n, Math.max(p, c));
            }
        }
        return n+1;
    }

}
