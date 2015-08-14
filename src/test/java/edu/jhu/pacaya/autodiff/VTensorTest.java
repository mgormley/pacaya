package edu.jhu.pacaya.autodiff;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import edu.jhu.pacaya.util.JUnitUtils;
import edu.jhu.pacaya.util.collections.QLists;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.Algebras;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.LogSignAlgebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;


public class VTensorTest {

    private Algebra s = RealAlgebra.getInstance();

    private List<Algebra> two = QLists.getList(RealAlgebra.getInstance(), LogSignAlgebra.getInstance());
    private List<Algebra> three = QLists.getList(RealAlgebra.getInstance(), LogSemiring.getInstance(), LogSignAlgebra.getInstance());
    
    @Test
    public void testInitializedToZeros() {
        for (Algebra s : three) {
            VTensor t1 = new VTensor(s, 2,3,5);
            assertEquals(s.zero(), t1.get(1,1,1), 1e-13);
        }
    }
    
    @Test
    public void testGetSetAddSubWithIndices() {
        testGetSetAddSubWithIndices(RealAlgebra.getInstance());
        testGetSetAddSubWithIndices(LogSignAlgebra.getInstance());
        testGetSetAddSubWithIndices(LogSemiring.getInstance());
    }
    
    private void testGetSetAddSubWithIndices(Algebra s) {
        VTensor t1 = new VTensor(s, 2,3,5);
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
        testValueOperations(RealAlgebra.getInstance());
        testValueOperations(LogSignAlgebra.getInstance());
        testValueOperations(LogSemiring.getInstance());
    }
    
    private void testValueOperations(Algebra s) {
        VTensor f1 = new VTensor(s, 2);
        f1.setValue(0, s.fromReal(0));
        f1.setValue(1, s.fromReal(1));
        
        // set, add, scale, get
        f1.fill(s.fromReal(2));
        JUnitUtils.assertArrayEquals(Algebras.getFromReal(new double[]{2, 2}, s), f1.getValuesAsNativeArray(), 1e-13);
        
        f1.setValue(0, s.fromReal(1));
        f1.add(s.fromReal(2));
        JUnitUtils.assertArrayEquals(Algebras.getFromReal(new double[]{3, 4}, s), f1.getValuesAsNativeArray(), 1e-13);
        
        f1.multiply(s.fromReal(0.5));
        JUnitUtils.assertArrayEquals(Algebras.getFromReal(new double[]{1.5, 2}, s), f1.getValuesAsNativeArray(), 1e-13);
        
        assertEquals(s.fromReal(1.5), f1.getValue(0), 1e-13);
        assertEquals(s.fromReal(2.0), f1.getValue(1), 1e-13);        
    }

    @Test
    public void testFactorAddIdentical() {   
        testFactorAddIdentical(RealAlgebra.getInstance());
        testFactorAddIdentical(LogSignAlgebra.getInstance());
        testFactorAddIdentical(LogSemiring.getInstance());
    }
    
    private void testFactorAddIdentical(Algebra s) {   
        // Test where vars1 is identical to vars2.
        VTensor f1 = new VTensor(s, 2, 3);
        f1.fill(s.fromReal(1));
        f1.setValue(2, s.fromReal(2));
        f1.setValue(3, s.fromReal(3));
        
        VTensor f2 = new VTensor(s, 2, 3);
        f2.fill(s.fromReal(2));
        f2.setValue(2, s.fromReal(5));
        f2.setValue(5, s.fromReal(7));
        
        // values=[1.0, 1.0, 2.0, 3.0, 1.0, 1.0]
        System.out.println("f1: " + f1);
        // values=[2.0, 2.0, 5.0, 2.0, 2.0, 7.0]
        System.out.println("f2: " + f2);
        
        f1.elemAdd(f2);                
        System.out.println("f1+f2:" + f1);
        
        JUnitUtils.assertArrayEquals(Algebras.getFromReal(new double[]{3.0, 3.0, 7.0, 5.0, 3.0, 8.0}, s), f1.getValuesAsNativeArray(), 1e-13);                
    }
    
    @Test
    public void testDotProduct() {
        for (Algebra s : two) {
            VTensor t1 = VTensorUtils.getVectorFromValues(s, s.fromReal(2), s.fromReal(3), s.fromReal(5));
            VTensor t2 = VTensorUtils.getVectorFromValues(s, s.fromReal(-4), s.fromReal(6), s.fromReal(7));
            assertEquals(s.fromReal(2*-4 + 3*6 + 5*7), t1.getDotProduct(t2), 1e-13);
        }
    }

}
