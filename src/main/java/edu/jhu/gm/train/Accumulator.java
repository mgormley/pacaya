package edu.jhu.gm.train;

import edu.jhu.gm.model.FgModel;

/** Struct for accumulating various values during optimization. */
public class Accumulator {

    // Values to accumulate.
    public double value = 0;
    public FgModel gradient = null;
    public double weight = 0;
    public double trainLoss = 0;
    public double devLoss = 0;

    // Flags indicating whether to accumulate each value.
    public boolean accumValue;
    public boolean accumGradient;
    public boolean accumWeight;
    public boolean accumTrainLoss;
    public boolean accumDevLoss;

    public Accumulator() { }
    
    public void addAll(Accumulator other) {
        this.value += other.value;
        this.weight += other.weight;
        this.trainLoss += other.trainLoss;
        this.devLoss += other.devLoss;
        if (this.gradient != null && other.gradient != null) {
            this.gradient.add(other.gradient);
        }
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