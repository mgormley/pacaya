package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.AbstractTensorModule;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TopoOrder;
import edu.jhu.autodiff.erma.ErmaObjective.DlFactory;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.LogPosNegAlgebra;

/**
 * Softmax MBR decoder for dependency parsing evaluated with expected recall. 
 * 
 * @author mgormley
 */
public class DepParseDecodeLoss extends AbstractTopoModule implements Module<Tensor> {
    
    /**
     * This factory defines the decoder / loss module as non-stationary: the softmax parameter on
     * the MBR decoder is annealed linearly from a starting temperature to a small epsilon.
     */
    public static class DepParseDecodeLossFactory implements DlFactory {
        public double startTemp = 100;
        public double endTemp = .1;
        
        @Override
        public Module<Tensor> getDl(FactorGraph fg, VarConfig goldConfig, Module<Beliefs> inf, int curIter, int maxIter) {
            double temperature = getTemperature(curIter, maxIter);
            return new DepParseDecodeLoss(inf, goldConfig, temperature);
        }

        public double getTemperature(int curIter, int maxIter) {
            double prop = (double) curIter / maxIter;
            return (1.0 - prop) * startTemp + prop * endTemp;
        }
    }
    
    private Module<Beliefs> inf;
    private VarConfig goldConfig;
    private double temperature;
    
    public DepParseDecodeLoss(Module<Beliefs> inf, VarConfig vc, double temperature) {
        super(inf.getAlgebra());
        this.inf = inf;
        this.goldConfig = vc;
        this.temperature = temperature;
        build();
    }

    private void build() {
        // Decoding.
        DepTensorFromBeliefs b2d = new DepTensorFromBeliefs(inf);
        topo.add(b2d);
        SoftmaxMbrDepParse mbr = new SoftmaxMbrDepParse(b2d, temperature, new LogPosNegAlgebra());
        topo.add(mbr);
        DepTensorToBeliefs d2b = new DepTensorToBeliefs(mbr, inf);
        topo.add(d2b);

        // Loss.
        VarSet predVars = VarSet.getVarsOfType(goldConfig.getVars(), VarType.PREDICTED);
        VarConfig predConfig = goldConfig.getSubset(predVars);
        ExpectedRecall er = new ExpectedRecall(d2b, predConfig);
        topo.add(er);
    }
    
    public Tensor forward() {        
        return topo.forward();
    }
    
    public void backward() {
        topo.backward();
    }
    
    @Override
    public List<Module<Beliefs>> getInputs() {
        return Lists.getList(inf);
    }
    
}
