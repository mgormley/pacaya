package edu.jhu.hltcoe.gm;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import cern.colt.Arrays;
import edu.jhu.hltcoe.gm.Var.VarType;

public class VarSetTest {

    @Test
    public void testGetNumConfigs() {
        VarSet vars = new VarSet();
        vars.add(getVar(0, 2));
        vars.add(getVar(1, 3));
        vars.add(getVar(2, 5));
        
        assertEquals(2*3*5, vars.getNumConfigs());
    }
    
    @Test
    public void testGetConfigArray1() {
        VarSet vars1 = new VarSet();
        vars1.add(getVar(0, 2));
        vars1.add(getVar(1, 3));
        vars1.add(getVar(2, 5));

        VarSet vars2 = new VarSet();
        vars2.add(getVar(1, 3));
        vars2.add(getVar(2, 5));
        
        int[] configs = vars2.getConfigArr(vars1);
        System.out.println(Arrays.toString(configs));
        assertEquals(2*3*5, configs.length);
        Assert.assertArrayEquals(new int[]{0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14}, configs);
    }

    @Test
    public void testGetConfigArray2() {
        VarSet vars1 = new VarSet();
        vars1.add(getVar(0, 2));
        vars1.add(getVar(1, 3));
        vars1.add(getVar(2, 5));

        VarSet vars2 = new VarSet();
        vars2.add(getVar(0, 2));
        vars2.add(getVar(2, 5));
        
        int[] configs = vars2.getConfigArr(vars1);
        System.out.println(Arrays.toString(configs));
        assertEquals(2*3*5, configs.length);
        Assert.assertArrayEquals(new int[]{0, 1, 0, 1, 0, 1, 2, 3, 2, 3, 2, 3, 4, 5, 4, 5, 4, 5, 6, 7, 6, 7, 6, 7, 8, 9, 8, 9, 8, 9}, configs);
    }
    

    @Test
    public void testGetConfigArray3() {
        VarSet vars1 = new VarSet();
        vars1.add(getVar(0, 2));
        vars1.add(getVar(1, 3));
        vars1.add(getVar(2, 5));

        VarSet vars2 = new VarSet();
        vars2.add(getVar(0, 2));
        vars2.add(getVar(1, 3));
        
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
        return new Var(VarType.OBSERVED, id, numStates, "var"+id, stateNames);
    }
    
}
