package edu.jhu.pacaya.autodiff;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import edu.jhu.pacaya.util.JUnitUtils;
import edu.jhu.pacaya.util.collections.Lists;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.Algebras;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.LogSignAlgebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;


public class TensorTest {

    private Algebra s = new RealAlgebra();

    private List<Algebra> two = Lists.getList(RealAlgebra.REAL_ALGEBRA, LogSignAlgebra.LOG_SIGN_ALGEBRA);
    private List<Algebra> three = Lists.getList(RealAlgebra.REAL_ALGEBRA, LogSemiring.LOG_SEMIRING, LogSignAlgebra.LOG_SIGN_ALGEBRA);
    
    @Test
    public void testInitializedToZeros() {
        for (Algebra s : three) {
            Tensor t1 = new Tensor(s, 2,3,5);
            assertEquals(s.zero(), t1.get(1,1,1), 1e-13);
        }
    }
    
    @Test
    public void testGetSetAddSubWithIndices() {
        testGetSetAddSubWithIndices(RealAlgebra.REAL_ALGEBRA);
        testGetSetAddSubWithIndices(LogSignAlgebra.LOG_SIGN_ALGEBRA);
        testGetSetAddSubWithIndices(LogSemiring.LOG_SEMIRING);
    }
    
    private void testGetSetAddSubWithIndices(Algebra s) {
        Tensor t1 = new Tensor(s, 2,3,5);
        // Test set.
        double val;
        val = 0;
        for (int i=0; i<2; i++) {
            for (int j=0; j<3; j++) {
                for (int k=0; k<5; k++) {
                    assertEquals(s.fromReal(0), t1.set(val++, i,j,k), 1e-13);            
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
                    double exp = s.times(s.fromReal(2), val);
                    assertEquals(exp, t1.get(i,j,k), 1e-13);            
                    val++;
                }
            }
        }
        
        // Test subtract.
        val = 0;
        for (int i=0; i<2; i++) {
            for (int j=0; j<3; j++) {
                for (int k=0; k<5; k++) {
                    double exp = s.times(s.fromReal(2), val);
                    assertEquals(exp, t1.subtract(val, i,j,k), 1e-13);
                    assertEquals(val, t1.get(i,j,k), 1e-13);            
                    val++;
                }
            }
        }
    }
    
    @Test
    public void testValueOperations() {
        testValueOperations(RealAlgebra.REAL_ALGEBRA);
        testValueOperations(LogSignAlgebra.LOG_SIGN_ALGEBRA);
        testValueOperations(LogSemiring.LOG_SEMIRING);
    }
    
    private void testValueOperations(Algebra s) {
        Tensor f1 = new Tensor(s, 2);
        f1.setValue(0, s.fromReal(0));
        f1.setValue(1, s.fromReal(1));
        
        // set, add, scale, get
        f1.fill(s.fromReal(2));
        JUnitUtils.assertArrayEquals(Algebras.getFromReal(new double[]{2, 2}, s), f1.getValues(), 1e-13);
        
        f1.setValue(0, s.fromReal(1));
        f1.add(s.fromReal(2));
        JUnitUtils.assertArrayEquals(Algebras.getFromReal(new double[]{3, 4}, s), f1.getValues(), 1e-13);
        
        f1.multiply(s.fromReal(0.5));
        JUnitUtils.assertArrayEquals(Algebras.getFromReal(new double[]{1.5, 2}, s), f1.getValues(), 1e-13);
        
        assertEquals(s.fromReal(1.5), f1.getValue(0), 1e-13);
        assertEquals(s.fromReal(2.0), f1.getValue(1), 1e-13);        
    }

    @Test
    public void testFactorAddIdentical() {   
        testFactorAddIdentical(RealAlgebra.REAL_ALGEBRA);
        testFactorAddIdentical(LogSignAlgebra.LOG_SIGN_ALGEBRA);
        testFactorAddIdentical(LogSemiring.LOG_SEMIRING);
    }
    
    private void testFactorAddIdentical(Algebra s) {   
        // Test where vars1 is identical to vars2.
        Tensor f1 = new Tensor(s, 2, 3);
        f1.fill(s.fromReal(1));
        f1.setValue(2, s.fromReal(2));
        f1.setValue(3, s.fromReal(3));
        
        Tensor f2 = new Tensor(s, 2, 3);
        f2.fill(s.fromReal(2));
        f2.setValue(2, s.fromReal(5));
        f2.setValue(5, s.fromReal(7));
        
        // values=[1.0, 1.0, 2.0, 3.0, 1.0, 1.0]
        System.out.println("f1: " + f1);
        // values=[2.0, 2.0, 5.0, 2.0, 2.0, 7.0]
        System.out.println("f2: " + f2);
        
        f1.elemAdd(f2);                
        System.out.println("f1+f2:" + f1);
        
        JUnitUtils.assertArrayEquals(Algebras.getFromReal(new double[]{3.0, 3.0, 7.0, 5.0, 3.0, 8.0}, s), f1.getValues(), 1e-13);                
    }
    
    @Test
    public void testDotProduct() {
        for (Algebra s : two) {
            Tensor t1 = TensorUtils.getVectorFromValues(s, s.fromReal(2), s.fromReal(3), s.fromReal(5));
            Tensor t2 = TensorUtils.getVectorFromValues(s, s.fromReal(-4), s.fromReal(6), s.fromReal(7));
            assertEquals(s.fromReal(2*-4 + 3*6 + 5*7), t1.getDotProduct(t2), 1e-13);
        }
    }

}
