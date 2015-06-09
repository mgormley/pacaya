package edu.jhu.pacaya.gm.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;


public class IndexForVcTest {

    private Algebra s = RealAlgebra.REAL_ALGEBRA;

    Var v0 = getVar(0, 2);
    Var v1 = getVar(1, 3);
    Var v2 = getVar(2, 5);
    Var v3 = getVar(3, 7);
    VarSet vars1 = new VarSet(v0, v1, v2); // v3 is NOT included here.
    
    @Test
    public void testIterate() {
        VarSet vars1 = new VarSet(v0, v1, v2, v3); // v3 IS included here.
        checkThatIterateWorks(vars1, v0, v2, 0, 0);
        checkThatIterateWorks(vars1, v0, v2, 0, 1);
        checkThatIterateWorks(vars1, v0, v2, 1, 0);
        checkThatIterateWorks(vars1, v0, v2, 1, 3);
    }

    private void checkThatIterateWorks(VarSet vars1, Var v0, Var v2, int aa, int cc) {
        VarConfig vc = new VarConfig();
        vc.put(v0, aa);
        vc.put(v2, cc);
        
        IndexFor iter = IndexForVc.getConfigIter(vars1, vc);
        for (int b = 0; b < 3; b++) {
            for (int d = 0; d < 7; d++) {
                assertTrue(iter.hasNext());
                int val = iter.next();
                System.out.println(val);
                assertEquals(aa * 3 * 5 * 7 + b * 5 * 7 + cc * 7 + d, val);
            }
        }
        assertFalse(iter.hasNext());
        System.out.println();
    }
        
    @Test
    public void testGetConfigArray1() {
        {
            VarConfig vc2 = new VarConfig();
            vc2.put(v0, 1);        
            vc2.put(v1, 1);
            int[] configs = IndexForVc.getConfigArr(vars1, vc2);
            System.out.println(Arrays.toString(configs));
            assertEquals(5, configs.length);
            Assert.assertArrayEquals(new int[]{20, 21, 22, 23, 24}, configs);        
        }
        {
            VarConfig vc2 = new VarConfig();
            vc2.put(v0, 1);        
            int[] configs = IndexForVc.getConfigArr(vars1, vc2);
            System.out.println(Arrays.toString(configs));
            assertEquals(3*5, configs.length);
            Assert.assertArrayEquals(new int[]{15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29}, configs);        
        }
    }
    
    @Test
    public void testGetConfigArray2() {
        VarConfig vc2 = new VarConfig();
        vc2.put(v1, 0);
        
        System.out.println(new VarTensor(s, vars1));
        
        // TODO: we can't loop over a particular configuration of vars1, only the config in which each (non-vars2) variable has state 0.
        int[] configs = IndexForVc.getConfigArr(vars1, vc2);
        System.out.println(Arrays.toString(configs));
        assertEquals(2*5, configs.length);
        Assert.assertArrayEquals(new int[]{0, 1, 2, 3, 4, 15, 16, 17, 18, 19}, configs);
    }
    
    @Test
    public void testGetConfigArray3() {
        VarConfig vc2 = new VarConfig();
        vc2.put(v1, 1);
        
        System.out.println(new VarTensor(s, vars1));
        
        // TODO: we can't loop over a particular configuration of vars1, only the config in which each (non-vars2) variable has state 0.
        int[] configs = IndexForVc.getConfigArr(vars1, vc2);
        System.out.println(Arrays.toString(configs));
        assertEquals(2*5, configs.length);
        Assert.assertArrayEquals(new int[]{5, 6, 7, 8, 9, 20, 21, 22, 23, 24}, configs);
    }
    
    @Test
    public void testGetConfigArray4() {
        VarConfig vc2 = new VarConfig();
        vc2.put(v0, 1);
        vc2.put(v2, 4);
        
        System.out.println(new VarTensor(s, vars1));
        
        // TODO: we can't loop over a particular configuration of vars1, only the config in which each (non-vars2) variable has state 0.
        int[] configs = IndexForVc.getConfigArr(vars1, vc2);
        System.out.println(Arrays.toString(configs));
        assertEquals(3, configs.length);
        Assert.assertArrayEquals(new int[]{19, 24, 29}, configs);
    }    

    public static Var getVar(int id, int numStates) {
        ArrayList<String> stateNames = new ArrayList<String>();
        for (int i=0; i<numStates; i++) {
            stateNames.add("state" + i);
        }
        return new Var(VarType.PREDICTED, numStates, "var"+id, stateNames);
    }
    
}
