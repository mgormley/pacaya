package edu.jhu.pacaya.gm.model;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import edu.jhu.pacaya.util.JUnitUtils;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.arrays.DoubleArrays;

public class VarTensorTest {

    private Algebra s = RealAlgebra.REAL_ALGEBRA;
    
    @Test
    public void testProdNoVars() {
        Var v0 = VarSetTest.getVar(0, 2);
        Var v1 = VarSetTest.getVar(0, 3);
        Var v2 = VarSetTest.getVar(0, 5);
        Var v3 = VarSetTest.getVar(0, 7);
        
        VarTensor f1 = new VarTensor(RealAlgebra.REAL_ALGEBRA, new VarSet());
        VarTensor f2 = new VarTensor(RealAlgebra.REAL_ALGEBRA, new VarSet(v0, v1, v2, v3));
        for (int c=0; c<f1.size(); c++) {
            f2.setValue(c, 2*c);
        }
        f1.prod(f2);
        for (int c=0; c<f1.size(); c++) {
            assertEquals(f2.getValue(c), f1.getValue(c), 1e-13);
        }
    }
    
    @Test
    public void testProdSameVars() {
        Var v0 = VarSetTest.getVar(0, 2);
        Var v1 = VarSetTest.getVar(0, 3);
        Var v2 = VarSetTest.getVar(0, 5);
        Var v3 = VarSetTest.getVar(0, 7);
        
        VarTensor f1 = new VarTensor(RealAlgebra.REAL_ALGEBRA, new VarSet(v0, v1, v2, v3));
        VarTensor f2 = new VarTensor(RealAlgebra.REAL_ALGEBRA, new VarSet(v0, v1, v2, v3));
        for (int c=0; c<f1.size(); c++) {
            f1.setValue(c, c);
            f2.setValue(c, 2*c);
        }
        f1.prod(f2);
        for (int c=0; c<f1.size(); c++) {
            assertEquals(2*c*c, f1.getValue(c), 1e-13);
        }
    }

    @Test
    public void testProdSupersetVars() {
        Var v0 = VarSetTest.getVar(0, 2);
        Var v1 = VarSetTest.getVar(0, 3);
        Var v2 = VarSetTest.getVar(0, 5);
        Var v3 = VarSetTest.getVar(0, 7);
        
        VarTensor f1 = new VarTensor(RealAlgebra.REAL_ALGEBRA, new VarSet(v0, v1, v2, v3));
        VarTensor f2 = new VarTensor(RealAlgebra.REAL_ALGEBRA, new VarSet(v0, v2));
        for (int a=0; a<2; a++) {
            for (int b=0; b<3; b++) {
                for (int c=0; c<5; c++) {
                    for (int d=0; d<7; d++) {
                        f1.set(1 + (a*3*5*7 + b*5*7 + c*7 + d), a, b, c, d);
                    }
                }
            }
        }
        for (int a=0; a<2; a++) {
            for (int c=0; c<5; c++) {
                f2.set(1 + (a*3*5*7 + c*7), a, c);
            }
        }
        f1.prod(f2);
        System.out.println(f1);
        for (int a=0; a<2; a++) {
            for (int b=0; b<3; b++) {
                for (int c=0; c<5; c++) {
                    for (int d=0; d<7; d++) {
                        assertEquals((1 + (a*3*5*7 + c*7)) * (1 + (a*3*5*7 + b*5*7 + c*7 + d)), f1.get(a, b, c, d), 1e-13);
                    }
                }
            }
        }
    }

    @Test
    public void testProdOneCommonVar() {
        Var v0 = VarSetTest.getVar(0, 2);
        Var v1 = VarSetTest.getVar(0, 3);
        Var v2 = VarSetTest.getVar(0, 5);
        Var v3 = VarSetTest.getVar(0, 7);
        
        VarTensor f1 = new VarTensor(RealAlgebra.REAL_ALGEBRA, new VarSet(v0, v1, v3));
        VarTensor f2 = new VarTensor(RealAlgebra.REAL_ALGEBRA, new VarSet(v0, v2));
        for (int a=0; a<2; a++) {
            for (int b=0; b<3; b++) {
                for (int d=0; d<7; d++) {
                    f1.set(1 + (a*3*5*7 + b*5*7 + d), a, b, d);
                }
            }
        }
        for (int a=0; a<2; a++) {
            for (int c=0; c<5; c++) {
                f2.set(1 + (a*3*5*7 + c*7), a, c);
            }
        }
        {
            // Test f1 * f2.
            VarTensor f1f2 = new VarTensor(f1);
            f1f2.prod(f2);
            System.out.println(f1);
            for (int a=0; a<2; a++) {
                for (int b=0; b<3; b++) {
                    for (int c=0; c<5; c++) {
                        for (int d=0; d<7; d++) {
                            assertEquals((1 + (a*3*5*7 + c*7)) * (1 + (a*3*5*7 + b*5*7 + d)), f1f2.get(a, b, c, d), 1e-13);
                        }
                    }
                }
            }
        }
        {
            // Test f2 * f1.
            VarTensor f1f2 = new VarTensor(f2);
            f1f2.prod(f1);
            System.out.println(f1);
            for (int a=0; a<2; a++) {
                for (int b=0; b<3; b++) {
                    for (int c=0; c<5; c++) {
                        for (int d=0; d<7; d++) {
                            assertEquals((1 + (a*3*5*7 + c*7)) * (1 + (a*3*5*7 + b*5*7 + d)), f1f2.get(a, b, c, d), 1e-13);
                        }
                    }
                }
            }
        }
    }
    
