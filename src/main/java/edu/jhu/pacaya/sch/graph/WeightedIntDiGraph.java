package edu.jhu.pacaya.sch.graph;

import static edu.jhu.pacaya.sch.graph.DiEdge.edge;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.function.Function;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.MathArrays;

public class WeightedIntDiGraph extends IntDiGraph {
    private HashMap<DiEdge, Double> weights;
    private Function<DiEdge, Double> makeDefaultWeight;

    public WeightedIntDiGraph() {
        this(Void -> 0.0);
    }

    public WeightedIntDiGraph(Function<DiEdge, Double> makeDefaultWeight) {
        super();
        weights = new HashMap<>();
    }

    @Override
    public void addEdge(int s, int t) {
        addEdge(s, t, makeDefaultWeight.apply(edge(s, t)));
    }

    /**
     * Adds the edge with the given weight
     */
    void addEdge(int s, int t, double w) {
        super.addEdge(s, t);
        weights.put(edge(s, t), w);
    }

    private void assertEdge(DiEdge e) {
        if (!getEdges().contains(e)) {
            throw new IndexOutOfBoundsException(String.format("missing edge: %s", e.toString()));
        }
    }

    /**
     * returns the weight along the given edge or throws and exception if edge
     * not present
     */
    double getWeight(int s, int t) {
        DiEdge e = edge(s, t);
        assertEdge(e);
        return weights.get(e);
    }

    /**
     * Sets the weight of the edge s->t to be w if the edge is present,
     * otherwise, an IndexOutOfBoundsException is thrown
     */
    void setWeight(int s, int t, double w) {
        DiEdge e = edge(s, t);
        assertEdge(e);
        weights.put(e, w);
    }

    public RealMatrix toMatrix() {
        int n = max();
        RealMatrix m = new OpenMapRealMatrix(n, n);
        for (Entry<DiEdge, Double> entry : weights.entrySet()) {
            DiEdge e = entry.getKey();
            double w = entry.getValue();
            m.setEntry(e.get1(), e.get2(), w);
        }
        return m;
    }

    /**
     * @return a new array of length d filled with ones
     */
    public static double[] ones(int d) {
        double[] a = new double[d];
        Arrays.fill(a, 1.0);
        return a;
    }
    
    public static double sumWalks(RealMatrix m, RealVector s, RealVector t) {
        LUDecomposition d = new LUDecomposition(m);
        RealMatrix invM = d.getSolver().getInverse();
        if (s == null) s = new ArrayRealVector(m.getColumnDimension(), 1.0);
        if (t == null) t = new ArrayRealVector(m.getRowDimension(), 1.0);
        return invM.preMultiply(s).dotProduct(t);
    }

    public static double computeSpectralNorm(RealMatrix m) {
        EigenDecomposition decomp = new EigenDecomposition(m);
        return Arrays.stream(decomp.getRealEigenvalues()).map(ev -> Math.abs(ev)).max().getAsDouble();
    }

}
