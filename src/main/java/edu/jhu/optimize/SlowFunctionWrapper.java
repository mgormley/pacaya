package edu.jhu.optimize;

import edu.jhu.util.Utilities;

public class SlowFunctionWrapper implements Function {

    private SlowFunction fn;
    private double[] point;    
    
    public SlowFunctionWrapper(SlowFunction fn) {
        this.fn = fn;
    }

    @Override
    public void setPoint(double[] point) {
        this.point = point;
    }

    @Override
    public double getValue() {
        return fn.getValue(point);
    }

    @Override
    public void getGradient(double[] gradient) {
        double[] tmp = fn.getGradientAtPoint(point);
        Utilities.copy(tmp, gradient);
    }

    @Override
    public int getNumDimensions() {
        return fn.getNumDimensions();
    }

}