    @Test
    public void testValueOperations() {
        VarSet vars1 = new VarSet();
        vars1.add(VarSetTest.getVar(0, 2));
        VarTensor f1 = new VarTensor(s, vars1);
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
        
        // normalize
        f1.setValue(0, 4);
        f1.setValue(1, 6);
        f1.normalize();
        JUnitUtils.assertArrayEquals(new double[]{0.4, 0.6}, f1.getValues(), 1e-13);
        
        // getSum        
        assertEquals(1.0, f1.getSum(), 1e-13);                    
    }
    
    @Test
    public void testGetMarginal() {
        Var v0 = VarSetTest.getVar(0, 2);
        Var v1 = VarSetTest.getVar(1, 3);
        VarSet vars1 = new VarSet();
        vars1.add(v0);
        vars1.add(v1);
        VarTensor f1 = new VarTensor(s, vars1);
        f1.set(0, 0, 0);
        f1.set(1, 1, 0);
        f1.set(2, 0, 1);
        f1.set(3, 1, 1);
        f1.set(4, 0, 2);
        f1.set(0, 1, 2);
        
        VarTensor marg = f1.getMarginal(new VarSet(v0), false);
        System.out.println(marg);
        JUnitUtils.assertArrayEquals(new double[]{6, 4}, marg.getValues(), 1e-13);
        
        // And normalize.
        marg = f1.getMarginal(new VarSet(v0), true);
        System.out.println(marg);
        JUnitUtils.assertArrayEquals(new double[]{.6, .4}, marg.getValues(), 1e-13);
    }

    @Test
    public void testGetClamped() {
        Var v0 = VarSetTest.getVar(0, 2);
        Var v1 = VarSetTest.getVar(1, 3);
        VarSet vars1 = new VarSet();
        vars1.add(v0);
        vars1.add(v1);
        VarTensor f1 = new VarTensor(s, vars1);
        f1.set(0, 0, 0);
        f1.set(1, 1, 0);
        f1.set(2, 0, 1);
        f1.set(3, 1, 1);
        f1.set(4, 0, 2);
        f1.set(5, 1, 2);
        
        System.out.println(f1);
        
        VarConfig vc0 = new VarConfig();
        vc0.put(v0, 0);
        VarTensor clmp = f1.getClamped(vc0);
        System.out.println(clmp);
        JUnitUtils.assertArrayEquals(new double[]{0, 2, 4}, clmp.getValues(), 1e-13);
        

        VarConfig vc1 = new VarConfig();
        vc1.put(v1, 1);    
        clmp = f1.getClamped(vc1);
        System.out.println(clmp);
        JUnitUtils.assertArrayEquals(new double[]{2, 3}, clmp.getValues(), 1e-13);
    }
    

    @Test
    public void testFactorAddIdentical() {   
        // Test where vars1 is identical to vars2.
        VarSet vars1 = new VarSet();
        vars1.add(VarSetTest.getVar(0, 2));
        vars1.add(VarSetTest.getVar(1, 3));        
        VarTensor f1 = new VarTensor(s, vars1);
        f1.fill(1);
        f1.setValue(2, 2);
        f1.setValue(3, 3);
        
        VarSet vars2 = vars1;
        VarTensor f2 = new VarTensor(s, vars2);
        f2.fill(2);
        f2.setValue(2, 5);
        f2.setValue(5, 7);
        
        // values=[1.0, 1.0, 2.0, 3.0, 1.0, 1.0]
        System.out.println("f1: " + f1);
        // values=[2.0, 2.0, 5.0, 2.0, 2.0, 7.0]
        System.out.println("f2: " + f2);
        
        f1.add(f2);                
        System.out.println("f1+f2:" + f1);
        
        JUnitUtils.assertArrayEquals(new double[]{3.0, 3.0, 7.0, 5.0, 3.0, 8.0}, f1.getValues(), 1e-13);                
    }
    
