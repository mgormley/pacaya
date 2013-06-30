package edu.jhu.gm;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.jhu.gm.Var.VarType;

public class VarTest {

    @Test
    public void testEquals() {
        Var w0 = new Var(VarType.OBSERVED, 2, "w0", null);
        Var w1 = new Var(VarType.OBSERVED, 2, "w0", null);
        assertFalse(w0.equals(w1));
        assertEquals(w0, w0);
        assertEquals(w1, w1);
    }

}
