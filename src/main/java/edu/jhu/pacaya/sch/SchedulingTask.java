package edu.jhu.pacaya.sch;

import java.util.function.Function;

import edu.jhu.pacaya.sch.graph.IntDiGraph;

public interface SchedulingTask {

    /**
     * @return the dependency graph for the problem
     */
    public IntDiGraph getGraph();
    
    /**
     * Returns the reward of executing the provided schedule for the task
     */
    public double score(Schedule s);
    
    /**
     * Helper to make it easy to make a task from an available graph and function
     */
    public static SchedulingTask makeTask(IntDiGraph g, Function<Schedule, Double> f) {
        return new SchedulingTask() {
            @Override
            public IntDiGraph getGraph() {
                return g;
            }

            @Override
            public double score(Schedule s) {
                return f.apply(s);
            }
        };
    }
}
