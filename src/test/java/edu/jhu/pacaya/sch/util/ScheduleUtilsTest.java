package edu.jhu.pacaya.sch.util;

import static edu.jhu.pacaya.sch.graph.DiEdge.edge;
import static edu.jhu.pacaya.sch.graph.IntDiGraph.simpleGraphWithStart;
import static edu.jhu.pacaya.sch.util.TestUtils.toIntArray;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.common.collect.Lists;

import edu.jhu.pacaya.sch.Schedule;
import edu.jhu.pacaya.sch.graph.IntDiGraph;

public class ScheduleUtilsTest {
    
    @Test
    public void testBuildTriggers() {
        new ScheduleUtils(); // HACK: just to keep coverage happy

        IntDiGraph g = simpleGraphWithStart();
        IntDiGraph triggers = ScheduleUtils.buildTriggers(g, new Schedule(4, 0, 1, 3, 0, 1, 3, 2, 3));
        assertArrayEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).toArray(),
                triggers.getNodes().stream().sorted().toArray());
        assertEquals(
                Arrays.asList(edge(0, 1), edge(0, 3), edge(1, 2), edge(1, 7), edge(2, 3), edge(3, 4), edge(4, 5),
                        edge(4, 7), edge(5, 6), edge(6, 9), edge(7, 8), edge(8, 9)),
                triggers.getEdges().stream().sorted().collect(Collectors.toList()));
    }

    @Test
    public void testCycle() {
        assertFalse(ScheduleUtils.cycle(Arrays.asList(1, 2, 3).iterator(), 0).hasNext());
        assertEquals(
                Arrays.asList(1, 2, 3),
                Lists.newArrayList(ScheduleUtils.cycle(Arrays.asList(1, 2, 3).iterator(), 1)));
           
    }

    @Test
    public void testIterable() {
        List<Integer> ints = Arrays.asList(5,4,1,6,7);
        Iterator<Integer> intItr = ints.iterator();
        Iterable<Integer> intIterable = ScheduleUtils.iterable(intItr);
        ArrayList<Integer> copy = Lists.newArrayList(intIterable);
        assertArrayEquals(toIntArray(ints), toIntArray(copy));

        // shouldn't allow me to call iterator on the iterable more than once
        assertTrue(TestUtils.checkThrows(() -> intIterable.iterator(), IllegalStateException.class));
        
        assertFalse(ScheduleUtils.cycle(Arrays.asList(1, 2, 3).iterator(), 0).hasNext());
        assertEquals(
                Arrays.asList(1, 2, 3),
                Lists.newArrayList(ScheduleUtils.cycle(Arrays.asList(1, 2, 3).iterator(), 1)));
           
    }
    
    
}
