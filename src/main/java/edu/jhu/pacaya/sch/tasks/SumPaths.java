package edu.jhu.pacaya.sch.tasks;

import static edu.jhu.pacaya.sch.util.Indexed.enumerate;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.DoubleConsumer;

import org.apache.commons.math3.linear.RealVector;

import edu.jhu.pacaya.sch.Schedule;
import edu.jhu.pacaya.sch.SchedulingTask;
import edu.jhu.pacaya.sch.graph.DiEdge;
import edu.jhu.pacaya.sch.graph.IntDiGraph;
import edu.jhu.pacaya.sch.graph.WeightedIntDiGraph;
import edu.jhu.pacaya.sch.util.DefaultDict;
import edu.jhu.pacaya.sch.util.Indexed;
import edu.jhu.pacaya.sch.util.ScheduleUtils;
import edu.jhu.prim.bimap.IntObjectBimap;
import edu.jhu.prim.tuple.Pair;

public class SumPaths implements SchedulingTask {

    private RealVector startWeight;
    private RealVector endWeight;
    private WeightedIntDiGraph wg;
    private IntDiGraph edgeGraph;
    private IntObjectBimap<DiEdge> edgesToNodes;
    private double lambda;
    private double regStrength;
    private Double goldWeight;
    private int haltAction;

    public SumPaths(WeightedIntDiGraph wg, RealVector s, RealVector t, double lambda, double regStrength) {
        this.lambda = lambda;
        this.regStrength = regStrength;
        startWeight = s;
        endWeight = t;
        this.wg = wg;
        Pair<IntDiGraph, IntObjectBimap<DiEdge>> p = wg.edgeGraph(false);
        edgeGraph = p.get1();
        haltAction = edgeGraph.max() + 1;
        // TODO: hook the halt action in
        edgeGraph.addNode(haltAction);
        this.edgesToNodes = p.get2();
        this.goldWeight = WeightedIntDiGraph.sumWalks(wg.toMatrix(), s, t);
    }

    /*
     * public SumPaths(WeightedIntDiGraph g, Set<Integer> s, Set<Integer> t,
     * boolean flip) { this(g, asVec(g, s, flip), asVec(g, t, flip)); }
     * 
     * public SumPaths(WeightedIntDiGraph g) { this(g, null, null); }
     */

    /**
     * returns a real vector with 1's on for each i in s if flip is true then
     * the vector produced puts a 1.0 on all but those provided
     */
    /*
     * private static RealVector asVec(WeightedIntDiGraph g, Set<Integer> s,
     * boolean flip) { if (s == null) { return null; } else { int n = g.max();
     * RealVector v = new OpenMapRealVector(n, flip ? 1.0 : 0.0); for (int i :
     * g.getNodes()) { v.setEntry(i, flip ? 0.0 : 1.0); } return v; } }
     */

    @Override
    public IntDiGraph getGraph() {

        // the nodes in the new graph are the edges of the old
        // the edges
        return wg;
    }

    public static class RecordingDoubleConsumer implements DoubleConsumer {
        private List<Double> record;

        public RecordingDoubleConsumer() {
            record = new LinkedList<Double>();
        }

        @Override
        public void accept(double value) {
            record.add(value);
        }

        public List<Double> getRecord() {
            return record;
        }
    }

    private class EvaluatingDoubleConsumer implements DoubleConsumer {
        private int haltTime;
        private double lambda;
        private double gold;
        private double reward;
        private double usedProbMass;
        private double regStrength;
        private double lastReward;
        private int i;
        
        
        public EvaluatingDoubleConsumer(double gold, double lambda, int haltTime, double regStrength) {
            this.haltTime = haltTime;
            this.lambda = lambda;
            this.gold = gold;
            this.regStrength = regStrength;
            usedProbMass = 0.0;
            reward = 0;
            lastReward = 0;
            i = 0;
        }

