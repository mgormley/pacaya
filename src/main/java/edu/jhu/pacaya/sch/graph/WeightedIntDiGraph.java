package edu.jhu.pacaya.sch.graph;

import static edu.jhu.pacaya.sch.graph.DiEdge.edge;

import java.util.Arrays;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map.Entry;
import java.util.function.Function;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealMatrixPreservingVisitor;
import org.apache.commons.math3.linear.RealVector;

public class WeightedIntDiGraph extends IntDiGraph {
    // The default function for generating an edge weight as a function of the edge
    private final static Function<DiEdge, Double> defaultMakeDefaultWeight = Void -> 0.0; 

    // The function for this graph for generating an edge weight as a function of the edge
    private Function<DiEdge, Double> makeDefaultWeight;

    private HashMap<DiEdge, Double> weights;

    public WeightedIntDiGraph() {
        this(defaultMakeDefaultWeight);
    }

    public WeightedIntDiGraph(Function<DiEdge, Double> makeDefaultWeight) {
        super();
        weights = new HashMap<>();
        this.makeDefaultWeight = makeDefaultWeight;
    }

    /**
     * Adds the edge with the given weight
     */
    public void addEdge(int s, int t, double w) {
        addEdge(edge(s, t), w);
    }

    /**
     * Attempts to adds the edge to the graph with default weight if not already present;
     * If the edge is already present, does nothing.
     */
    @Override
    public void addEdge(DiEdge e) {
        if (!getEdges().contains(e)) { 
            super.addEdge(e);
            weights.put(e, makeDefaultWeight.apply(e));
        }
    }

    /**
     * Adds the edge with the given weight
     */
    public void addEdge(DiEdge e, double w) {
        if (!getEdges().contains(e)) { 
            super.addEdge(e);
            weights.put(e, w);
        }
    }

    /**
     * Constructs a weighted directed graph with the same nodes and edges as
     * the given unweighted graph
     */
    public static WeightedIntDiGraph fromUnweighted(IntDiGraph g) {
       
        WeightedIntDiGraph wg = new WeightedIntDiGraph();
        wg.addAll(g);
        return wg;
    }

    /**
     * Constructs a weighted directed graph with the same nodes and edges as
     * the given unweighted graph
     */
    public static WeightedIntDiGraph fromUnweighted(IntDiGraph g, Function<DiEdge, Double> makeDefaultWeight) {
        WeightedIntDiGraph wg = new WeightedIntDiGraph(makeDefaultWeight);
        wg.addAll(g);
        return wg;
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
    public double getWeight(int s, int t) {
        return getWeight(edge(s, t));
    }

    public double getWeight(DiEdge e) {
        assertEdge(e);
        return weights.get(e);
    }
    
    /**
     * Sets the weight of the edge s->t to be w if the edge is present,
     * otherwise, an IndexOutOfBoundsException is thrown
     */
    public void setWeight(int s, int t, double w) {
        DiEdge e = edge(s, t);
        assertEdge(e);
        weights.put(e, w);
    }

    public RealMatrix toMatrix() {
        int n = max() + 1;
        RealMatrix m = new OpenMapRealMatrix(n, n);
        for (Entry<DiEdge, Double> entry : weights.entrySet()) {
            DiEdge e = entry.getKey();
            double w = entry.getValue();
            m.setEntry(e.get1(), e.get2(), w);
        }
        return m;
    }

    public static WeightedIntDiGraph fromMatrix(RealMatrix m) {
        WeightedIntDiGraph g = new WeightedIntDiGraph();
        m.walkInOptimizedOrder(new RealMatrixPreservingVisitor() {
            
            @Override
            public void visit(int row, int column, double value) {
                g.addEdge(row, column, value);
            }
            
            @Override
            public void start(int rows, int columns, int startRow, int endRow, int startColumn, int endColumn) {
                // do nothing
            }
            
            @Override
            public double end() {
                // do nothing
                return 0;
            }
        });
        return g;
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
        int n = m.getColumnDimension();
        if (n != m.getRowDimension()) {
            throw new InputMismatchException("sum walks can only be computed on square matrices");
        }
        LUDecomposition d = new LUDecomposition(MatrixUtils.createRealIdentityMatrix(n).subtract(m));
        RealMatrix sum = d.getSolver().getInverse();
        if (s == null) s = new ArrayRealVector(n, 1.0);
        if (t == null) t = new ArrayRealVector(n, 1.0);
        return sum.preMultiply(s).dotProduct(t);
    }

    public static double computeSpectralNorm(RealMatrix m) {
        EigenDecomposition decomp = new EigenDecomposition(m);
        return Arrays.stream(decomp.getRealEigenvalues()).map(ev -> Math.abs(ev)).max().getAsDouble();
    }

}
