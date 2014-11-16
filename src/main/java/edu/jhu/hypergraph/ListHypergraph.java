package edu.jhu.hypergraph;

import java.util.List;

/**
 * Hypergraph which stores its edge list in topo order explicitly.
 * @author mgormley
 */
public class ListHypergraph implements Hypergraph {

    private Hypernode root;
    private List<Hypernode> nodes;
    private List<Hyperedge> edges;
      
    protected ListHypergraph() { }
    
    public ListHypergraph(Hypernode root, List<Hypernode> nodes, List<Hyperedge> edges) {
        super();
        this.root = root;
        this.nodes = nodes;
        this.edges = edges;
    }

    @Override
    public Hypernode getRoot() {
        return root;
    }

    @Override
    public List<Hypernode> getNodes() {
        return nodes;
    }

    @Override
    public int getNumEdges() {
        return edges.size();
    }

    @Override
    public void applyTopoSort(HyperedgeFn fn) {
        for (int i=0; i<edges.size(); i++) {
            fn.apply(edges.get(i));
        }
    }

    @Override
    public void applyRevTopoSort(HyperedgeFn fn) {
        for (int i=edges.size()-1; i>=0; i--) {
            fn.apply(edges.get(i));
        }        
    }

}
