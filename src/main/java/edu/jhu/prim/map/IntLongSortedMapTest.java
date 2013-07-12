package edu.jhu.prim.map;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Test;

public class IntLongSortedMapTest {

	@Test
	public void testOrderedUsage() {
		IntLongMap map = new IntLongSortedMap();
		map.put(1, toLong(11));
		map.put(2, toLong(22));
		map.put(3, toLong(33));
		
		assertEquals(11, toInt(map.get(1)));
		assertEquals(22, toInt(map.get(2)));
		assertEquals(33, toInt(map.get(3)));
	}
	
	@Test
	public void testNormalUsage() {
		IntLongMap map = new IntLongSortedMap();
		map.put(2, toLong(22));
		map.put(1, toLong(11));
		map.put(3, toLong(33));
		map.put(-1, toLong(-11));
		map.put(8, toLong(88));
		map.put(6, toLong(66));

		assertEquals(33, toInt(map.get(3)));		
		assertEquals(11, toInt(map.get(1)));
		assertEquals(-11, toInt(map.get(-1)));
		assertEquals(22, toInt(map.get(2)));
		assertEquals(88, toInt(map.get(8)));
		assertEquals(66, toInt(map.get(6)));
		
		// Clear the map.
		map.clear();
		
		map.put(3, toLong(33));
		map.put(2, toLong(22));
		map.put(1, toLong(11));
		
		assertEquals(22, toInt(map.get(2)));
		assertEquals(11, toInt(map.get(1)));
		assertEquals(33, toInt(map.get(3)));
	}


	@Test
	public void testRemove() {
		// First element.
		IntLongMap map = new IntLongSortedMap();
		map.put(2, toLong(22));
		map.put(1, toLong(11));
		assertEquals(22, toInt(map.get(2)));
		assertEquals(11, toInt(map.get(1)));
		
		map.remove(1);
		assertEquals(false, map.contains(1));
		assertEquals(22, toInt(map.get(2)));
		assertEquals(1, map.size());
		
		// Middle element.
		map = new IntLongSortedMap();
		map.put(2, toLong(22));
		map.put(3, toLong(33));
		map.put(1, toLong(11));
		assertEquals(22, toInt(map.get(2)));
		assertEquals(11, toInt(map.get(1)));
		assertEquals(33, toInt(map.get(3)));		
		
		map.remove(2);
		assertEquals(false, map.contains(2));
		assertEquals(11, toInt(map.get(1)));
		assertEquals(33, toInt(map.get(3)));		
		assertEquals(2, map.size());
		
		// Last element.
		map = new IntLongSortedMap();
		map.put(2, toLong(22));
		map.put(3, toLong(33));
		map.put(1, toLong(11));
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
		IntLongMap map = new IntLongSortedMap();

		try {
			map.get(2);
		} catch(Exception e) {
			// pass
		}
		map.put(3, toLong(33));
		try {
			map.get(-3);
		} catch(Exception e) {
			// pass
		}
	}

    @Test
    public void testIterator() {
        IntLongSortedMap map = new IntLongSortedMap();
        map.put(2, toLong(22));
        map.put(1, toLong(11));
        
        IntLongEntry cur;
        Iterator<IntLongEntry> iter = map.iterator();
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
    
    private int toInt(long d) {
        return (int)d;
    }

    private long[] toLongs(int... b) {
        long[] a = new long[b.length];
        for (int i=0; i<b.length; i++) {
            a[i] = b[i];
        }
        return a;
    }

    private long toLong(int i) {
        return i;
    }
    
}
