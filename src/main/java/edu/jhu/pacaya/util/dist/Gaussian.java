package edu.jhu.pacaya.util.dist;

import org.uncommons.maths.random.GaussianGenerator;

import edu.jhu.prim.util.random.Prng;

public class Gaussian {

	private GaussianGenerator normal;

	public Gaussian(double mean, double standardDeviation) {
	    this.normal = new GaussianGenerator(mean, standardDeviation, Prng.getRandom());
	}
	
	public double nextDouble() {
		return normal.nextValue();
	}

	public void nextDoubleArray(double[] outArray) {
	    for (int i=0; i<outArray.length; i++) {
	        outArray[i] = normal.nextValue();
	    }
    }

	public static double nextDouble(double mean, double standardDeviation) {
		Gaussian gaussian = new Gaussian(mean, standardDeviation);
		return gaussian.nextDouble();
	}
	
    public static void nextDoubleArray(double mean, double standardDeviation, double[] outArray) {
        Gaussian gaussian = new Gaussian(mean, standardDeviation);
        gaussian.nextDoubleArray(outArray);
    }
    
}
