package edu.jhu.parse.dep.edmonds;

/**
 * Represent a complete weighted graph by using a matrix with n rows and n
 * columns, where n is the number of nodes. Each node is represented as an
 * integer between 0 and (n-1).
 * 
 * @author eraldo
 * 
 */
public final class CompleteGraph {

    /**
     * Weights of the edges.
     */
    double[][] weights;

    public CompleteGraph(int numberOfNodes) {
        weights = new double[numberOfNodes][numberOfNodes];
    }

    public CompleteGraph(double[][] weights) {
        this.weights = weights;
    }

    public int getNumberOfNodes() {
        return weights.length;
    }

    public double getEdgeWeight(int from, int to) {
        return weights[from][to];
    }

    public void setEdgeWeight(int from, int to, double weight) {
        weights[from][to] = weight;
    }
}