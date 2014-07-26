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
 * Conversion from a Tensor representation of the edge marginals back to Beliefs. The input beliefs
 * are used as a reference to determine the correct set of variables.
 * 
 * For each edge e_{p,c}, the dependency tensor gives the edge marginal as m_{p,c}.
 * Denote the beliefs by b(e_{p,c} = True) and b(e_{p,c} = False) for each edge.
 * 
 * We use the following abbreviations below:
 *    m_i := m_{p,c}
 *    t_i := b(e_{p,c} = True)
 *    f_i := b(e_{p,c} = False)
 *    
 * @author mgormley
 */
public class DepTensorToBeliefs extends AbstractModule<Beliefs> implements Module<Beliefs> {

    private Module<Tensor> depIn;
    private Module<Beliefs> inf;
    
    /**
     * Constructor.
     * @param dep The tensor representation of the edge marginals.
     * @param inf The reference beliefs used only to determine the correct set of variables.
     */
    public DepTensorToBeliefs(Module<Tensor> dep, Module<Beliefs> inf) {
        super(dep.getAlgebra());
        this.depIn = dep;
        this.inf = inf;
    }
    
    /** 
     * Forward pass:
     *    b(e_{p,c} = True) = d_{p,c}
     *    b(e_{p,c} = False) = 1.0 - d_{p,c}
     *    
     * Abbreviated:
     *    t_i = m_i
     *    f_i = 1.0 - m_i
     */
    @Override
    public Beliefs forward() {
        Tensor dep = depIn.getOutput();
        int n = dep.getDims()[1];
        Beliefs origB = inf.getOutput();
        y = new Beliefs(s);
        y.varBeliefs = new VarTensor[origB.varBeliefs.length];
        y.facBeliefs = new VarTensor[0];
        
        for (int v=0; v<origB.varBeliefs.length; v++) {
            Var var = origB.varBeliefs[v].getVars().get(0);
            if (var instanceof LinkVar) {
                LinkVar link = (LinkVar) var;
                int p = link.getParent();
                int c = link.getChild();
                int pp = EdgeScores.getTensorParent(p, c);
                assert p < n && c < n;
                
                // Initialize the belief tensor.
                y.varBeliefs[v] = new VarTensor(s, origB.varBeliefs[v].getVars(), s.zero());
                // Set the marginal b(e_{p,c} = True) and b(e_{p,c} = False).
                y.varBeliefs[v].setValue(LinkVar.TRUE, dep.get(pp, c));
                y.varBeliefs[v].setValue(LinkVar.FALSE, s.minus(s.one(), dep.get(pp, c)));
            }
        }
        return y;
    }

    /**
     * Backward pass:
     *     dG/dm_i += dG/dt_i*dt_i/dm_i + dG/df_i*df_i/dm_i
     *              = dG/dt_i - dG/df_i
     */
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
                    
                    // Add the adjoint of b(e_{p,c} = True).
                    depAdj.add(yAdj.varBeliefs[v].getValue(LinkVar.TRUE), pp, c);
                    // Add the adjoint of b(e_{p,c} = False)
                    depAdj.subtract(yAdj.varBeliefs[v].getValue(LinkVar.FALSE), pp, c);
                }
            }
        }
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        return Lists.getList(depIn);
    }    

}
