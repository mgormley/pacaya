package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TopoOrder;
import edu.jhu.autodiff.erma.ErmaObjective.DlFactory;
import edu.jhu.autodiff.tensor.ElemLinear;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.LogSignAlgebra;

/**
 * Softmax MBR decoder for dependency parsing evaluated with expected recall. 
 * 
 * @author mgormley
 */
public class DepParseDecodeLoss extends TopoOrder<Tensor> implements Module<Tensor> {
    
    /**
     * This factory defines the decoder / loss module as non-stationary: the softmax parameter on
     * the MBR decoder is annealed linearly from a starting temperature to a small epsilon.
     * 
     * Optionally, this loss function can be annealed from MSE to softmax MBR with expected recall.
     */
    public static class DepParseDecodeLossFactory implements DlFactory {
        public double startTemp = 10;
        public double endTemp = .1;
        public boolean annealMse = true;
        
        @Override
        public Module<Tensor> getDl(VarConfig goldConfig, ExpFamFactorsModule effm, Module<Beliefs> inf, int curIter, int maxIter) {
            double temperature = getTemperature(curIter, maxIter);
            
            if (annealMse) {
                double prop = (double) curIter / maxIter;
                
                Module<Tensor> mse = new MeanSquaredError(inf, goldConfig);
                Module<Tensor> dep = new DepParseDecodeLoss(inf, goldConfig, temperature);                
                Module<Tensor> lin = new ElemLinear(mse, dep, (1.0-prop), prop);
                return new TopoOrder<Tensor>(Lists.getList(inf), lin);
            } else {
                return new DepParseDecodeLoss(inf, goldConfig, temperature);
            }
        }

        public double getTemperature(int curIter, int maxIter) {
            double prop = (double) curIter / maxIter;
            double temp = (1.0 - prop) * startTemp + prop * endTemp;
            assert !Double.isNaN(temp);
            return temp;
        }
    }
        
    public DepParseDecodeLoss(Module<Beliefs> inf, VarConfig vc, double temperature) {
        super(Lists.getList(inf), build(inf, vc, temperature));
    }

    private static Module<Tensor> build(Module<Beliefs> inf, VarConfig goldConfig, double temperature) {
        // Decoding.
        DepTensorFromBeliefs b2d = new DepTensorFromBeliefs(inf);
        SoftmaxMbrDepParse mbr = new SoftmaxMbrDepParse(b2d, temperature, new LogSignAlgebra());
        DepTensorToBeliefs d2b = new DepTensorToBeliefs(mbr, inf);

        // Loss.
        VarSet predVars = VarSet.getVarsOfType(goldConfig.getVars(), VarType.PREDICTED);
        VarConfig predConfig = goldConfig.getSubset(predVars);
        ExpectedRecall er = new ExpectedRecall(d2b, predConfig);
        return er;
    }
    
}
