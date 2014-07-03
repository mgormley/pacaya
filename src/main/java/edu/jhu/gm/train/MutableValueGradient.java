package edu.jhu.gm.train;

import org.apache.commons.lang.mutable.MutableDouble;

import edu.jhu.gm.model.FgModel;

public class MutableValueGradient {

    private MutableDouble value;
    private FgModel gradient;
    private MutableDouble weight;
    
    public MutableValueGradient(MutableDouble value, FgModel gradient, MutableDouble weight) {
        this.value = value;
        this.gradient = gradient;
        this.weight = weight;
    }
   
    /* ------ Gradient ------ */
    
    public boolean hasGradient() {
        return gradient != null;
    }

    public FgModel getGradient() {
        return gradient;
    }
    
    public void setGradient(FgModel gradient) {
        this.gradient = gradient;
    }   
    
    public void addGradient(FgModel gradient) {
        this.gradient.add(gradient);
    }
    
    /* ------ Value ------ */
    
    public boolean hasValue() {
        return value != null;
    }
    
    public double getValue() {
        return value.doubleValue();
    }
    
    public void setValue(MutableDouble value) {
        this.value = value;
    }

    public void addValue(double value) {
        this.value.add(value);
    } 

    /* ------ Weight ------ */
    
    public boolean hasWeight() {
        return weight != null;
    }

    public double getWeight() {
        return weight.doubleValue();
    }
    
    public void setWeight(MutableDouble weight) {
        this.weight = weight;
    }
    
    public void addWeight(double weight) {
        this.weight.add(weight);
    }

}