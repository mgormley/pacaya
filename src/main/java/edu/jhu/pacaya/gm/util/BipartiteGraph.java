package edu.jhu.pacaya.gm.util;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.prim.list.ByteStack;
import edu.jhu.prim.list.IntArrayList;
import edu.jhu.prim.list.IntStack;

/**
 * Undirected bipartite graph.
 * 
 * @author mgormley
 *
 */
public final class BipartiteGraph<T1, T2> {

    private static final Logger log = LoggerFactory.getLogger(BipartiteGraph.class);

    /** Nodes of type 1. */
    private final List<T1> nodes1;
    /** Nodes of type 2. */
    private final List<T2> nodes2;

    /** Edge ids of type 1 nodes. Indexed by [Position of node in nodes1 list][Neighbor index]. */
    private final int[][] edges1;
    /** Edge ids of type 2 nodes. Indexed by [Position of node in nodes2 list][Neighbor index]. */
    private final int[][] edges2;

    private final int numEdges;
    
    // Instead of constructing actual Edge objects, we store all the information for each edge in
    // the arrays below indexed by edge ID.
    private final int[] prnt;
    private final int[] chld;
    private final int[] dual;
    private final int[] iter; // TODO: Use or remove.
    
    public BipartiteGraph(List<T1> nodes1, List<T2> nodes2, EdgeList edgeList) {
        this(nodes1, nodes2, edgeList, false);
    }
    
    public BipartiteGraph(List<T1> nodes1, List<T2> nodes2, EdgeList edgeList, boolean orderEdgesByT1) {
        this.nodes1 = nodes1;
        this.nodes2 = nodes2;
        this.numEdges = edgeList.size() * 2;
        this.prnt = new int[numEdges];
        this.chld = new int[numEdges];
        this.dual = new int[numEdges];
        this.iter = new int[numEdges];
        
        // Count number of neighbors for each node.
        int[] t1NumNbs = new int[nodes1.size()];
        int[] t2NumNbs = new int[nodes2.size()];
        for (int e=0; e<edgeList.size(); e++) {
            t1NumNbs[edgeList.getN1(e)]++;
            t2NumNbs[edgeList.getN2(e)]++;
        }

        this.edges1 = new int[nodes1.size()][];
        for (int t1=0; t1<nodes1.size(); t1++) {
            edges1[t1] = new int[t1NumNbs[t1]];
        }
        this.edges2 = new int[nodes2.size()][];
        for (int t2=0; t2<nodes2.size(); t2++) {
            edges2[t2] = new int[t2NumNbs[t2]];
        }
        
        // Add edges.
        int edgeCount = 0;
        int[] t1Count = new int[nodes1.size()];
        int[] t2Count = new int[nodes2.size()];
        for (int e=0; e<edgeList.size(); e++) {
            int t1 = edgeList.getN1(e);
            int t2 = edgeList.getN2(e);
            
            // Add edge t1 --> t2.
            assert edgeCount < numEdges;
            assert edgeCount % 2 == 0 : "t1 --> t2 edges are always odd";
            edges1[t1][t1Count[t1]] = edgeCount;
            prnt[edgeCount] = t1;
            chld[edgeCount] = t2;
            dual[edgeCount] = t2Count[t2];
            iter[edgeCount] = t1Count[t1];
            edgeCount++;
            
            // Add edge t2 --> t1.
            assert edgeCount < numEdges;
            assert edgeCount % 2 == 1 : "t2 --> t1 edges are always odd";
            edges2[t2][t2Count[t2]] = edgeCount;
            prnt[edgeCount] = t2;
            chld[edgeCount] = t1;
            dual[edgeCount] = t1Count[t1];
            iter[edgeCount] = t2Count[t2];            
            edgeCount++;
            
            // Increment neighbor counters.
            t1Count[t1]++;
            t2Count[t2]++;
        }

        // TODO: test this.
        if (orderEdgesByT1) {
            int i=0;
            for (int t1=0; t1<numT1Nodes(); t1++) {
                for (int t1Nb=0; t1Nb<numNbsT1(t1); t1Nb++) {
                    int e = edges1[t1][t1Nb];
                    int t2 = chld[e];
                    int t2Nb = dual[e];
                    swapEdges(e, i++);
                    e = edges2[t2][t2Nb];
                    swapEdges(e, i++);
                }
            }
            assert edgeCount == i;
        }
    }

    private void swapEdges(int e, int f) {
        if (isT1T2(e)) {
            edges1[prnt[e]][iter[e]] = f;
        } else {
            edges2[prnt[e]][iter[e]] = f;
        }
        if (isT1T2(f)) {
            edges1[prnt[f]][iter[f]] = e;
        } else {
            edges2[prnt[f]][iter[f]] = e;
        }
        swapVals(e, f, prnt);
        swapVals(e, f, chld);
        swapVals(e, f, dual);
        swapVals(e, f, iter);
    }
    
