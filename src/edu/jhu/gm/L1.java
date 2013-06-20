package edu.jhu.hltcoe.gm;

public class L1 implements Regularizer {

    @Override
    public double getValue(double[] params) {
        double sum = 0.0;
        for (int i=0; i<params.length; i++) {
            sum += Math.abs(params[i]);
        }
        return sum;
    }

    @Override
    public double[] getGradient(double[] params) {
        double[] gradient = new double[params.length];
        for (int j=0; j<gradient.length; j++) {
            if (params[j] < 0) {
                gradient[j] = -1;
            } else if (params[j] > 0) {
                gradient[j] = 1;
            } else {
                // TODO: should we just return 0 here?
                throw new RuntimeException("The derivative is undefined at zero.");
            }
        }
        return gradient;
    }

    @Override
    public int getNumDimensions() {
        // TODO Auto-generated method stub
        return 0;
    }

}
