package edu.jhu.pacaya.sch.tasks;

import java.util.Set;
import java.util.function.DoubleConsumer;

import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.RealVector;

import edu.jhu.pacaya.sch.Schedule;
import edu.jhu.pacaya.sch.SchedulingTask;
import edu.jhu.pacaya.sch.graph.IntDiGraph;
import edu.jhu.pacaya.sch.graph.WeightedIntDiGraph;
import edu.jhu.prim.Primitives.MutableDouble;
import edu.jhu.prim.Primitives.MutableInt;

public class SumPaths implements SchedulingTask {

    private WeightedIntDiGraph g;
    private Double goldWeight;
    
    public SumPaths(WeightedIntDiGraph g, RealVector s, RealVector t) {
        this.g = g;
        goldWeight = WeightedIntDiGraph.sumWalks(g.toMatrix(), s, t);
    }
    
    public SumPaths(WeightedIntDiGraph g, Set<Integer> s, Set<Integer> t, boolean flip) {
        this(g, asVec(g, s, flip), asVec(g, t, flip));
    }

    public SumPaths(WeightedIntDiGraph g) {
        this(g, null, null);
    }
    
    /**
     * returns a real vector with 1's on for each i in s
     * if flip is true then the vector produced puts a 1.0 on all but those provided
     */
    private static RealVector asVec(WeightedIntDiGraph g, Set<Integer> s, boolean flip) {
        if (s == null) {
            return null;
        } else {
            int n = g.max();
            RealVector v = new OpenMapRealVector(n, flip ? 1.0 : 0.0);
            for (int i : g.getNodes()) {
                v.setEntry(i, flip ? 0.0 : 1.0);
            }
            return v;
        }
    }
    
    @Override
    public IntDiGraph getGraph() {
        return g;
    }

    
    /**
     * Computes the approximate sum of paths through the graph where the weight of each 
     * path is the product of edge weights along the path;
     * 
     * If consumer c is not null, it will be given the intermediate estimates as they are available
     */
    public static double approxSumPaths(WeightedIntDiGraph g, Schedule s, DoubleConsumer c) {
        return 0;
    }

    @Override
    public double score(Schedule s) {
        if (goldWeight == null) {
            goldWeight = 0.0; 
        }
        
        // accumulate the total di
        final MutableInt i = new MutableInt(0);
        final MutableDouble lastDiff = new MutableDouble(0.0);
        final MutableDouble totalDiff = new MutableDouble(0.0);
        final RealVector A= new OpenMapRealVector(g.max());
        approxSumPaths(g, s, a -> {
//            if (i.v < baselineSchedule.size()) {
//                lastDiff.v = Math.abs(a - goldWeight);
//                totalDiff.v += lastDiff.v;
//            }
        });
        return 0.0;
    }

    
}
