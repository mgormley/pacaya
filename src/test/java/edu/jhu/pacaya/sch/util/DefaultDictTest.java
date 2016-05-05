package edu.jhu.pacaya.sch.util;

import static org.junit.Assert.assertEquals;

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
        assertEquals(4                     , d.size()                              );
        assertEquals(Arrays.asList(8, 10)  , d.get(5)                              );
        assertEquals(Arrays.asList()       , d.get(6)                              );
        assertEquals(Arrays.asList(1, 2, 3), d.get(7)                              );
        assertEquals(Arrays.asList()       , d.getOrDefault(4,  new LinkedList<>()));
        assertEquals(Arrays.asList()       , d.get(10)                             );
    }


}
