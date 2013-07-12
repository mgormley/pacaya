package edu.jhu.prim.set;

import static org.junit.Assert.*;

import org.junit.Test;

public class SmallSetTest {
    
    @Test
    public void testAdd() {
        SmallSet<Integer> s1 = new SmallSet<Integer>();
        s1.add(1);
        s1.add(2);
        s1.add(3);
        
        SmallSet<Integer> s2 = new SmallSet<Integer>();
        s2.add(3);
        s2.add(1);
        s2.add(2);
        s2.add(1);
        s2.add(2);
        
        assertEquals(3, s1.size());
        assertEquals(3, s2.size());
        
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    public void testSuperset() {
        SmallSet<Integer> s1 = new SmallSet<Integer>();
        s1.add(1);
        s1.add(2);
        s1.add(3);
        
        SmallSet<Integer> s2 = new SmallSet<Integer>();
        s2.add(3);
        s2.add(1);
        s2.add(2);
        s2.add(1);
        s2.add(2);
        
        assertTrue(s1.isSuperset(s2));
        
        s1.add(0);
        
        assertTrue(s1.isSuperset(s2));
        assertFalse(s2.isSuperset(s1));
    }
    

    @Test
    public void testUnion() {
        SmallSet<Integer> s1 = new SmallSet<Integer>();
        s1.add(1);
        s1.add(2);
        
        SmallSet<Integer> s2 = new SmallSet<Integer>();
        s2.add(3);
        s2.add(1);

        SmallSet<Integer> union = new SmallSet<Integer>(s1, s2);
        assertEquals(3, union.size());
        
        assertTrue(union.isSuperset(s1));
        assertTrue(union.isSuperset(s2));
        
        s2.add(2);
        assertTrue(union.equals(s2));
    }
    
}
