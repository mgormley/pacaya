package edu.jhu.gm.train;

import edu.jhu.gm.model.FgModel;

/** Struct for accumulating various values during optimization. */
public class Accumulator {

    // Values to accumulate.
    public double value = 0;
    public FgModel gradient = null;
    public double weight = 0;
    public double loss = 0;

    // Flags indicating whether to accumulate each value.
    public boolean accumValue;
    public boolean accumGradient;
    public boolean accumWeight;
    public boolean accumLoss;

    // For nonstationary functions:
    public int curIter = 0;
    public int maxIter = 0;
    
    public Accumulator() { }
    
    public void addAll(Accumulator other) {
        this.value += other.value;
        this.weight += other.weight;
        this.loss += other.loss;
        if (this.gradient != null && other.gradient != null) {
            this.gradient.add(other.gradient);
        }
    }

    public void setFlagsFromOther(Accumulator other) {
        this.accumValue = other.accumValue;
        this.accumGradient = other.accumGradient;
        this.accumWeight = other.accumWeight;
        this.accumLoss = other.accumLoss;
        
        this.curIter = other.curIter;
        this.maxIter = other.maxIter;
    }
    
    /* ------ Gradient ------ */
    
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
        
    public double getValue() {
        return value;
    }
    
    public void setValue(double value) {
        this.value = value;
    }

    public void addValue(double value) {
        this.value += value;
    } 

    /* ------ Weight ------ */
    
    public double getWeight() {
        return weight;
    }
    
    public void setWeight(double weight) {
        this.weight = weight;
    }
    
    public void addWeight(double weight) {
        this.weight += weight;
    }

}