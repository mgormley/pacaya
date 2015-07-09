package edu.jhu.pacaya.gm.train;

import edu.jhu.pacaya.autodiff.Identity;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.TopoOrder;
import edu.jhu.pacaya.autodiff.tensor.ElemLinear;
import edu.jhu.pacaya.gm.decode.SoftmaxMbrDepParse;
import edu.jhu.pacaya.gm.inf.Beliefs;
import edu.jhu.pacaya.gm.model.FactorsModule;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.util.collections.QLists;
import edu.jhu.pacaya.util.semiring.LogSignAlgebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.util.math.FastMath;

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
        public boolean useLogScale = true;
        
        @Override
        public Module<Tensor> getDl(VarConfig goldConfig, FactorsModule effm, Module<Beliefs> inf, int curIter, int maxIter) {
            double temperature = getTemperature(curIter, maxIter);
            Identity<Tensor> temp = new Identity<Tensor>(Tensor.getScalarTensor(RealAlgebra.getInstance(), temperature)); 
                    
            if (annealMse) {
                double prop = (double) curIter / maxIter;
                
                Module<Tensor> mse = new L2Distance(inf, goldConfig);
                Module<Tensor> dep = new DepParseDecodeLoss(inf, goldConfig, temp);                
                Module<Tensor> lin = new ElemLinear(mse, dep, (1.0-prop), prop);
                return new TopoOrder<Tensor>(QLists.getList(inf, temp), lin);
            } else {
                return new DepParseDecodeLoss(inf, goldConfig, temp);
            }
        }

        /** Computes the temperature using a linear annealing schedule. */
        public double getTemperature(int curIter, int maxIter) {
            double prop = (double) curIter / maxIter;
            double temp;
            if (useLogScale) {
                // Use log scale.
                double startLog = FastMath.log(startTemp);
                double endLog = FastMath.log(endTemp);
                double tempLog = ((1.0 - prop) * startLog) + (prop * endLog);
                temp = Math.exp(tempLog);
            } else {
                // Use linear scale.
                temp = ((1.0 - prop) * startTemp) + (prop * endTemp);
            }
            assert !Double.isNaN(temp);
            return temp;
        }
    }
        
    public DepParseDecodeLoss(Module<Beliefs> inf, VarConfig vc, Module<Tensor> temperature) {
        super();
        shallowCopy(build(inf, vc, temperature));
    }

    private static TopoOrder<Tensor> build(Module<Beliefs> inf, VarConfig goldConfig, Module<Tensor> temperature) {
        // Decoding.
        DepTensorFromBeliefs b2d = new DepTensorFromBeliefs(inf);
        SoftmaxMbrDepParse mbr = new SoftmaxMbrDepParse(b2d, temperature, LogSignAlgebra.getInstance());
        DepTensorToBeliefs d2b = new DepTensorToBeliefs(mbr, inf);

        // Loss.
        VarSet predVars = VarSet.getVarsOfType(goldConfig.getVars(), VarType.PREDICTED);
        VarConfig predConfig = goldConfig.getSubset(predVars);
        ExpectedRecall er = new ExpectedRecall(d2b, predConfig);
        return new TopoOrder<Tensor>(QLists.getList(inf, temperature), er);
    }
    
}
