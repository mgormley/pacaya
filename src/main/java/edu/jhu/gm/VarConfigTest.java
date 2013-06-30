package edu.jhu.gm;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.junit.Test;

import edu.jhu.gm.Var.VarType;

public class VarConfigTest {

    @Test
    public void testGetState() {

        Var w0 = new Var(VarType.OBSERVED, 2, "w0", null);
        Var w1 = new Var(VarType.OBSERVED, 5, "w1", null);
        Var w2 = new Var(VarType.OBSERVED, 3, "w2", null);
        
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

        Var w0 = new Var(VarType.OBSERVED, 2, "w0", null);
        Var w1 = new Var(VarType.OBSERVED, 5, "w1", null);
        Var w2 = new Var(VarType.OBSERVED, 3, "w2", null);
        
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

        Var w0 = new Var(VarType.OBSERVED, 2, "w0", null);
        Var w1 = new Var(VarType.OBSERVED, 5, "w1", null);
        Var w2 = new Var(VarType.OBSERVED, 3, "w2", null);
        
        VarConfig config = new VarConfig();
        config.put(w0, 1);
        config.put(w1, 4);
        config.put(w2, 0);
        
        assertEquals(1+4*2+0*5, config.getConfigIndex());
    }

    @Test
    public void testGetConfigIndex2() {

        Var w0 = new Var(VarType.OBSERVED, 2, "w0", null);
        Var w1 = new Var(VarType.OBSERVED, 5, "w1", null);
        Var w2 = new Var(VarType.OBSERVED, 3, "w2", null);
        
        VarConfig config = new VarConfig();
        config.put(w0, 0);
        config.put(w1, 0);
        config.put(w2, 0);

        HashSet<Integer> set = new HashSet<Integer>();
        for (int c = 0; c < 3; c++) {
            config.put(w2, c);
            for (int b = 0; b < 5; b++) {
                config.put(w1, b);
                for (int a = 0; a < 2; a++) {
                    config.put(w0, a);
                    int configIndex = a * 1 + b * 2 + c * 10;
                    assertEquals(configIndex, config.getConfigIndex());
                    System.out.println(configIndex);
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

        Var w0 = new Var(VarType.OBSERVED, 2, "w0", null);
        Var w1 = new Var(VarType.OBSERVED, 5, "w1", null);
        Var w2 = new Var(VarType.OBSERVED, 3, "w2", null);
        
        VarConfig config = new VarConfig();
        config.put(w0, 1);
        config.put(w1, 4);
        config.put(w2, 2);
        
        assertEquals(config.getVars().calcNumConfigs()-1, config.getConfigIndex());
    }
}
