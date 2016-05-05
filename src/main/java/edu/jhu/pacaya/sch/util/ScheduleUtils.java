package edu.jhu.pacaya.sch.util;

import static edu.jhu.pacaya.sch.util.Indexed.enumerate;

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

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
    
}
