package edu.jhu.gm.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor.LinkVar;
import edu.jhu.util.semiring.Algebras;

public class VarSetTest {

    @Test
    public void testConvertBackAndForthBtwnVcAndId() {
        backAndForthVc(4);
        backAndForthVc(5);
        backAndForthVc(6);
        backAndForthVc(7);
    }

    private void backAndForthVc(int n) {
        // Create vc for left branching tree.
        VarConfig vc1 = new VarConfig();
        for (int i=-1; i<n; i++) {
            for (int j=0; j<n; j++) {
                if (i == j) { continue; }
                Var var = new LinkVar(VarType.PREDICTED, "Link_"+i+"_"+j, i, j);
                vc1.put(var, (i == j - 1) ? LinkVar.TRUE : LinkVar.FALSE);
            }
        }
        if (n <= 5) {
            // We should be able to convert back and forth correctly.
            int vcid1 = vc1.getConfigIndex();
            VarConfig vc2 = vc1.getVars().getVarConfig(vcid1);
            int vcid2 = vc2.getConfigIndex();
            assertEquals(vcid1, vcid2);
            assertEquals(vc1.getVars(), vc2.getVars());
            System.out.println(vc1);
            System.out.println(vc2);
            for (Var var : vc1.getVars()) {
                System.out.println(var);
                assertEquals(vc1.getStateName(var), vc2.getStateName(var));
            }
            assertEquals(vc1, vc2);       
        } else {
            // We should hit an integer overflow exception.
            try {
                int vcid1 = vc1.getConfigIndex();
                fail();
            } catch(IllegalStateException e) {
                assertTrue(e.getMessage().contains("overflow"));
            }
        }
    }
    
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
    
    @Test
    public void testGetConfigArray2Swapped() {
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
        
        System.out.println(new VarTensor(Algebras.REAL_ALGEBRA, vars1));
        
        // TODO: we can't loop over a particular configuration of vars1, only the config in which each (non-vars2) variable has state 0.
        int[] configs = vars1.getConfigArr(vars2);
        System.out.println(Arrays.toString(configs));
        assertEquals(2*5, configs.length);
        Assert.assertArrayEquals(new int[]{0, 1, 6, 7, 12, 13, 18, 19, 24, 25}, configs);
    }
    
    public static Var getVar(int id, int numStates) {
        ArrayList<String> stateNames = new ArrayList<String>();
        for (int i=0; i<numStates; i++) {
            stateNames.add("state" + i);
        }
        return new Var(VarType.OBSERVED, numStates, "var"+id, stateNames);
    }
    
}
