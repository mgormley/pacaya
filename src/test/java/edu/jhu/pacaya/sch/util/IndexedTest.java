package edu.jhu.pacaya.sch.util;

import static edu.jhu.pacaya.sch.util.Indexed.collect;
import static edu.jhu.pacaya.sch.util.Indexed.enumerate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import edu.jhu.prim.tuple.Pair; 

public class IndexedTest {

    @Test
    public void testEnumerate() {
        List<String> strings = Arrays.asList("this", "is", "a", "test");
        List<Object> indexesAndVals = new ArrayList<>();
        for (Indexed<String> e : enumerate(strings)) {
            indexesAndVals.add(e.index());
            indexesAndVals.add(e.get());
        }
        List<Object> expected = Arrays.asList(0, "this", 1, "is", 2, "a", 3, "test");
        assertEquals(expected, indexesAndVals);
    }

    @Test
    public void testCollect() {
        List<String> strings = Arrays.asList("this", "is", "a", "test");
        List<Indexed<String>> collected= new ArrayList<>(collect(enumerate(strings)));
        List<Indexed<String>> expected = Arrays.asList(
                new Indexed<String>("this", 0),
                new Indexed<String>("is", 1),
                new Indexed<String>("a", 2),
                new Indexed<String>("test", 3));
        assertEquals(expected, collected);
    }

    @Test
    public void testEquals() {
        String s1 = "this";
        String s2 = "is";
        List<String> strings = Arrays.asList(s1, s2, "a", "test");
        List<Indexed<String>> collected = new ArrayList<>(collect(enumerate(strings)));
        TestUtils.testEquals(new Object[][] {
            { collected, new ArrayList<>(collect(enumerate(strings))) },
            { null },
            { collected.get(0), new Indexed<String>("this", 0), new Indexed<String>("this", 0), new Indexed<String>(s1, 0), new Indexed<String>(s1, 0) },
            { new Indexed<String>("this", 1), new Indexed<String>("this", 1), new Indexed<String>(s1, 1), new Indexed<String>(s1, 1) },
            { collected.get(1), new Indexed<String>("is", 1), new Indexed<String>("is", 1), new Indexed<String>(s2, 1), new Indexed<String>(s2, 1) },
            { collected.get(2) },
            { collected.get(3) },
            { new Pair<String, Integer>(s1, 0) },
        });
    }

    @Test
    public void testToString() {
        assertEquals("<this, 0>", new Indexed<>("this", 0).toString());
        assertEquals("<null, 0>", new Indexed<>(null, 0).toString());
        
    }
    
    @Test
    public void testHashing() {
        String s1 = "this";
        String s2 = "is";
        List<String> strings = Arrays.asList(s1, s2, "a", "test");
        List<Indexed<String>> collected = new ArrayList<>(collect(enumerate(strings)));
        List<Indexed<String>> collected2 = new ArrayList<>(collect(enumerate(strings)));
        HashSet<Indexed<String>> s = new HashSet<>();
        s.addAll(collected);
        s.add(new Indexed<>(null, 0));
        assertEquals(5, s.size());
        assertTrue(s.containsAll(collected2));
        assertTrue(s.contains(new Indexed<>("this", 0)));
        assertTrue(s.contains(new Indexed<>("is", 1)));
        assertTrue(s.contains(new Indexed<>("a", 2)));
        assertTrue(s.contains(new Indexed<>("test", 3)));
        assertFalse(s.contains(new Indexed<>("is", 0)));
        assertFalse(s.contains(new Indexed<>("blah", 0)));
        assertFalse(s.contains(new Indexed<>("blah", 5)));
        assertTrue(s.contains(new Indexed<>(null, 0)));
        assertFalse(s.contains(new Indexed<>(null, 1)));
    }
    
}
