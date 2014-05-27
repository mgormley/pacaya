package edu.jhu.autodiff2;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.util.JUnitUtils;


public class TensorTest {

    @Test
    public void testValueOperations() {
        Tensor f1 = new Tensor(2);
        f1.setValue(0, 0);
        f1.setValue(1, 1);
        
        // set, add, scale, get
        f1.fill(2);
        JUnitUtils.assertArrayEquals(new double[]{2, 2}, f1.getValues(), 1e-13);
        
        f1.setValue(0, 1);
        f1.add(2);
        JUnitUtils.assertArrayEquals(new double[]{3, 4}, f1.getValues(), 1e-13);
        
        f1.multiply(0.5);
        JUnitUtils.assertArrayEquals(new double[]{1.5, 2}, f1.getValues(), 1e-13);
        
        assertEquals(1.5, f1.getValue(0), 1e-13);
        assertEquals(2.0, f1.getValue(1), 1e-13);        
    }

    @Test
    public void testFactorAddIdentical() {   
        // Test where vars1 is identical to vars2.
        Tensor f1 = new Tensor(2, 3);
        f1.fill(1);
        f1.setValue(2, 2);
        f1.setValue(3, 3);
        
        Tensor f2 = new Tensor(2, 3);
        f2.fill(2);
        f2.setValue(2, 5);
        f2.setValue(5, 7);
        
        // values=[1.0, 1.0, 2.0, 3.0, 1.0, 1.0]
        System.out.println("f1: " + f1);
        // values=[2.0, 2.0, 5.0, 2.0, 2.0, 7.0]
        System.out.println("f2: " + f2);
        
        f1.elemAdd(f2);                
        System.out.println("f1+f2:" + f1);
        
        JUnitUtils.assertArrayEquals(new double[]{3.0, 3.0, 7.0, 5.0, 3.0, 8.0}, f1.getValues(), 1e-13);                
    }

}
