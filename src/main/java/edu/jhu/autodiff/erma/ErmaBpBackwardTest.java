package edu.jhu.autodiff.erma;

import java.util.Arrays;

import org.apache.commons.math3.util.FastMath;
import org.junit.Test;

import edu.jhu.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.model.ExplicitFactor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraphTest;
import edu.jhu.gm.model.FactorGraphTest.FgAndVars;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.hlt.optimize.function.DifferentiableFunction;
import edu.jhu.hlt.optimize.function.ValueGradient;
import edu.jhu.prim.Primitives;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.prim.vector.IntDoubleVector;
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
	    IntDoubleVector theta0 = new IntDoubleDenseVector();
	    int numParams = getTheta(fg, theta0);
        Prng.seed(System.currentTimeMillis());
	    
	    // The gradient estimate
	    IntDoubleVector grad = new IntDoubleDenseVector(numParams);
	    // The scaling term
	    double c = 0.00001;
        // The dimension
        double p = numParams;
        // The number of samples
        int numSamples = 1000;
	    for (int k=0; k<numSamples; k++) {	 
            // Get a random direction
            IntDoubleVector e0 = getRandomBernoulliDirection(numParams);
            // Compute ( L(\theta + c * e) - L(\theta - c * e) ) / (2c)
            double scaler = 0;
            {
                // L(\theta + c * e)
                IntDoubleVector e = e0.copy();
                e.scale(c);
                IntDoubleVector theta = theta0.copy();
                theta.add(e);
                scaler += getExpectedRecall(theta, logDomain, fg, goldConfig);
            }
            {
                // - L(\theta - c * e)
                IntDoubleVector e = e0.copy();
                e.scale(-c);
                IntDoubleVector theta = theta0.copy();
                theta.add(e);        
                scaler -= getExpectedRecall(theta, logDomain, fg, goldConfig);
            }
            scaler /= 2.0 * c;
            for (int i=0; i< numParams; i++) {
                grad.add(i, scaler * 1.0 / e0.get(i));
            }
	    }
	    grad.scale(1.0 / numSamples);
	    
	    System.out.println(grad);
	    
//	    er.backward(1);
//	    bp.backward(er.getVarBeliefsAdjs(), er.getFacBeliefsAdjs());
//	    
//	    DenseFactor[] potentialsAdj = bp.getPotentialsAdj();
//	    for (DenseFactor adj : potentialsAdj) {
//	        System.out.println(adj);
//	    }
	}

    private int getTheta(FactorGraph fg, IntDoubleVector theta) {
        // Define the "model" as the explicit factor entries.
        int numParams = 0;
        for (Factor f : fg.getFactors()) {
            numParams += f.getVars().calcNumConfigs();
        }
        // Randomly initialize the model.
        for (int i=0; i< numParams; i++) {
            theta.set(i, Math.abs(Gaussian.nextDouble(0.0, 1.0)));
        }
        return numParams;
    }


    private double getExpectedRecall(IntDoubleVector theta, boolean logDomain, FactorGraph fg, VarConfig goldConfig) {
        // Update factors from the model in params array.
	    int i=0;
	    for (Factor f : fg.getFactors()) {
	        ExplicitFactor factor = (ExplicitFactor) f;
            for (int c=0; c<factor.size(); c++) {
                factor.setValue(c, theta.get(i++));
            }
        }
	    
	    return getExpectedRecall(logDomain, fg, goldConfig);
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
    
    public static IntDoubleVector getRandomBernoulliDirection(int p) {
        IntDoubleVector e = new IntDoubleDenseVector(p);
        for (int i=0; i<p; i++) {
            // Bernoulli distribution chooses either positive or negative 1.
            e.set(i, (Prng.nextBoolean()) ? 1 : -1);
        }
        return e;
    }

    public static IntDoubleVector getRandomGaussianDirection(int p) {
        IntDoubleVector e = new IntDoubleDenseVector(p);
        for (int i=0; i<p; i++) {
            e.set(i, Gaussian.nextDouble(0, 1));
        }
        scaleL2NormToValue(e, p);
        return e;
    }

    public static void scaleL2NormToValue(IntDoubleVector e, int desiredL2) {
        double curL2 = getSumOfSquares(e);
        e.scale(FastMath.sqrt(desiredL2 / curL2));
        assert Primitives.equals(getSumOfSquares(e), desiredL2, 1e-13); 
    }

    public static double getSumOfSquares(IntDoubleVector e) {
        double curL2 = 0;
        for (int i=0; i<e.getNumImplicitEntries(); i++) {
            curL2 += e.get(i) * e.get(i);
        }
        return curL2;
    }
    
}
