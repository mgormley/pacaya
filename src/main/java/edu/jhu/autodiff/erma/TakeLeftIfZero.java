package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.AbstractModule;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.util.collections.Lists;

/**
 * Element-wise operation which takes the left side if the mark is zero, and the right side
 * otherwise.
 * 
 * NOTE: This module abuses the standard design since no information is back-propagated to the mark.
 * 
 * @author mgormley
 */
public class TakeLeftIfZero extends AbstractModule<Tensor> implements Module<Tensor> {
    
    Module<Tensor> leftIn; 
    Module<Tensor> rightIn; 
    Module<Tensor> markIn;
    
    public TakeLeftIfZero(Module<Tensor> left, Module<Tensor> right, Module<Tensor> mark) {
        super(left.getAlgebra());
        checkEqualAlgebras(left, right, mark);
        this.leftIn = left;
        this.rightIn = right;
        this.markIn = mark;
    }

    @Override
    public Tensor forward() {
        Tensor mark = markIn.getOutput();
        Tensor left = leftIn.getOutput();
        Tensor right = rightIn.getOutput();
        Tensor.checkEqualSize(left, right);
        y = new Tensor(left); // copy
        left.fill(s.zero());
        for (int c=0; c<y.size(); c++) {
            Tensor t = (mark.getValue(c) == s.zero()) ? left : right;
            y.setValue(c, t.getValue(c));
        }
        return y;
    }

    @Override
    public void backward() {
        Tensor mark = markIn.getOutput();
        Tensor leftAdj = leftIn.getOutputAdj();
        Tensor rightAdj = rightIn.getOutputAdj();
        for (int c=0; c<yAdj.size(); c++) {
            Tensor t = (mark.getValue(c) == s.zero()) ? leftAdj : rightAdj;
            t.addValue(c, yAdj.getValue(c));
        }
        // Get the mark's adjoint, but don't add to it. 
        // TODO: The mark should really be a constant tensor.
        markIn.getOutputAdj();
    }

    @Override
    public List<Module<Tensor>> getInputs() {
        // The mark is excluded from the inputs list since nothing is backpropagated to it.
        return Lists.getList(leftIn, rightIn);
    }
    
}