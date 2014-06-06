package edu.jhu.autodiff;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.util.JUnitUtils;
import edu.jhu.util.Timer;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.RealAlgebra;


public class TensorTest {

    private Algebra s = new RealAlgebra();
    
    @Test
    public void testGetSetAddWithIndices() {
        Tensor t1 = new Tensor(s, 2,3,5);
        // Test set.
        double val;
        val = 0;
        for (int i=0; i<2; i++) {
            for (int j=0; j<3; j++) {
                for (int k=0; k<5; k++) {
                    assertEquals(0, t1.set(val++, i,j,k), 1e-13);            
                }
            }
        }
        // Test set returns previous.
        val = 0;
        for (int i=0; i<2; i++) {
            for (int j=0; j<3; j++) {
                for (int k=0; k<5; k++) {
                    assertEquals(val, t1.set(val, i,j,k), 1e-13);
                    val++;
                }
            }
        }
        // Test get.
        val = 0;
        for (int i=0; i<2; i++) {
            for (int j=0; j<3; j++) {
                for (int k=0; k<5; k++) {
                    assertEquals(val++, t1.get(i,j,k), 1e-13);            
                }
            }
        }

        // Test add.
        val = 0;
        for (int i=0; i<2; i++) {
            for (int j=0; j<3; j++) {
                for (int k=0; k<5; k++) {
                    assertEquals(val, t1.add(val, i,j,k), 1e-13);
                    assertEquals(val*2, t1.get(i,j,k), 1e-13);            
                    val++;
                }
            }
        }
    }
    
    @Test
    public void testSpeedOfIndexOperations() {
        Tensor[] arr = new Tensor[1000];
        int[] dims = new int[]{31, 5, 7, 11};
        for (int i=0; i<arr.length; i++) {
            arr[i] = new Tensor(s, dims);
        }
        {
            Timer timer = new Timer();
            timer.start();
            for (int i=0; i<arr.length; i++) {
                Tensor tensor = arr[i];                
                for (int c=0; c < tensor.size(); c++) {
                    tensor.addValue(c, c);
                }
            }
            timer.stop();
            System.out.println("tot(ms) direct iteration: " + timer.totMs());
        }
        {
            Timer timer = new Timer();
            timer.start();
            for (int i=0; i<arr.length; i++) {
                Tensor tensor = arr[i];  
                DimIter iter = new DimIter(tensor.getDims());                
                //for (int c=0; c < tensor.size(); c++) {
                int c = 0;
                while (iter.hasNext()) {
                    tensor.add(c++, iter.next());
                }
            }
            timer.stop();
            System.out.println("tot(ms) index iteration: " + timer.totMs());
        }
    }
    
    @Test
    public void testValueOperations() {
        Tensor f1 = new Tensor(s, 2);
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
        Tensor f1 = new Tensor(s, 2, 3);
        f1.fill(1);
        f1.setValue(2, 2);
        f1.setValue(3, 3);
        
        Tensor f2 = new Tensor(s, 2, 3);
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
    
    @Test
    public void testDotProduct() {
        Tensor t1 = ModuleTestUtils.getVector(s, 2, 3, 5);
        Tensor t2 = ModuleTestUtils.getVector(s, -4, 6, 7);
        
        assertEquals(2*-4 + 3*6 + 5*7, t1.getDotProduct(t2), 1e-13);
    }

}