        @Override
        public void accept(double value) {
            // at each step, we compute the reward of stopping at that time step
            // with the current value given the known target value;
            //
            double accuracy = -Math.abs(value - gold);
            double currentReward = accuracy - lambda * i;
            // TODO: compute probability of halt under a distribution parameterized by the haltTime and the regStrength (something like a discrete truncated gaussian with mean at haltTime
            double pHalt = (i == haltTime) ? 1.0 : 0.0;
            usedProbMass += pHalt;
            reward += pHalt * currentReward;
            i++;

        }

        public double getScore() {
            return reward + lastReward * (1.0 - usedProbMass);
        }
    }
    
    /**
     * Computes the approximate sum of paths through the graph where the weight
     * of each path is the product of edge weights along the path;
     * 
     * If consumer c is not null, it will be given the intermediate estimates as
     * they are available
     */
    public static double approxSumPaths(WeightedIntDiGraph g, RealVector startWeights, RealVector endWeights,
            Iterator<DiEdge> seq, DoubleConsumer c) {
        // we keep track of the total weight of discovered paths ending along
        // each edge and the total weight
        // of all paths ending at each node (including the empty path); on each
        // time step, we
        // at each step, we pick an edge (s, t), update the sum at s, and extend
        // each of those (including
        // the empty path starting there) with the edge (s, t)

        DefaultDict<DiEdge, Double> prefixWeightsEndingAt = new DefaultDict<DiEdge, Double>(Void -> 0.0);

        // we'll maintain node sums and overall sum with subtraction rather than
        // re-adding (it's an approximation anyway!)
        RealVector currentSums = startWeights.copy();
        double currentTotal = currentSums.dotProduct(endWeights);
        if (c != null) {
            c.accept(currentTotal);
        }
        for (DiEdge e : ScheduleUtils.iterable(seq)) {
            int s = e.get1();
            int t = e.get2();
            // compute the new sums
            double oldTargetSum = currentSums.getEntry(t);
            double oldEdgeSum = prefixWeightsEndingAt.get(e);
            // new edge sum is the source sum times the edge weight
            double newEdgeSum = currentSums.getEntry(s) * g.getWeight(e);
            // new target sum is the old target sum plus the difference between
            // the new and old edge sums
            double newTargetSum = oldTargetSum + (newEdgeSum - oldEdgeSum);
            // the new total is the old total plus the difference in new and
            // target
            double newTotal = currentTotal + (newTargetSum - oldTargetSum) * endWeights.getEntry(t);
            // store the new sums
            prefixWeightsEndingAt.put(e, newEdgeSum);
            currentSums.setEntry(t, newTargetSum);
            currentTotal = newTotal;
            // and report the new total to the consumer
            if (c != null) {
                c.accept(currentTotal);
            }
        }
        return currentTotal;
    }

    private Pair<Iterator<DiEdge>, Integer> filterOutStopTime(Schedule s) {
        int haltTime = -1;
        List<DiEdge> nonStopActions = new LinkedList<>();
        for (Indexed<Integer> a : enumerate(s)) {
            if (a.get() == haltAction) {
                assert haltTime < 0;
                haltTime = a.index();
            } else {
                nonStopActions.add(edgesToNodes.lookupObject(a.get()));
            }
        }
        if (haltTime == -1) {
            haltTime = nonStopActions.size();
        }
        return new Pair<>(nonStopActions.iterator(), haltTime);
    }
    
    @Override
    public double score(Schedule s) {
        if (goldWeight == null) {
            goldWeight = 0.0; 
        }
        Pair<Iterator<DiEdge>, Integer> p = filterOutStopTime(s);
        // record the diffs
        EvaluatingDoubleConsumer eval = new EvaluatingDoubleConsumer(goldWeight, lambda, p.get2(), regStrength);
        approxSumPaths(wg, startWeight, endWeight, p.get1(), eval);
        return eval.getScore();
    }

}
