package edu.jhu.pacaya.gm.model;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.junit.Test;

import edu.jhu.pacaya.gm.model.Var.VarType;


public class VarConfigTest {

    @Test
    public void testGetState() {

        Var w0 = new Var(VarType.PREDICTED, 2, "w0", null);
        Var w1 = new Var(VarType.PREDICTED, 5, "w1", null);
        Var w2 = new Var(VarType.PREDICTED, 3, "w2", null);
        
        VarConfig config = new VarConfig();
        config.put(w0, 1);
        config.put(w1, 4);
        config.put(w2, 0);
        
        assertEquals(1, config.getState(w0));
        assertEquals(4, config.getState(w1));
        assertEquals(0, config.getState(w2));
    }
    
    @Test
    public void testGetIntersection() {

        Var w0 = new Var(VarType.PREDICTED, 2, "w0", null);
        Var w1 = new Var(VarType.PREDICTED, 5, "w1", null);
        Var w2 = new Var(VarType.PREDICTED, 3, "w2", null);
        
        VarConfig config = new VarConfig();
        config.put(w0, 1);
        config.put(w1, 4);
        config.put(w2, 0);
        
        VarConfig inter = config.getIntersection(new VarSet(w1, w2));
        
        assertEquals(2, inter.size());
        assertEquals(4, inter.getState(w1));
        assertEquals(0, inter.getState(w2));
    }
    
    @Test
    public void testGetConfigIndex() {

        Var w0 = new Var(VarType.PREDICTED, 2, "w0", null);
        Var w2 = new Var(VarType.PREDICTED, 3, "w2", null);
        Var w1 = new Var(VarType.PREDICTED, 5, "w1", null);
        
        VarConfig config = new VarConfig();
        config.put(w0, 1);
        config.put(w1, 4);
        config.put(w2, 0);

        // Vars are now sorted by numStates first. Then later by name.
        assertEquals(1*3*5 + 0*5 + 4, config.getConfigIndex());
    }
    
    @Test
    public void testGetConfigIndexOfSubset() {

        Var w0 = new Var(VarType.PREDICTED, 2, "w0", null);
        Var w1 = new Var(VarType.PREDICTED, 5, "w1", null);
        Var w2 = new Var(VarType.PREDICTED, 3, "w2", null);
        Var w3 = new Var(VarType.PREDICTED, 3, "w3", null);
        Var w4 = new Var(VarType.PREDICTED, 6, "w4", null);
        
        VarConfig config = new VarConfig();
        config.put(w0, 1);
        config.put(w1, 4);
        config.put(w2, 1);
        config.put(w3, 2);
        config.put(w4, 2);
        
        VarSet vars = new VarSet(w0, w1, w3);
        
        assertEquals(1+4*2+2*10, config.getConfigIndexOfSubset(vars));
    }

    @Test
    public void testGetConfigIndex2() {
        Var w0 = new Var(VarType.PREDICTED, 2, "w0", null);
        Var w2 = new Var(VarType.PREDICTED, 3, "w2", null);
        Var w1 = new Var(VarType.PREDICTED, 5, "w1", null);
        
        VarConfig config = new VarConfig();
        config.put(w0, 0);
        config.put(w1, 0);
        config.put(w2, 0);


        HashSet<Integer> set = new HashSet<Integer>();
        for (int a = 0; a < 2; a++) {
            config.put(w0, a);
            for (int c = 0; c < 3; c++) {
                config.put(w2, c);
                for (int b = 0; b < 5; b++) {
                    config.put(w1, b);
                    int configIndex = a * 3*5 + c *5 + b;
                    System.out.println(configIndex);
                    assertEquals(configIndex, config.getConfigIndex());
                    set.add(configIndex);
                    
                    // Check that this equals the result of getVarConfig().
                    VarConfig other = config.getVars().getVarConfig(configIndex);
                    assertEquals(config, other);
                }
            }
        }        
        assertEquals(30, set.size());
    }
    
    @Test
    public void testGetConfigIndex3() {

        Var w0 = new Var(VarType.PREDICTED, 2, "w0", null);
        Var w1 = new Var(VarType.PREDICTED, 5, "w1", null);
        Var w2 = new Var(VarType.PREDICTED, 3, "w2", null);
        
        VarConfig config = new VarConfig();
        config.put(w0, 1);
        config.put(w1, 4);
        config.put(w2, 2);
        
        assertEquals(config.getVars().calcNumConfigs()-1, config.getConfigIndex());
    }
}
