package edu.jhu.gm.train;

import org.apache.commons.lang.mutable.MutableDouble;

import edu.jhu.gm.model.FgModel;

public class MutableValueGradient {

    private MutableDouble value;
    private FgModel gradient;
    
    public MutableValueGradient(MutableDouble value, FgModel gradient) {
        this.value = value;
        this.gradient = gradient;
    }
    
    public boolean hasValue() {
        return value != null;
    }
    
    public boolean hasGradient() {
        return gradient != null;
    }

    public FgModel getGradient() {
        return gradient;
    }
    
    public double getValue() {
        return value.doubleValue();
    }
    
    public void setValue(MutableDouble value) {
        this.value = value;
    }

    public void setGradient(FgModel gradient) {
        this.gradient = gradient;
    }

    public void addValue(double ll) {
        this.value.add(ll);
    }
    
    public void addGradient(FgModel gradient) {
        this.gradient.add(gradient);
    }

}