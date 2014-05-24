package edu.jhu.autodiff2;


public class Tensor {

    /**
     * The dimensions of this tensor.
     * @param dimensions
     */
    public Tensor(int... dimensions) {
        
    }

    /** Copy constructor. */
    public Tensor(Tensor input) {
        
    }

    public double getValue(int idx) {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setValue(int idx, double val) {
        // TODO Auto-generated method stub
        
    }

    public void addValue(int idx, double val) {
        // TODO Auto-generated method stub
        
    }

    public void add(double val) {
        // TODO Auto-generated method stub
        
    }

    public void subtract(double val) {
        // TODO Auto-generated method stub
        
    }
    
    public void multiply(double val) {
        // TODO Auto-generated method stub
        
    }

    public void divide(double val) {
        // TODO Auto-generated method stub
        
    }

    public void fill(int val) {
        // TODO Auto-generated method stub
        
    }

    public void elemAdd(Tensor other) {
        if (this.size() != other.size()) {
            throw new IllegalStateException("Input tensors are not the same size");
        }
        // TODO Auto-generated method stub
        
    }

    public void elemSubtract(Tensor other) {
        // TODO Auto-generated method stub
        
    }

    public void elemMultiply(Tensor other) {
        // TODO Auto-generated method stub
        
    }

    public void elemDivide(Tensor other) {
        // TODO Auto-generated method stub
        
    }

    public Tensor copy() {
        // TODO Auto-generated method stub
        return null;
    }

    public double getSum() {
        // TODO Auto-generated method stub
        return 0;
    }

    public double getProd() {
        // TODO Auto-generated method stub
        return 0;
    }

    /** Gets the number of entries in the Tensor. */
    public int size() {
        // TODO Auto-generated method stub
        return 0;
    }

    public Tensor zeroedCopy() {
        // TODO Auto-generated method stub
        return null;
    }

    public Tensor copyAndFill(double val) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
