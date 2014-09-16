package edu.jhu.hypergraph;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * A hypergraph with all nodes and edges stored in memory.
 * 
 * @author mgormley
 */
public class MemHypergraph implements Hypergraph {
    
    public static class MemHypernode implements Hypernode {

        private String label;
        private int id;
        private List<Hyperedge> outEdges = new ArrayList<Hyperedge>();
        private List<Hyperedge> inEdges = new ArrayList<Hyperedge>();
                
        public MemHypernode(String label, int id) {
            this.label = label;
            this.id = id;
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public int getId() {
            return id;
        }

        public List<Hyperedge> getOutEdges() {
            return outEdges;
        }

        public List<Hyperedge> getInEdges() {
            return inEdges;
        }

        public String toString() {
            return label;
        }
        
    }
    
    private Hypernode root;
    private List<MemHypernode> nodes;
    private List<Hyperedge> edges;
    private boolean sorted = false;
        
    public MemHypergraph() {
        this(null, new ArrayList<MemHypernode>(), new ArrayList<Hyperedge>());
    }
    
    public MemHypergraph(Hypernode root, List<MemHypernode> nodes, List<Hyperedge> edges) {
        super();
        this.root = root;
        this.nodes = nodes;
        this.edges = edges;
        sorted = false;
        for (int i=0; i<nodes.size(); i++) {
            if (!(nodes.get(i) instanceof MemHypernode)){
                throw new IllegalStateException("Nodes must be MemHypernode");
            }
            if (i != nodes.get(i).getId()) {
                throw new IllegalArgumentException("Node ids must correspond to their order");
            }
        }
    }
    
    public MemHypernode newRoot(String label) {
        root = newNode(label);
        return (MemHypernode) root;
    }
    
    public MemHypernode newNode(String label) {
        MemHypernode n = new MemHypernode(label, nodes.size());
        nodes.add(n);
        return n;
    }

    public Hyperedge newEdge(MemHypernode headNode, MemHypernode... tailNodes) {
        StringBuilder label = new StringBuilder();
        label.append(headNode.getLabel());
        label.append("<--");
        for (MemHypernode tailNode : tailNodes) {
            label.append(tailNode.getLabel());
            label.append(",");
        }
        if (tailNodes.length > 0) {
            label.deleteCharAt(label.length()-1);
        }
        return newEdge(label.toString(), headNode, tailNodes);        
    }
    
    public Hyperedge newEdge(String label, MemHypernode headNode, MemHypernode... tailNodes) {
        Hyperedge e = new Hyperedge(edges.size(), label, headNode, tailNodes);
        edges.add(e);
        headNode.inEdges.add(e);
        for (MemHypernode tailNode : tailNodes) {
            tailNode.outEdges.add(e);
        }
        sorted = false;
        return e;
    }

    @Override
    public Hypernode getRoot() {
        return root;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public List<Hypernode> getNodes() {
        return (List) nodes;
    }

    public List<MemHypernode> getMemNodes() {
        return nodes;
    }
    
    public List<Hyperedge> getEdges() {
        return edges;
    }

    @Override
    public int getNumEdges() {
        return edges.size();
    }

    @Override
    public void applyTopoSort(HyperedgeFn fn) {
        sort();
        for (Hyperedge e : edges) {
            fn.apply(e);
        }
    }

    @Override
    public void applyRevTopoSort(HyperedgeFn fn) {
        sort();
        for (int i=edges.size()-1; i>=0; i--) {
            fn.apply(edges.get(i));
        }
    }
    
    public void sort() {
        if (!sorted) {
            edges = topoSortEdges(nodes, edges);
            sorted = true;
        }
    }
    
    public static List<MemHypernode> topoSortNodes(List<MemHypernode> nodes) {
        // L ← Empty list that will contain the sorted nodes
        LinkedList<MemHypernode> sort = new LinkedList<MemHypernode>();
        Stack<MemHypernode> unmarked = new Stack<MemHypernode>();
        unmarked.addAll(nodes);
        boolean[] tempMarks = new boolean[nodes.size()];
        boolean[] permMarks = new boolean[nodes.size()];
        // while there are unmarked nodes do
        while (unmarked.size() > 0) {
            // select an unmarked node n
            MemHypernode n = unmarked.pop();
            if (permMarks[n.getId()]) {
                continue;
            }
            // visit(n)
            visitNode1(n, sort, unmarked, tempMarks, permMarks);
        }
        return sort;
    }

    private static void visitNode1(MemHypernode n, LinkedList<MemHypernode> sort, Stack<MemHypernode> unmarked, boolean[] tempMarks,
            boolean[] permMarks) {
        // function visit(node n)
        // if n has a temporary mark then stop (not a DAG)
        if (tempMarks[n.getId()]) {
            throw new IllegalStateException("Not a DAG. Found temporarily marked node: " + n.getLabel());
        }
        // if n is not marked (i.e. has not been visited yet) then
        if (!permMarks[n.getId()]) {
            // mark n temporarily
            tempMarks[n.getId()] = true;
            // for each node m with an edge from n to m do
            for (Hyperedge e : n.outEdges) {
                // This is the set of all outgoing edges from this node.
                MemHypernode m = (MemHypernode) e.getHeadNode();
                // visit(m)
                visitNode1(m, sort, unmarked, tempMarks, permMarks);   
            }
            // mark n permanently
            tempMarks[n.getId()] = false;
            permMarks[n.getId()] = true;
            // add n to head of L
            sort.addFirst(n);
        }
    }

    public static List<Hyperedge> topoSortEdges(List<MemHypernode> nodes, List<Hyperedge> edges) {
        // L ← Empty list that will contain the sorted nodes
        LinkedList<Hyperedge> sort = new LinkedList<Hyperedge>();
        Stack<MemHypernode> unmarked = new Stack<MemHypernode>();
        unmarked.addAll(nodes);
        boolean[] tempMarks = new boolean[nodes.size()];
        boolean[] permMarks = new boolean[nodes.size()];
        // while there are unmarked nodes do
        while (unmarked.size() > 0) {
            // select an unmarked node n
            MemHypernode n = unmarked.pop();
            if (permMarks[n.getId()]) {
                continue;
            }
            // visit(n)
            visitNode2(n, sort, unmarked, tempMarks, permMarks, edges);
        }
        return sort;
    }

    private static void visitNode2(MemHypernode n, LinkedList<Hyperedge> sort, Stack<MemHypernode> unmarked, boolean[] tempMarks,
            boolean[] permMarks, List<Hyperedge> edges) {
        // function visit(node n)
        // if n has a temporary mark then stop (not a DAG)
        if (tempMarks[n.getId()]) {
            throw new IllegalStateException("Not a DAG. Found temporarily marked node: " + n.getLabel());
        }
        // if n is not marked (i.e. has not been visited yet) then
        if (!permMarks[n.getId()]) {
            // mark n temporarily
            tempMarks[n.getId()] = true;
            // for each node m with an edge from n to m do
            for (Hyperedge e : n.outEdges) {
                // This is the set of all outgoing edges from this node.
                MemHypernode m = (MemHypernode) e.getHeadNode();
                // visit(m)
                visitNode2(m, sort, unmarked, tempMarks, permMarks, edges);   
            }
            // mark n permanently
            tempMarks[n.getId()] = false;
            permMarks[n.getId()] = true;
            // add all incoming edges to n to head of L
            for (Hyperedge e : edges) {
                if (e.getHeadNode() == n) {
                    sort.addFirst(e);
                }
            }
        }
    }

}