    /** Swap the values of two positions in an array. */
    private void swapVals(int e, int f, int[] vals) {
        int vals_e = vals[e];
        vals[e] = vals[f];
        vals[f] = vals_e;
    }

    public boolean isT1T2(int e) {
        // Edge ids from type 1 to type 2 are always even.
        return e % 2 == 0;
    }

    /* ---- Indexed by Type 1 or 2 id and neighbor index ----- */

    public int numNbsT1(int t1) {
        return edges1[t1].length;
    }
    
    public int numNbsT2(int t2) {
        return edges2[t2].length;
    }
    
    /* ---- Indexed by Type 1 or 2 id and neighbor index ----- */
    
    public int edgeT1(int t1, int nb) {
        return edges1[t1][nb];
    }
    
    public int edgeT2(int t2, int nb) {
        return edges2[t2][nb];        
    }

    public int childT1(int t1, int nb) {
        return chld[edges1[t1][nb]];
    }

    public int childT2(int t2, int nb) {
        return chld[edges2[t2][nb]];
    }
    
    public int dualT1(int t1, int nb) {
        return dual[edges1[t1][nb]];
    }

    public int dualT2(int t2, int nb) {
        return dual[edges2[t2][nb]];
    }

    // TODO: test this.
    public int opposingT1(int t1, int nb) {
        return edges1[t1][nb]+1;
    }

    public int opposingT2(int t2, int nb) {
        return edges2[t2][nb]-1;
    }

    /* ---- Indexed by Edge Id ----- */
    
    public int parentE(int e) {
        return prnt[e];
    }
    
    public int childE(int e) {
        return chld[e];
    }

    public int dualE(int e) {
        return dual[e];
    }
    
    public int iterE(int e) {
        return iter[e];
    }

    public int opposingE(int edge) {
        return isT1T2(edge) ? edge+1 : edge-1;
    }

    public T1 t1E(int e) {
        return nodes1.get(isT1T2(e) ? prnt[e] : chld[e]);
    }

    public T2 t2E(int e) {
        return nodes2.get(isT1T2(e) ? chld[e] : prnt[e]);
    }

    public String edgeToString(int e) {
        if (isT1T2(e)) {
            return String.format("Edge[id=%d, %s --> %s]", e, t1E(e), t2E(e));
        } else {
            return String.format("Edge[id=%d, %s --> %s]", e, t2E(e), t1E(e));
        }
    }
    
    /* ---- Other stats and accessors ----- */

    /** Number of directed edges. */
    public int getNumEdges() {
        return numEdges;
    }

    /** Number of undirected edges. */
    public int getNumUndirEdges() {
        return numEdges / 2;
    }

    public int numT1Nodes() {
        return nodes1.size();
    }
    
    public int numT2Nodes() {
        return nodes2.size();
    }

    public List<T1> getT1s() {
        return nodes1;
    }
    
    public List<T2> getT2s() {
        return nodes2;
    }
    
    /* ---- Graph algorithms ----- */
    
    /**
     * Gets the connected components of the graph.
     * @return A list containing an arbitrary node in each each connected component.
     */
    public IntArrayList getConnectedComponentsT2() {
        boolean[] marked1 = new boolean[nodes1.size()];
        boolean[] marked2 = new boolean[nodes2.size()];
        
        IntArrayList roots = new IntArrayList();
        for (int t2=0; t2<nodes2.size(); t2++) {
            if (!marked2[t2]) {
                roots.add(t2);
                
                // Depth first search.
                dfs(t2, false, marked1, marked2, null);
            }
        }
        return roots;
    }
    
    public interface BipVisitor<T1,T2> {
        void visit(int nodeId, boolean isT1, BipartiteGraph<T1,T2> bg);
    }
    
    /** Runs a depth-first-search starting at the root node. */
    public int dfs(int root, boolean isRootT1, boolean[] marked1, boolean[] marked2, BipVisitor<T1,T2> visitor) {
        IntStack sNode = new IntStack();
        ByteStack sIsT1 = new ByteStack(); // TODO: This should be a boolean stack.
        sNode.push(root);
        sIsT1.push((byte)(isRootT1 ? 1 : 0)); 
        
        // The number of times we check if any node is marked. 
        // For acyclic graphs this should be two times the number of nodes.
        int numMarkedChecks = 0;
        
        while (sNode.size() != 0) {
            int node = sNode.pop();
            boolean isT1 = (sIsT1.pop() == 1);            
            if (isT1) {
                // Visit node only if not marked.
                int t1 = node;
                if (!marked1[t1]) {
                    marked1[t1] = true;
                    if (visitor != null) { visitor.visit(t1, true, this); }
                    for (int nb = numNbsT1(t1)-1; nb >= 0; nb--) {
                        int t2 = childT1(t1, nb);
                        if (!marked2[t2]) {
                            sNode.push(t2);
                            sIsT1.push((byte)0);
                        }
                        numMarkedChecks++;
                    }
                }
                numMarkedChecks++;
            } else {
                // Visit node only if not marked.
                int t2 = node;
                if (!marked2[t2]) {
                    marked2[t2] = true;
                    if (visitor != null) { visitor.visit(t2, false, this); }
                    for (int nb = numNbsT2(t2)-1; nb >= 0; nb--) {
                        int t1 = childT2(t2, nb);
                        if (!marked1[t1]) {
                            sNode.push(t1);
                            sIsT1.push((byte)1);
                        }
                        numMarkedChecks++;
                    }
                }
                numMarkedChecks++;
            }
        }
        return numMarkedChecks;
    }
    
