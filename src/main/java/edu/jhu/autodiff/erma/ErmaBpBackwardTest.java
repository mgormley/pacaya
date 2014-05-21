package edu.jhu.autodiff.erma;

import java.util.Arrays;

import org.apache.commons.math3.util.FastMath;
import org.junit.Test;

import edu.jhu.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.ExplicitFactor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraphTest;
import edu.jhu.gm.model.FactorGraphTest.FgAndVars;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.hlt.util.math.Vectors;
import edu.jhu.prim.Primitives;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.util.Prng;
import edu.jhu.util.dist.Gaussian;


public class ErmaBpBackwardTest {
	    
	@Test
	public void testGradientMatchesFiniteDifferences() {
	    
	    boolean logDomain = false;
        FgAndVars fgv = FactorGraphTest.getLinearChainFgWithVars(logDomain);
        FactorGraph fg = fgv.fg;
        
        VarConfig goldConfig = new VarConfig();
        goldConfig.put(fgv.w0, 0);
        goldConfig.put(fgv.w1, 1);
        goldConfig.put(fgv.w2, 0);
        goldConfig.put(fgv.t1, 1);
        goldConfig.put(fgv.t2, 1);
        
        Prng.seed(12345);
	    double[] theta0 = getTheta(fg);
	    Prng.seed(System.currentTimeMillis());
	    
	    // The gradient estimate
	    double[] grad = new double[theta0.length];
	    // The scaling term
	    double c = 0.00001;
        // The dimension
        double p = theta0.length;
        // The number of samples
        int numSamples = 1000;
	    for (int k=0; k<numSamples; k++) {	 
            // Get a random direction
            double[] e0 = getRandomBernoulliDirection(theta0.length);
            // Compute ( L(\theta + c * e) - L(\theta - c * e) ) / (2c)
            double scaler = 0;
            {
                // L(\theta + c * e)
                double[] e = DoubleArrays.copyOf(e0);
                DoubleArrays.scale(e, c);
                double[] theta = DoubleArrays.copyOf(theta0);
                DoubleArrays.add(theta, e);	        
                scaler += getExpectedRecall(theta, logDomain, fg, goldConfig);
            }
            {
                // - L(\theta - c * e)
                double[] e = DoubleArrays.copyOf(e0);
                DoubleArrays.scale(e, -c);
                double[] theta = DoubleArrays.copyOf(theta0);
                DoubleArrays.add(theta, e);         
                scaler -= getExpectedRecall(theta, logDomain, fg, goldConfig);
            }
            scaler /= 2.0 * c;
            for (int i=0; i<grad.length; i++) {
                grad[i] += scaler * 1.0 / e0[i];
            }
	    }
	    DoubleArrays.scale(grad, 1.0 / numSamples);
	    
	    System.out.println(Arrays.toString(grad));
	    
//	    er.backward(1);
//	    bp.backward(er.getVarBeliefsAdjs(), er.getFacBeliefsAdjs());
//	    
//	    DenseFactor[] potentialsAdj = bp.getPotentialsAdj();
//	    for (DenseFactor adj : potentialsAdj) {
//	        System.out.println(adj);
//	    }
	}

    private double getExpectedRecall(double[] theta, boolean logDomain, FactorGraph fg, VarConfig goldConfig) {
        // Update factors from the model in params array.
	    int i=0;
	    for (Factor f : fg.getFactors()) {
	        ExplicitFactor factor = (ExplicitFactor) f;
            for (int c=0; c<factor.size(); c++) {
                factor.setValue(c, theta[i++]);
            }
        }
	    
	    return getExpectedRecall(logDomain, fg, goldConfig);
	}

    private double[] getTheta(FactorGraph fg) {
        // Define the "model" as the explicit factor entries.
	    int numParams = 0;
	    for (Factor f : fg.getFactors()) {
	        numParams += f.getVars().calcNumConfigs();
	    }
	    // Randomly initialize the model.
	    double[] theta = new double[numParams];
	    for (int i=0; i<theta.length; i++) {
	        theta[i] = Math.abs(Gaussian.nextDouble(0.0, 1.0));
	    }
        return theta;
    }

    private double getExpectedRecall(boolean logDomain, FactorGraph fg, VarConfig goldConfig) {
        ErmaBpPrm prm = new ErmaBpPrm();
	    prm.updateOrder = BpUpdateOrder.SEQUENTIAL;
	    prm.schedule = BpScheduleType.TREE_LIKE;
	    prm.logDomain = logDomain;
	    prm.normalizeMessages = true;
	    ErmaBp bp = new ErmaBp(fg, prm);
	    bp.forward();
        
	    ExpectedRecall er = new ExpectedRecall(bp, goldConfig);
	    er.forward();
	    
	    return er.getExpectedRecall();
    }
    
    /* ----------- For random directions ------------ */
    // TODO: Move to separate class
    
    public static double[] getRandomBernoulliDirection(int p) {
        double[] e = new double[p];
        for (int i=0; i<e.length; i++) {
            // Bernoulli distribution chooses either positive or negative 1.
            e[i] = (Prng.nextBoolean()) ? 1 : -1;
        }
        return e;
    }

    public static double[] getRandomGaussianDirection(int p) {
        double[] e = new double[p];
        for (int i=0; i<e.length; i++) {
            e[i] = Gaussian.nextDouble(0, 1);
        }
        scaleL2NormToValue(e, p);
        return e;
    }

    public static void scaleL2NormToValue(double[] e, int desiredL2) {
        double curL2 = getSumOfSquares(e);
        DoubleArrays.scale(e, FastMath.sqrt(desiredL2 / curL2));
        assert Primitives.equals(getSumOfSquares(e), desiredL2, 1e-13); 
    }

    public static double getSumOfSquares(double[] e) {
        double curL2 = 0;
        for (int i=0; i<e.length; i++) {
            curL2 += e[i] * e[i];
        }
        return curL2;
    }
    
}
