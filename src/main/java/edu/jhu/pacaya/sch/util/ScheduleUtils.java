package edu.jhu.pacaya.sch.util;

import static edu.jhu.pacaya.sch.util.Indexed.enumerate;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.google.common.collect.Lists;

import edu.jhu.pacaya.sch.Schedule;
import edu.jhu.pacaya.sch.graph.IntDiGraph;

public class ScheduleUtils {

    /**
     * Slight convenience for building pairs to avoid having to include the types when using with asList
     */
    /*
    public static <K,V> Pair<K,V> pair(K k, V v) {
        return new Pair<>(k, v);
    }
    */
    
    /**
     * return a DAG, G' = V', E' such that vertecies correspond to indexes in
     * the schedule and there is an edge (i,j) \in E' if s_i triggered s_j
     */
    public static IntDiGraph buildTriggers(IntDiGraph g, Schedule s) {
        // return trigger DAG
        IntDiGraph d = new IntDiGraph();

        // map from node to triggering indexes
        DefaultDict<Integer, List<Integer>> currentTriggers = new DefaultDict<>(i -> new LinkedList<Integer>());
        for (Indexed<Integer> s_j : enumerate(s)) {
            // add in arcs from triggers
            for (int i : currentTriggers.get(s_j.get())) {
                d.addEdge(i, s_j.index());
            }
            // remove s_j from the agenda
            currentTriggers.remove(s_j.get());
            // record that j is triggering consequents
            for (int s_k : g.getSuccessors(s_j.get())) {
                currentTriggers.get(s_k).add(s_j.index());
            }
        }
        // add a link to the unpopped version of each node still on the agenda
        // the integer will be the length of the trajectory plus an index into
        // the set of nodes
        for (Entry<Integer, List<Integer>> item : currentTriggers.entrySet()) {
            int s_k = item.getKey();
            List<Integer> triggers = item.getValue();
            for (int j : triggers) {
                d.addEdge(j, s.size() + g.index(s_k));
            }
        }
        return d;
    }
    
    /**
     * Returns an iterator cycles over the elements in items repeat times;
     * if repeat < 0, then this cycles indefinitely
     * if repeat == 0, then the iterator is an empty iterator
     */
    public static <T> Iterator<T> cycle(Iterator<T> itr, int times) {
        // if we repeat 0, then it is as if the itr were empty, so don't take the items
        final List<T> items = (times != 0) ? Lists.newLinkedList(iterable(itr)) : Collections.emptyList();
        
        return new Iterator<T>() {
            
            private Iterator<T> currentItr = Collections.emptyIterator();
            private int ncalls = 0;
            
            private Iterator<T> getItr() {
                // if this is the first call or we've gotten to the end of a round
                if (!currentItr.hasNext() && ncalls < times) {
                    currentItr = items.iterator();
                    ncalls++;
                }
                return currentItr;
            }
            
            @Override
            public boolean hasNext() {
                return getItr().hasNext();
            }

            @Override
            public T next() {
                return getItr().next();
            }
            
        };
    }

    public static <T> Iterable<T> iterable(Iterator<T> seq) {
        final MutableBoolean used = new MutableBoolean(false);
        return new Iterable<T>() {

            @Override
            public Iterator<T> iterator() {
                if (!used.booleanValue()) {
                    used.setValue(true);;
                    return seq;
                } else {
                    throw new IllegalStateException("only allowed to iterate this iterable once");
                }
            }
        };
    }

    
}
