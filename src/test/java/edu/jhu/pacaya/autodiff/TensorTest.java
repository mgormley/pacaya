package edu.jhu.pacaya.autodiff;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import edu.jhu.pacaya.util.JUnitUtils;
import edu.jhu.pacaya.util.collections.QLists;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.Algebras;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.LogSignAlgebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;


public class TensorTest {

    private Algebra s = RealAlgebra.getInstance();

    private List<Algebra> two = QLists.getList(RealAlgebra.getInstance(), LogSignAlgebra.getInstance());
    private List<Algebra> three = QLists.getList(RealAlgebra.getInstance(), LogSemiring.getInstance(), LogSignAlgebra.getInstance());
    
    @Test
    public void testInitializedToZeros() {
        for (Algebra s : three) {
            Tensor t1 = new Tensor(s, 2,3,5);
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
        testValueOperations(RealAlgebra.getInstance());
        testValueOperations(LogSignAlgebra.getInstance());
        testValueOperations(LogSemiring.getInstance());
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
        testFactorAddIdentical(RealAlgebra.getInstance());
        testFactorAddIdentical(LogSignAlgebra.getInstance());
        testFactorAddIdentical(LogSemiring.getInstance());
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

    /* ----- Tests from UnravelIndexTest ------- */

    private void brokenVarArgsSwap(int... ints) {
        ints = ints.clone();
        int tmp = ints[0];
        ints[0] = ints[1];
        ints[1] = tmp;
    }

    @Test
    public void testBrokenSwap() {
        int[] orig = { 1, 2 };
        int[] intsA = { 1, 2 };
        brokenVarArgsSwap(intsA);
        org.junit.Assert.assertArrayEquals(orig, intsA);
        int[] intsB = new int[2];
        intsB[0] = 1;
        intsB[1] = 2;
        brokenVarArgsSwap(intsB);
        org.junit.Assert.assertArrayEquals(orig, intsB);
    }

    private void auxTestIndex(int configIx, int[] dims, int... expected) {
        org.junit.Assert.assertArrayEquals(expected, Tensor.unravelIndex(configIx, dims));
        org.junit.Assert.assertEquals(configIx, Tensor.ravelIndex(expected, dims));
    }

    private void auxTestMatlabIndex(int configIx, int[] dims, int... expected) {
        ArrayUtils.toString(expected);
        int[] observed = Tensor.unravelIndexMatlab(configIx, dims);
        System.out.println(String.format("expected: %s\nobserved: %s", ArrayUtils.toString(expected), ArrayUtils.toString(observed)));
        org.junit.Assert.assertArrayEquals(expected, observed);
        org.junit.Assert.assertEquals(configIx, Tensor.ravelIndexMatlab(expected, dims));
    }

    @Test
    public void testUnravelIndex() {
        int[] dimsEasy = { 1, 1 };
        auxTestIndex(0, dimsEasy, 0, 0);

        int[] dimsEasy2 = { 2, 2 };
        auxTestIndex(0, dimsEasy2, 0, 0);
        auxTestIndex(1, dimsEasy2, 0, 1);
        auxTestIndex(2, dimsEasy2, 1, 0);
        auxTestIndex(3, dimsEasy2, 1, 1);

        int[] dimsEasy3 = { 1, 2, 3 };
        auxTestIndex(0, dimsEasy3, 0, 0, 0);
        auxTestIndex(1, dimsEasy3, 0, 0, 1);
        auxTestIndex(2, dimsEasy3, 0, 0, 2);
        auxTestIndex(3, dimsEasy3, 0, 1, 0);
        auxTestIndex(4, dimsEasy3, 0, 1, 1);
        auxTestIndex(5, dimsEasy3, 0, 1, 2);

        int[] dimsEasy4 = { 4, 1, 2, 3 };
        auxTestIndex(0, dimsEasy4, 0, 0, 0, 0);
        auxTestIndex(1, dimsEasy4, 0, 0, 0, 1);
        auxTestIndex(2, dimsEasy4, 0, 0, 0, 2);
        auxTestIndex(3, dimsEasy4, 0, 0, 1, 0);
        auxTestIndex(4, dimsEasy4, 0, 0, 1, 1);
        auxTestIndex(5, dimsEasy4, 0, 0, 1, 2);
        auxTestIndex(6, dimsEasy4, 1, 0, 0, 0);
        auxTestIndex(7, dimsEasy4, 1, 0, 0, 1);
        auxTestIndex(8, dimsEasy4, 1, 0, 0, 2);
        auxTestIndex(9, dimsEasy4, 1, 0, 1, 0);
        auxTestIndex(10, dimsEasy4, 1, 0, 1, 1);
        auxTestIndex(11, dimsEasy4, 1, 0, 1, 2);
        auxTestIndex(12, dimsEasy4, 2, 0, 0, 0);
        auxTestIndex(13, dimsEasy4, 2, 0, 0, 1);
        auxTestIndex(14, dimsEasy4, 2, 0, 0, 2);
        auxTestIndex(15, dimsEasy4, 2, 0, 1, 0);
        auxTestIndex(16, dimsEasy4, 2, 0, 1, 1);
        auxTestIndex(17, dimsEasy4, 2, 0, 1, 2);
        auxTestIndex(18, dimsEasy4, 3, 0, 0, 0);
        auxTestIndex(19, dimsEasy4, 3, 0, 0, 1);
        auxTestIndex(20, dimsEasy4, 3, 0, 0, 2);
        auxTestIndex(21, dimsEasy4, 3, 0, 1, 0);
        auxTestIndex(22, dimsEasy4, 3, 0, 1, 1);
        auxTestIndex(23, dimsEasy4, 3, 0, 1, 2);

        auxTestMatlabIndex(0,  dimsEasy4, 0, 0, 0, 0);
        auxTestMatlabIndex(1,  dimsEasy4, 1, 0, 0, 0);
        auxTestMatlabIndex(2,  dimsEasy4, 2, 0, 0, 0);
        auxTestMatlabIndex(3,  dimsEasy4, 3, 0, 0, 0);
        auxTestMatlabIndex(4,  dimsEasy4, 0, 0, 1, 0);
        auxTestMatlabIndex(5,  dimsEasy4, 1, 0, 1, 0);
        auxTestMatlabIndex(6,  dimsEasy4, 2, 0, 1, 0);
        auxTestMatlabIndex(7,  dimsEasy4, 3, 0, 1, 0);
        auxTestMatlabIndex(8,  dimsEasy4, 0, 0, 0, 1);
        auxTestMatlabIndex(9,  dimsEasy4, 1, 0, 0, 1);
        auxTestMatlabIndex(10, dimsEasy4, 2, 0, 0, 1);
        auxTestMatlabIndex(11, dimsEasy4, 3, 0, 0, 1);
        auxTestMatlabIndex(12, dimsEasy4, 0, 0, 1, 1);
        auxTestMatlabIndex(13, dimsEasy4, 1, 0, 1, 1);
        auxTestMatlabIndex(14, dimsEasy4, 2, 0, 1, 1);
        auxTestMatlabIndex(15, dimsEasy4, 3, 0, 1, 1);
        auxTestMatlabIndex(16, dimsEasy4, 0, 0, 0, 2);
        auxTestMatlabIndex(17, dimsEasy4, 1, 0, 0, 2);
        auxTestMatlabIndex(18, dimsEasy4, 2, 0, 0, 2);
        auxTestMatlabIndex(19, dimsEasy4, 3, 0, 0, 2);
        auxTestMatlabIndex(20, dimsEasy4, 0, 0, 1, 2);
        auxTestMatlabIndex(21, dimsEasy4, 1, 0, 1, 2);
        auxTestMatlabIndex(22, dimsEasy4, 2, 0, 1, 2);
        auxTestMatlabIndex(23, dimsEasy4, 3, 0, 1, 2);
    }
}
