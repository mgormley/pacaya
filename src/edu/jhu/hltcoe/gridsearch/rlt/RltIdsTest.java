package edu.jhu.hltcoe.gridsearch.rlt;

import org.junit.Test;

import static org.junit.Assert.*;

public class RltIdsTest {

    @Test
    public void testGetRltId() {
        RltIds ids = new RltIds(3);
        assertEquals(4, ids.get(0, 0));
        assertEquals(8, ids.get(1, 0));
        assertEquals(8, ids.get(0, 1));
        assertEquals(9, ids.get(1, 1));
        assertEquals(12, ids.get(2, 0));
        assertEquals(12, ids.get(0, 2));
        assertEquals(13, ids.get(1, 2));
        assertEquals(14, ids.get(2, 2));
    }

    @Test
    public void testGetIJ() {
        RltIds ids = new RltIds(3);
        
        assertEquals(0, ids.getI(4));
        assertEquals(0, ids.getJ(4));
        
        assertEquals(1, ids.getI(8));
        assertEquals(0, ids.getJ(8));

        assertEquals(1, ids.getI(9));
        assertEquals(1, ids.getJ(9));

        assertEquals(2, ids.getI(12));
        assertEquals(0, ids.getJ(12));   
    }
    
}
