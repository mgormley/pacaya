package edu.jhu.gm;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import edu.jhu.gm.Var.VarType;

public class VarSetTest {

    @Test
    public void testGetNumConfigs() {
        Var v0 = getVar(0, 2);
        Var v1 = getVar(1, 3);
        Var v2 = getVar(2, 5);

        VarSet vars = new VarSet();
        vars.add(v0);
        vars.add(v1);
        vars.add(v2);
        
        assertEquals(2*3*5, vars.calcNumConfigs());
    }
    
    @Test
    public void testGetConfigArray1() {
        Var v0 = getVar(0, 2);
        Var v1 = getVar(1, 3);
        Var v2 = getVar(2, 5);

        VarSet vars1 = new VarSet();
        vars1.add(v0);
        vars1.add(v1);
        vars1.add(v2);

        VarSet vars2 = new VarSet();
        vars2.add(v1);
        vars2.add(v2);
        
        int[] configs = vars2.getConfigArr(vars1);
        System.out.println(Arrays.toString(configs));
        assertEquals(2*3*5, configs.length);
        Assert.assertArrayEquals(new int[]{0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14}, configs);
    }

    @Test
    public void testGetConfigArray2() {
        Var v0 = getVar(0, 2);
        Var v1 = getVar(1, 3);
        Var v2 = getVar(2, 5);

        VarSet vars1 = new VarSet();
        vars1.add(v0);
        vars1.add(v1);
        vars1.add(v2);

        VarSet vars2 = new VarSet();
        vars2.add(v0);
        vars2.add(v2);
        
        int[] configs = vars2.getConfigArr(vars1);
        System.out.println(Arrays.toString(configs));
        assertEquals(2*3*5, configs.length);
        Assert.assertArrayEquals(new int[]{0, 1, 0, 1, 0, 1, 2, 3, 2, 3, 2, 3, 4, 5, 4, 5, 4, 5, 6, 7, 6, 7, 6, 7, 8, 9, 8, 9, 8, 9}, configs);
    }
    

    @Test
    public void testGetConfigArray3() {
        Var v0 = getVar(0, 2);
        Var v1 = getVar(1, 3);
        Var v2 = getVar(2, 5);

        VarSet vars1 = new VarSet();
        vars1.add(v0);
        vars1.add(v1);
        vars1.add(v2);

        VarSet vars2 = new VarSet();
        vars2.add(v0);
        vars2.add(v1);
        
        int[] configs = vars2.getConfigArr(vars1);
        System.out.println(Arrays.toString(configs));
        assertEquals(2*3*5, configs.length);
        Assert.assertArrayEquals(new int[]{0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5}, configs);
    }

    public static Var getVar(int id, int numStates) {
        ArrayList<String> stateNames = new ArrayList<String>();
        for (int i=0; i<numStates; i++) {
            stateNames.add("state" + i);
        }
        return new Var(VarType.OBSERVED, numStates, "var"+id, stateNames);
    }
    
}