    public boolean isAcyclic() {        
        boolean[] marked1 = new boolean[nodes1.size()];
        boolean[] marked2 = new boolean[nodes2.size()];
        for (int t2=0; t2<nodes2.size(); t2++) {
            if (!marked2[t2]) {
                if (!isAcyclic(t2, false, marked1, marked2)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private boolean isAcyclic(int root, boolean isRootT1, boolean[] marked1, boolean[] marked2) {
        IntStack sNode = new IntStack();
        IntStack sDual = new IntStack();
        ByteStack sIsT1 = new ByteStack();
        
        sNode.push(root);
        sDual.push(-1);
        sIsT1.push((byte) (isRootT1 ? 1 : 0));
        
        while (sNode.size() != 0) {
            int node = sNode.pop();
            int dual = sDual.pop();
            boolean isT1 = (sIsT1.pop() == 1);            
            if (isT1) {
                int t1 = node;
                if (marked1[t1]) { return false; }
                marked1[t1] = true;                
                for (int nb = 0; nb < numNbsT1(t1); nb++) {
                    if (nb == dual) { continue; }
                    int t2 = childT1(t1, nb);
                    sNode.push(t2);
                    sDual.push(dualT1(t1, nb));
                    sIsT1.push((byte)0);
                }
            } else {
                int t2 = node;
                if (marked2[t2]) { return false; }
                marked2[t2] = true;
                for (int nb = 0; nb < numNbsT2(t2); nb++) {
                    if (nb == dual) { continue; }
                    int t1 = childT2(t2, nb);
                    sNode.push(t1);
                    sDual.push(dualT2(t2, nb));
                    sIsT1.push((byte)1);
                }
            }
        }
        return true;
    }
        
    /** Runs a breadth-first-search starting at the root node. */
    public IntArrayList bfs(int root, boolean isRootT1) {
        boolean[] marked1 = new boolean[nodes1.size()];
        boolean[] marked2 = new boolean[nodes2.size()];
        return bfs(root, isRootT1, marked1, marked2);
    }
    
    /** Runs a breadth-first-search starting at the root node. */
    public IntArrayList bfs(int root, boolean isRootT1, boolean[] marked1, boolean[] marked2) {
        IntArrayList ccorder = new IntArrayList();
        Queue<Integer> qNode = new ArrayDeque<>();
        Queue<Boolean> qIsT1 = new ArrayDeque<>();
        qNode.add(root);
        qIsT1.add(isRootT1);

        while (!qNode.isEmpty()) {
            // Process the next node in the queue.
            int parent = qNode.remove();
            boolean isT1 = qIsT1.remove();

            if (isT1) {
                // Visit node only if not marked.
                if (!marked1[parent]) {
                    log.trace("Visiting node {} of type T1", parent);
                    // For each neighbor...
                    for (int nb = 0; nb < this.numNbsT1(parent); nb++) {
                        int e = this.edgeT1(parent, nb);
                        int child = this.childE(e);
                        if (!marked2[child]) {
                            // If the neighbor is not marked, queue the node and add the edge to
                            // the order.
                            qNode.add(child);
                            qIsT1.add(false);
                            ccorder.add(e);
                        }
                    }
                    // Mark the node as visited.
                    marked1[parent] = true;
                }
            } else {
                // Visit node only if not marked.
                if (!marked2[parent]) {
                    log.trace("Visiting node {} of type T2", parent);
                    // For each neighbor...
                    for (int nb = 0; nb < this.numNbsT2(parent); nb++) {
                        int e = this.edgeT2(parent, nb);
                        int child = this.childE(e);
                        if (!marked1[child]) {
                            // If the neighbor is not marked, queue the node and add the edge to
                            // the order.
                            qNode.add(child);
                            qIsT1.add(true);
                            ccorder.add(e);
                        }
                    }
                    // Mark the node as visited.
                    marked2[parent] = true;
                }
            }
        }
        return ccorder;
    }
        
}
