package edu.jhu.gm.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import edu.jhu.gm.model.Var.VarType;

public class VarTest {

    // TODO: Remove
//  @Test
//  public void testEquals() {
//      Var w0 = new Var(VarType.OBSERVED, 2, "w0", null);
//      Var w1 = new Var(VarType.OBSERVED, 2, "w0", null);
//      assertFalse(w0.equals(w1));
//      assertEquals(w0, w0);
//      assertEquals(w1, w1);
//  }

    @Test
    public void testEquals() {
        Var w0 = new Var(VarType.OBSERVED, 2, "w0", null);
        Var w1 = new Var(VarType.OBSERVED, 2, "w1", null);
        Var w0Copy = new Var(VarType.OBSERVED, 2, "w0", null);
        assertFalse(w0.equals(w1));
        assertEquals(w0, w0);
        assertEquals(w1, w1);
        assertEquals(w0, w0Copy);
    }

}
