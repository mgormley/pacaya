package edu.jhu.pacaya.sch.util;

import static edu.jhu.pacaya.sch.util.TestUtils.checkThrows;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class OrderedSetTest {

    @Test
    public void test() {
        OrderedSet<Integer> s = new OrderedSet<Integer>();
        // new set should be empty
        assertTrue(s.isEmpty());
        assertEquals(0, s.size());

        assertTrue(s.add(4));
        assertTrue(s.add(10));
        assertTrue(s.containsAll(Arrays.asList(4, 10, 10)));
        assertFalse(s.containsAll(Arrays.asList(4, 10, 11)));
        assertTrue(s.containsAll(Arrays.asList()));
        assertEquals(Arrays.asList(4, 10), s.subList(0, 2));
        assertEquals(Arrays.asList(10), s.subList(1, 2));
        assertEquals((Integer) 10, s.listIterator(1).next());
        assertEquals((Integer) 4, s.listIterator(0).next());

        assertEquals(Arrays.asList(4, 10).toString(), s.toString());
        assertArrayEquals(Arrays.asList(4, 10).toArray(), s.toArray());
        Integer[] expected = new Integer[] { 4, 10 };
        Integer[] observed = new Integer[2];
        s.toArray(observed);
        assertArrayEquals(Arrays.asList(4, 10).toArray(), s.toArray(observed));
        assertArrayEquals(expected, observed);

        assertEquals((Integer)10, s.get(1));
        assertEquals((Integer)4, s.get(0));
        assertEquals(1, s.indexOf(10));
        assertEquals(0, s.indexOf(4));
        assertEquals(-1, s.indexOf(20));
        assertEquals(1, s.lastIndexOf(10));
        assertEquals(0, s.lastIndexOf(4));
        assertEquals(-1, s.lastIndexOf(20));
        
        OrderedSet<Integer> s2 = new OrderedSet<Integer>(s);
        assertTrue(s.addAll(Arrays.asList(1, 2, 3, 10, 15, 20, 21)));
        assertFalse(s.addAll(Arrays.asList(1, 2, 3, 10, 15, 20, 21)));
        assertTrue(s.add(6));
        assertFalse(s.add(4));
        assertFalse(s.add(2));

        
        OrderedSet<Integer> s3 = new OrderedSet<Integer>(s);
        // Shouldn't be able to call remove with and int (this invokes the
        // remove at index function)
        assertTrue(checkThrows(() -> s3.remove(2), UnsupportedOperationException.class));
        assertTrue(checkThrows(() -> s3.add(0, 2), UnsupportedOperationException.class));
        assertTrue(checkThrows(() -> s3.addAll(0, Arrays.asList()), UnsupportedOperationException.class));
        assertTrue(checkThrows(() -> s3.set(0, 10), UnsupportedOperationException.class));

        // working remove
        assertTrue(s3.remove((Integer) 2));

        // removing not present element
        assertFalse(s3.remove((Integer) 0));

        // some are present; some are not; some are duplicates
        assertTrue(s3.removeAll(Arrays.asList(0, 15, 15, 21, 0)));
        // doesn't include 10:
        assertTrue(s3.retainAll(Arrays.asList(0, 3, 4, 3, 5, 6, 20, 0, 50, 21)));

        // not there
        assertTrue(s3.add(45));
        // not there
        assertTrue(s3.add(2));
        // there
        assertFalse(s3.add(3));

        List<Integer> expected1 = Arrays.asList(4, 10, 1, 2, 3, 15, 20, 21, 6);
        List<Integer> expected2 = Arrays.asList(4, 10);
        List<Integer> expected3 = Arrays.asList(4, 3, 20, 6, 45, 2);
        assertEquals(9, s.size());
        assertEquals(2, s2.size());
        assertEquals(6, s3.size());

        OrderedSet<Integer> s4 = new OrderedSet<Integer>(s3);
        s4.clear();
        assertTrue(TestUtils.checkEquals(
                new Object[][] { { s, new OrderedSet<Integer>(s), new OrderedSet<Integer>(expected1), expected1 },
                        { s2, new OrderedSet<Integer>(expected2), expected2 },
                        { s3, new OrderedSet<Integer>(expected3), expected3 },

        }));
        s.clear();
        assertEquals(0, s.size());
        assertTrue(s.isEmpty());
        assertTrue(TestUtils.checkEquals(new Object[][] {
                { s, new OrderedSet<Integer>(s), new OrderedSet<Integer>(Arrays.asList()), Arrays.asList() } }));
    }

}