    @Test
    public void testFactorAddSuperset() {   
        Var v0 = VarSetTest.getVar(0, 2);
        Var v1 = VarSetTest.getVar(1, 3);

        // Test where vars1 is a superset of vars2.
        VarSet vars1 = new VarSet();
        vars1.add(v0);
        vars1.add(v1);        
        VarTensor f1 = new VarTensor(s, vars1);
        f1.fill(1);
        f1.set(2.0, 0, 1);
        f1.set(3.0, 1, 1);
        
        VarSet vars2 = new VarSet();
        vars2.add(v1);        
        VarTensor f2 = new VarTensor(s, vars2);
        f2.fill(2);
        f2.set(5.0, 2);
        
        // values=[1.0, 1.0, 2.0, 3.0, 1.0, 1.0]
        System.out.println("f1: " + f1);
        // values=[2.0, 2.0, 5.0]]
        System.out.println("f2: " + f2);
        
        f1.add(f2);                
        System.out.println("f1+f2:" + f1);
        
        JUnitUtils.assertArrayEquals(new double[]{3.0, 4.0, 6.0, 3.0, 5.0, 6.0}, f1.getValues(), 1e-13);                
    }

    @Test
    public void testFactorAddDiff() {   
        Var v0 = VarSetTest.getVar(0, 2);
        Var v2 = VarSetTest.getVar(2, 2);
        Var v1 = VarSetTest.getVar(1, 3);

        // Test where the difference of vars1 and vars2 is non-empty and so we must take their union.
        VarSet vars1 = new VarSet();
        vars1.add(v0);
        vars1.add(v1);        
        VarTensor f1 = new VarTensor(s, vars1);
        f1.fill(1);
        f1.set(2.0, 0, 1);
        f1.set(3.0, 1, 1);
        
        VarSet vars2 = new VarSet();
        vars2.add(v1);
        vars2.add(v2);    
        VarTensor f2 = new VarTensor(s, vars2);
        f2.fill(2);
        f2.set(5.0, 0, 1);
        f2.set(7.0, 1, 2);
        
        // values=[1.0, 1.0, 2.0, 3.0, 1.0, 1.0]
        System.out.println("f1: " + f1);
        // values=[2.0, 2.0, 5.0, 2.0, 2.0, 7.0]
        System.out.println("f2: " + f2);
        
        f1.add(f2);                
        System.out.println("f1+f2:" + f1);
        System.out.println("f1+f2:" + Arrays.toString(f1.getValues()));
                
        JUnitUtils.assertArrayEquals(new double[]{3.0, 7.0, 3.0, 3.0, 4.0, 8.0, 3.0, 8.0, 3.0, 3.0, 5.0, 8.0}, f1.getValues(), 1e-13);                
    }
    
    @Test
    public void testNormalize() {
        Var v0 = VarSetTest.getVar(0, 2);
        Var v1 = VarSetTest.getVar(1, 3);
        VarSet vars1 = new VarSet();
        vars1.add(v0);
        vars1.add(v1);
        VarTensor f1 = new VarTensor(s, vars1);
        f1.setValue(0, 0);
        f1.setValue(1, 1);
        f1.setValue(2, 2);
        f1.setValue(3, 3);
        f1.setValue(4, 4);
        f1.setValue(5, 5);
        
        VarTensor f2 = f1.copyAndConvertAlgebra(LogSemiring.LOG_SEMIRING);

        double[] expected = new double[]{0.0, 1/15., 2/15., 3/15., 4/15., 5/15.};
        
        f1.normalize();        
        JUnitUtils.assertArrayEquals(expected, f1.getValues(), 1e-13);

        DoubleArrays.log(expected);
        f2.normalize();        
        JUnitUtils.assertArrayEquals(expected, f2.getValues(), 1e-13);        
    }    
    
    @Test
    public void testNormalizeZeros() {
        Var v1 = VarSetTest.getVar(1, 3);
        VarSet vars1 = new VarSet();
        vars1.add(v1);
        VarTensor f1 = new VarTensor(s, vars1);
        f1.setValue(0, 0);
        f1.setValue(1, 0);
        f1.setValue(2, 0);
        
        VarTensor f2 = f1.copyAndConvertAlgebra(LogSemiring.LOG_SEMIRING);

        double[] expected = new double[]{1/3., 1/3., 1/3.};
        
        f1.normalize();        
        JUnitUtils.assertArrayEquals(expected, f1.getValues(), 1e-13);

        DoubleArrays.log(expected);
        f2.normalize();        
        JUnitUtils.assertArrayEquals(expected, f2.getValues(), 1e-13);        
    }

    @Test
    public void testTensorGetVsVarSetConfig() throws Exception {
        Var v0 = VarSetTest.getVar(0, 2);
        Var v1 = VarSetTest.getVar(1, 3);
        VarSet vars1 = new VarSet();
        vars1.add(v0);
        vars1.add(v1);
        VarTensor f1 = new VarTensor(s, vars1);
        JUnitUtils.assertArrayEquals(new int[]{2, 3}, f1.getDims());
        for (int c=0; c<f1.size(); c++) {
            f1.setValue(c, c);
        }
        int c = 0;
        for (int a=0; a<2; a++) {
            for (int b=0; b<3; b++) {
                assertEquals(f1.getValue(c), f1.get(a, b), 1e-13);
                JUnitUtils.assertArrayEquals(new int[]{a, b}, vars1.getVarConfigAsArray(c));
                c++;
            }
        }
    }    

}
