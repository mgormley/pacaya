package edu.jhu.pacaya.sch.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test; 

public class DefaultDictTest {

    @Test
    public void test() {
        DefaultDict<Integer, List<Integer>> d = new DefaultDict<>(i -> new LinkedList<>());
        // default add
        d.get(5).add(8);
        assertEquals(1,                d.size());
        assertEquals(Arrays.asList(8), d.get(5));

        // access already there
        d.get(5);
        assertEquals(1,                d.size());
        assertEquals(Arrays.asList(8), d.get(5));

        // access already there and add
        d.get(5).add(10);
        assertEquals(1,                    d.size());
        assertEquals(Arrays.asList(8, 10), d.get(5));

        // access new 
        d.get(6);
        assertEquals(2                   , d.size());
        assertEquals(Arrays.asList(8, 10), d.get(5));
        assertEquals(Arrays.asList()     , d.get(6));

        // explicit put
        d.put(7, new LinkedList<Integer>(Arrays.asList(1,2,3)));
        assertEquals(3                     , d.size());
        assertEquals(Arrays.asList(8, 10)  , d.get(5));
        assertEquals(Arrays.asList()       , d.get(6));
        assertEquals(Arrays.asList(1, 2, 3), d.get(7));
        
        // get or default
        d.getOrDefault(4,  new LinkedList<>()).add(9);
        assertEquals(3                     , d.size()                              );
        assertEquals(Arrays.asList(8, 10)  , d.get(5)                              );
        assertEquals(Arrays.asList()       , d.get(6)                              );
        assertEquals(Arrays.asList(1, 2, 3), d.get(7)                              );
        assertEquals(Arrays.asList()       , d.getOrDefault(4,  new LinkedList<>()));

        // get or default
        d.get(4).add(9);
        assertEquals(4                     , d.size()               );
        assertEquals(Arrays.asList(8, 10)  , d.get(5)               );
        assertEquals(Arrays.asList()       , d.get(6)               );
        assertEquals(Arrays.asList(1, 2, 3), d.get(7)               );
        assertEquals(Arrays.asList(9)      , d.get(4)               );
        assertEquals(Arrays.asList(8, 10)  , d.getOrDefault(5, null));
        assertEquals(Arrays.asList()       , d.getOrDefault(6, null));
        assertEquals(Arrays.asList(1, 2, 3), d.getOrDefault(7, null));
        assertEquals(Arrays.asList(9)      , d.getOrDefault(4, null));

        // remove
        d.remove(4);
        assertEquals(3                     , d.size()                              );
        assertEquals(Arrays.asList(8, 10)  , d.get(5)                              );
        assertEquals(Arrays.asList()       , d.get(6)                              );
        assertEquals(Arrays.asList(1, 2, 3), d.get(7)                              );
        assertEquals(Arrays.asList()       , d.getOrDefault(4,  new LinkedList<>()));

        // try add
        d.add(10);
        assertEquals(4                     , d.size()                              );
        assertEquals(Arrays.asList(8, 10)  , d.get(5)                              );
        assertEquals(Arrays.asList()       , d.get(6)                              );
        assertEquals(Arrays.asList(1, 2, 3), d.get(7)                              );
        assertEquals(Arrays.asList()       , d.getOrDefault(4,  new LinkedList<>()));
        assertEquals(Arrays.asList()       , d.get(10)                             );

        // try it again
        d.add(10);
        // try contains
        assertTrue(d.containsKey(5));
        assertFalse(d.containsKey(21));
        assertTrue(d.containsValue(Arrays.asList(8, 10)));
        assertFalse(d.containsValue(Arrays.asList(9, 10)));
        // make sure we didn't change anything
        assertEquals(4                     , d.size()                              );
        assertEquals(Arrays.asList(8, 10)  , d.get(5)                              );
        assertEquals(Arrays.asList()       , d.get(6)                              );
        assertEquals(Arrays.asList(1, 2, 3), d.get(7)                              );
        assertEquals(Arrays.asList()       , d.getOrDefault(4,  new LinkedList<>()));
        assertEquals(Arrays.asList()       , d.get(10)                             );

        // test copy constructor
        DefaultDict<Integer, List<Integer>> d2 = new DefaultDict<>(d, a -> new LinkedList<>(a));
        assertEquals(4                     , d2.size()                              );
        assertEquals(Arrays.asList(8, 10)  , d2.get(5)                              );
        assertEquals(Arrays.asList()       , d2.get(6)                              );
        assertEquals(Arrays.asList(1, 2, 3), d2.get(7)                              );
        assertEquals(Arrays.asList()       , d2.getOrDefault(4,  new LinkedList<>()));
        assertEquals(Arrays.asList()       , d2.get(10)                             );

        // add to d2 and shouldn't change d
        d2.get(10).add(11);
        assertEquals(4                     , d2.size()                              );
        assertEquals(Arrays.asList(8, 10)  , d2.get(5)                              );
        assertEquals(Arrays.asList()       , d2.get(6)                              );
        assertEquals(Arrays.asList(1, 2, 3), d2.get(7)                              );
        assertEquals(Arrays.asList()       , d2.getOrDefault(4,  new LinkedList<>()));
        assertEquals(Arrays.asList(11)     , d2.get(10)                             );

        // test default copy constructor
        DefaultDict<Integer, List<Integer>> d3 = new DefaultDict<>(d);
        assertEquals(4                     , d3.size()                              );
        assertEquals(Arrays.asList(8, 10)  , d3.get(5)                              );
        assertEquals(Arrays.asList()       , d3.get(6)                              );
        assertEquals(Arrays.asList(1, 2, 3), d3.get(7)                              );
        assertEquals(Arrays.asList()       , d3.getOrDefault(4,  new LinkedList<>()));
        assertEquals(Arrays.asList()       , d3.get(10)                             );

        // add to existing element of d3 and will also change d
        d3.get(10).add(12);
        // but the new element will only show up in d3
        d3.add(15);
        assertEquals(4                     , d2.size()                              );
        assertEquals(Arrays.asList(8, 10)  , d2.get(5)                              );
        assertEquals(Arrays.asList()       , d2.get(6)                              );
        assertEquals(Arrays.asList(1, 2, 3), d2.get(7)                              );
        assertEquals(Arrays.asList()       , d2.getOrDefault(4,  new LinkedList<>()));
        assertEquals(Arrays.asList(11)     , d2.get(10)                             );
        assertEquals(5                     , d3.size()                              );
        assertEquals(Arrays.asList(8, 10)  , d3.get(5)                              );
        assertEquals(Arrays.asList()       , d3.get(6)                              );
        assertEquals(Arrays.asList(1, 2, 3), d3.get(7)                              );
        assertEquals(Arrays.asList()       , d3.getOrDefault(4,  new LinkedList<>()));
        assertEquals(Arrays.asList(12)     , d3.get(10)                             );
        assertEquals(Arrays.asList()       , d3.get(15)                            );
        assertEquals(4                     , d.size()                              );
        assertEquals(Arrays.asList(8, 10)  , d.get(5)                              );
        assertEquals(Arrays.asList()       , d.get(6)                              );
        assertEquals(Arrays.asList(1, 2, 3), d.get(7)                              );
        assertEquals(Arrays.asList()       , d.getOrDefault(4,  new LinkedList<>()));
        assertEquals(Arrays.asList(12)     , d.get(10)                             );

    }

}
