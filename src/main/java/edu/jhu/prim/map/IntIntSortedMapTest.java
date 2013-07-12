package edu.jhu.prim.map;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Test;

public class IntIntSortedMapTest {

	@Test
	public void testOrderedUsage() {
		IntIntMap map = new IntIntSortedMap();
		map.put(1, toInt(11));
		map.put(2, toInt(22));
		map.put(3, toInt(33));
		
		assertEquals(11, toInt(map.get(1)));
		assertEquals(22, toInt(map.get(2)));
		assertEquals(33, toInt(map.get(3)));
	}
	
	@Test
	public void testNormalUsage() {
		IntIntMap map = new IntIntSortedMap();
		map.put(2, toInt(22));
		map.put(1, toInt(11));
		map.put(3, toInt(33));
		map.put(-1, toInt(-11));
		map.put(8, toInt(88));
		map.put(6, toInt(66));

		assertEquals(33, toInt(map.get(3)));		
		assertEquals(11, toInt(map.get(1)));
		assertEquals(-11, toInt(map.get(-1)));
		assertEquals(22, toInt(map.get(2)));
		assertEquals(88, toInt(map.get(8)));
		assertEquals(66, toInt(map.get(6)));
		
		// Clear the map.
		map.clear();
		
		map.put(3, toInt(33));
		map.put(2, toInt(22));
		map.put(1, toInt(11));
		
		assertEquals(22, toInt(map.get(2)));
		assertEquals(11, toInt(map.get(1)));
		assertEquals(33, toInt(map.get(3)));
	}


	@Test
	public void testRemove() {
		// First element.
		IntIntMap map = new IntIntSortedMap();
		map.put(2, toInt(22));
		map.put(1, toInt(11));
		assertEquals(22, toInt(map.get(2)));
		assertEquals(11, toInt(map.get(1)));
		
		map.remove(1);
		assertEquals(false, map.contains(1));
		assertEquals(22, toInt(map.get(2)));
		assertEquals(1, map.size());
		
		// Middle element.
		map = new IntIntSortedMap();
		map.put(2, toInt(22));
		map.put(3, toInt(33));
		map.put(1, toInt(11));
		assertEquals(22, toInt(map.get(2)));
		assertEquals(11, toInt(map.get(1)));
		assertEquals(33, toInt(map.get(3)));		
		
		map.remove(2);
		assertEquals(false, map.contains(2));
		assertEquals(11, toInt(map.get(1)));
		assertEquals(33, toInt(map.get(3)));		
		assertEquals(2, map.size());
		
		// Last element.
		map = new IntIntSortedMap();
		map.put(2, toInt(22));
		map.put(3, toInt(33));
		map.put(1, toInt(11));
		assertEquals(22, toInt(map.get(2)));
		assertEquals(11, toInt(map.get(1)));
		assertEquals(33, toInt(map.get(3)));		
		
		map.remove(3);
		assertEquals(false, map.contains(3));
		assertEquals(11, toInt(map.get(1)));
		assertEquals(22, toInt(map.get(2)));		
		assertEquals(2, map.size());
	}

	@Test
	public void testBadGets() {
		IntIntMap map = new IntIntSortedMap();

		try {
			map.get(2);
		} catch(Exception e) {
			// pass
		}
		map.put(3, toInt(33));
		try {
			map.get(-3);
		} catch(Exception e) {
			// pass
		}
	}

    @Test
    public void testIterator() {
        IntIntSortedMap map = new IntIntSortedMap();
        map.put(2, toInt(22));
        map.put(1, toInt(11));
        
        IntIntEntry cur;
        Iterator<IntIntEntry> iter = map.iterator();
        assertEquals(true, iter.hasNext()); 
        assertEquals(true, iter.hasNext()); 
        cur = iter.next();
        assertEquals(1, cur.index()); 
        assertEquals(11, toInt(cur.get())); 
        assertEquals(true, iter.hasNext()); 
        cur = iter.next();
        assertEquals(2, cur.index()); 
        assertEquals(22, toInt(cur.get())); 
        assertEquals(false, iter.hasNext());
    }
    
    private int toInt(int d) {
        return (int)d;
    }

    private int[] toInts(int... b) {
        int[] a = new int[b.length];
        for (int i=0; i<b.length; i++) {
            a[i] = b[i];
        }
        return a;
    }

}
