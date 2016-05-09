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
import edu.jhu.pacaya.sch.util.dist.TruncatedNormal;
import edu.jhu.prim.bimap.IntObjectBimap;
import edu.jhu.prim.tuple.Pair;

public class SumPaths implements SchedulingTask {

    private RealVector startWeight;
    private RealVector endWeight;
    private WeightedIntDiGraph wg;
    private IntDiGraph edgeGraph;
    private IntObjectBimap<DiEdge> edgesToNodes;
    private double lambda;
    private double sigma;
    private Double goldWeight;
    private int haltAction;

    public SumPaths(WeightedIntDiGraph wg, RealVector s, RealVector t, double lambda, double sigma) {
        this.lambda = lambda;
        this.sigma = sigma;
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

    /**
     * A consumer that computes an expected reward of a sequence of 
     * observed values;  the expectation is take with respect to the actual haltTime
     * which is a TruncatedNormal random variable with the mean of the underlying Normal
     * being the anticipated haltTime and the standard deviation of the underlying Normal
     * being a parameter. The reward given a halt time is the negative absolute difference of the
     * current result from gold minus lambda times the number of time steps taken.
     *
     */
    private class EvaluatingDoubleConsumer implements DoubleConsumer {
        // the max probability halt time
        private int haltTime;
        // reward = -diff - lambda timestep
        private double lambda;
        // the target sum
        private double gold;
        // the total accumulated reward
        private double reward;
        // the amount of probabilty mass accounted for for the halt time
        private double usedProbMass;
        // the standard deviation of the underlying normal distribution over
        // actual stop time (the actual distribution that we model is a truncated version)
        private double sigma;
        // the most recent reward observed
        private double lastReward;
        // the current timestep (starts at 0)
        private int i;
        
        public EvaluatingDoubleConsumer(double gold, double lambda, int haltTime, double sigma) {
            this.haltTime = haltTime;
            this.lambda = lambda;
            this.gold = gold;
            usedProbMass = 0.0;
            reward = 0;
            lastReward = 0;
            i = 0;
        }

        public double probRemaining() {
            return 1.0 - usedProbMass;
        }
        
        /**
         * accept the value that will be available from the current time step (starting at 0)
         * and the next time step
         */
        @Override
        public void accept(double value) {
            double accuracy = -Math.abs(value - gold);
            double currentReward = accuracy - lambda * i;
            double pHalt = TruncatedNormal.probabilityTruncZero(i, i+1, haltTime, sigma);
            pHalt = Math.min(probRemaining(), pHalt);
            usedProbMass += pHalt;
            reward += pHalt * currentReward;
            i++;

        }

        public double getScore() {
            return reward + lastReward * probRemaining();
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

    // TODO: probably better to just have halt be explicit as an int rather than an action
    private Pair<Iterator<DiEdge>, Integer> filterOutStopTime(Schedule s) {
        int haltTime = -1;
        List<DiEdge> nonStopActions = new LinkedList<>();
        for (Indexed<Integer> a : enumerate(s)) {
            if (a.get() == haltAction) {
                if (haltTime > -1) {
                    throw new IllegalStateException("cannot have more than one halt action in schedule");
                }
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
        // get the sequence of edges and the halt time
        Pair<Iterator<DiEdge>, Integer> p = filterOutStopTime(s);
        // compute the expected reward
        EvaluatingDoubleConsumer eval = new EvaluatingDoubleConsumer(goldWeight, lambda, p.get2(), sigma);
        approxSumPaths(wg, startWeight, endWeight, p.get1(), eval);
        return eval.getScore();
    }

    public int haltAction() {
        return haltAction;
    }

}
