package edu.jhu.gm.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.util.JUnitUtils;

public class DenseFactorTest {

    @Test
    public void testValueOperations() {
        VarSet vars1 = new VarSet();
        vars1.add(VarSetTest.getVar(0, 2));
        DenseFactor f1 = new DenseFactor(vars1);
        f1.setValue(0, 0);
        f1.setValue(1, 1);
        
        // set, add, scale, get
        f1.set(2);
        JUnitUtils.assertArrayEquals(new double[]{2, 2}, f1.getValues(), 1e-13);
        
        f1.setValue(0, 1);
        f1.add(2);
        JUnitUtils.assertArrayEquals(new double[]{3, 4}, f1.getValues(), 1e-13);
        
        f1.scale(0.5);
        JUnitUtils.assertArrayEquals(new double[]{1.5, 2}, f1.getValues(), 1e-13);
        
        assertEquals(1.5, f1.getValue(0), 1e-13);
        assertEquals(2.0, f1.getValue(1), 1e-13);
        
        // normalize, logNormalize
        // convertRealToLog, convertLogToReal

        f1.setValue(0, 4);
        f1.setValue(1, 6);
        f1.normalize();
        JUnitUtils.assertArrayEquals(new double[]{0.4, 0.6}, f1.getValues(), 1e-13);

        f1.convertRealToLog();
        JUnitUtils.assertArrayEquals(new double[]{-0.91, -0.51}, f1.getValues(), 1e-1);
        
        f1.add(10);
        f1.logNormalize();
        JUnitUtils.assertArrayEquals(new double[]{-0.91, -0.51}, f1.getValues(), 1e-1);
        
        // getSum, getLogSum
        
        assertEquals(0.0, f1.getLogSum(), 1e-13);
        
        f1.convertLogToReal();
        
        assertEquals(1.0, f1.getSum(), 1e-13);
    }

    @Test
    public void testGetMarginal() {
        Var v0 = VarSetTest.getVar(0, 2);
        Var v1 = VarSetTest.getVar(1, 3);
        VarSet vars1 = new VarSet();
        vars1.add(v0);
        vars1.add(v1);
        DenseFactor f1 = new DenseFactor(vars1);
        f1.setValue(0, 0);
        f1.setValue(1, 1);
        f1.setValue(2, 2);
        f1.setValue(3, 3);
        f1.setValue(4, 4);
        
        DenseFactor marg = f1.getMarginal(new VarSet(v0), false);
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
        DenseFactor f1 = new DenseFactor(vars1);
        f1.setValue(0, 0);
        f1.setValue(1, 1);
        f1.setValue(2, 2);
        f1.setValue(3, 3);
        f1.setValue(4, 4);
        f1.setValue(5, 5);
        
        System.out.println(f1);
        
        VarConfig vc0 = new VarConfig();
        vc0.put(v0, 0);
        DenseFactor clmp = f1.getClamped(vc0);
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
        DenseFactor f1 = new DenseFactor(vars1);
        f1.set(1);
        f1.setValue(2, 2);
        f1.setValue(3, 3);
        
        VarSet vars2 = vars1;
        DenseFactor f2 = new DenseFactor(vars2);
        f2.set(2);
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
        DenseFactor f1 = new DenseFactor(vars1);
        f1.set(1);
        f1.setValue(2, 2);
        f1.setValue(3, 3);
        
        VarSet vars2 = new VarSet();
        vars2.add(v1);        
        DenseFactor f2 = new DenseFactor(vars2);
        f2.set(2);
        f2.setValue(2, 5);
        
        // values=[1.0, 1.0, 2.0, 3.0, 1.0, 1.0]
        System.out.println("f1: " + f1);
        // values=[2.0, 2.0, 5.0]]
        System.out.println("f2: " + f2);
        
        f1.add(f2);                
        System.out.println("f1+f2:" + f1);
        
        JUnitUtils.assertArrayEquals(new double[]{3.0, 3.0, 4.0, 5.0, 6.0, 6.0}, f1.getValues(), 1e-13);                
    }

    @Test
    public void testFactorAddDiff() {   
        Var v0 = VarSetTest.getVar(0, 2);
        Var v1 = VarSetTest.getVar(1, 3);
        Var v2 = VarSetTest.getVar(2, 2);

        // Test where the difference of vars1 and vars2 is non-empty and so we must take their union.
        VarSet vars1 = new VarSet();
        vars1.add(v0);
        vars1.add(v1);        
        DenseFactor f1 = new DenseFactor(vars1);
        f1.set(1);
        f1.setValue(2, 2);
        f1.setValue(3, 3);
        
        VarSet vars2 = new VarSet();
        vars2.add(v1);
        vars2.add(v2);    
        DenseFactor f2 = new DenseFactor(vars2);
        f2.set(2);
        f2.setValue(2, 5);
        f2.setValue(5, 7);
        
        // values=[1.0, 1.0, 2.0, 3.0, 1.0, 1.0]
        System.out.println("f1: " + f1);
        // values=[2.0, 2.0, 5.0, 2.0, 2.0, 7.0]
        System.out.println("f2: " + f2);
        
        f1.add(f2);                
        System.out.println("f1+f2:" + f1);
        
        JUnitUtils.assertArrayEquals(new double[]{3.0, 3.0, 4.0, 5.0, 6.0, 6.0, 3.0, 3.0, 4.0, 5.0, 8.0, 8.0}, f1.getValues(), 1e-13);                
    }

}
