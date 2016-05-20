package edu.jhu.pacaya.parse.dep.edmonds;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Chu-Liu-Edmonds' algorithm for finding a maximum branching in a complete,
 * directed graph. This implementation is based on Tarjan's 'Finding Optimum
 * Branchings' paper.
 * 
 * Let n be the number vertices and m the number of edges. Then the
 * running time of the algorithm may be characterized as:
 *
 *   Worst case:   O(m log n)
 *   Average case: O(n (log n) + m)
 *
 * @author eraldo
 * @author Nicholas Andrews
 * 
 */
public class Edmonds {
    /**
     * Fill <code>maxBranching</code> with a maximum branching of the given
     * complete graph <code>graph</code> and rooted in the given node.
     * 
     * @param graph
     * @param root
     * @param invertedMaxBranching
     */
    public void getMaxBranching(CompleteGraph graph, int root,
                                int[] invertedMaxBranching) {
        // Number of nodes.
        int numNodes = graph.getNumberOfNodes();

        // Branching.
        SparseGraph maxBranching = new SparseGraph();

        /*
         * Weighted edges used to allow weight modification and avoid any impact
         * in the original graph.
         */
        SimpleWeightedEdge[][] edges = new SimpleWeightedEdge[numNodes][numNodes];

        // Disjoint sets for the strongly connected components (SCC).
        DisjointSets sPartition = new DisjointSets(numNodes);
        // Disjoint sets for the weakly connected components (WCC).
        DisjointSets wPartition = new DisjointSets(numNodes);

        /*
         * Priority queues for each strongly connected component. In the
         * beginning, each SCC is composed by exactly one node.
         */
        ArrayList<PriorityQueue<SimpleWeightedEdge>> incomingEdges = new ArrayList<PriorityQueue<SimpleWeightedEdge>>(
                                                                                                                      numNodes);

        // Unique incoming edges for each SCC. Initially, no SCC has such edge.
        SimpleWeightedEdge[] enter = new SimpleWeightedEdge[numNodes];

        /*
         * List of root components, i.e., SCCs that have no incoming edges
         * (enter[scc] == null). In the beginning, every SCC is a root
         * component.
         */
        LinkedList<Integer> rootComponents = new LinkedList<Integer>();

        // Root node of each root component.
        int[] min = new int[numNodes];

        for (int node = 0; node < numNodes; ++node) {

            // Every SCC is a root component.
            rootComponents.add(node);

            /*
             * The head of its root component is its only node. This array is
             * called min in Tarjan's paper.
             */
            min[node] = node;

            // Create a priority queue for each SCC.
            PriorityQueue<SimpleWeightedEdge> sccPriorityQueue = new PriorityQueue<SimpleWeightedEdge>();
            incomingEdges.add(sccPriorityQueue);

            // No incoming edge is considered (nor created) for the root node.
            if (node != root) {
                /*
                 * Create and add all incoming edges of <code>node</code> to its
                 * SCC priority queue.
                 */
                for (int from = 0; from < numNodes; ++from) {
                    if (from == node)
                        // Skip autocycle edges.
                        continue;
                    // Create an weighted edge and add it to the priority queue.
                    edges[from][node] = new SimpleWeightedEdge(from, node,
                                                               graph.getEdgeWeight(from, node));
                    sccPriorityQueue.add(edges[from][node]);
                }
            }
        }

        // Root component with no available incoming edges.
        LinkedList<Integer> doneRootComponents = new LinkedList<Integer>();

        while (!rootComponents.isEmpty()) {
            // Get some arbitrary root component.
            int sccTo = rootComponents.pop();
            // Maximum edge entering the root component 'sccTo'.
            SimpleWeightedEdge maxInEdge = incomingEdges.get(sccTo).poll();

            if (maxInEdge == null) {
                // No edge left to consider in this component.
                doneRootComponents.add(sccTo);
                continue;
            }

            // SCC component of edge 'e' from node: e = (from, to).
            int sccFrom = sPartition.find(maxInEdge.from);

            if (sccFrom == sccTo) {
                // Skip, for now, this component.
                rootComponents.add(sccTo);
                continue;
            }

            // Include the selected edge in the current branching.
            maxBranching.addEdge(maxInEdge.from, maxInEdge.to);

            // SCC component of edge 'e' from node, where e = (from, to).
            int wssFrom = wPartition.find(maxInEdge.from);
            // SCC component of edge 'e' to node, where e = (from, to).
            int wssTo = wPartition.find(maxInEdge.to);

            // Edge connects two weakly connected components.
            if (wssFrom != wssTo) {
                wPartition.union(wssFrom, wssTo);
                enter[sccTo] = maxInEdge;
                continue;
            }

            /*
             * Edge is within the same WCC, thus it inclusion will create a new
             * SCC by uniting some old SCCs (the ones on the path from e.to to
             * e.from).
             */
            double minEdgeWeight = Double.POSITIVE_INFINITY;
            int minScc = -1;
            SimpleWeightedEdge tmpEdge = maxInEdge;
            while (tmpEdge != null) {
                if (tmpEdge.weight < minEdgeWeight) {
                    minEdgeWeight = tmpEdge.weight;
                    minScc = sPartition.find(tmpEdge.to);
                }

                tmpEdge = enter[sPartition.find(tmpEdge.from)];
            }

            // Increment incoming edges weight.
            double inc = minEdgeWeight - maxInEdge.weight;
            for (SimpleWeightedEdge e : incomingEdges.get(sccTo))
                e.weight += inc;

            // Set the head of the current SCC.
            min[sccTo] = min[minScc];

            // Include all used SCCs in the current SCC.
            tmpEdge = enter[sccFrom];
            while (tmpEdge != null) {
                /*
                 * Increment incoming edges weight and include them in the
                 * current SCC priority queue.
                 */
                int tmpSccTo = sPartition.find(tmpEdge.to);
                inc = minEdgeWeight - tmpEdge.weight;
                for (SimpleWeightedEdge e : incomingEdges.get(tmpSccTo)) {
                    e.weight += inc;
                    incomingEdges.get(sccTo).add(e);
                }
                // Remove the priority queue of this SCC.
                incomingEdges.set(tmpSccTo, null);
                sPartition.union(sccTo, tmpSccTo);

                // Next edge.
                tmpEdge = enter[sPartition.find(tmpEdge.from)];
            }

            // Include the new SCC to be considered in the future.
            rootComponents.add(sccTo);
        }

        // Invert the maximum branching.
        boolean[] visited = new boolean[numNodes];
        for (int scc : doneRootComponents)
            invertMaximumBranching(min[scc], maxBranching, visited,
                                   invertedMaxBranching);
    }

    /**
     * Walk through the given branching from <code>node</code> and store the
     * inverted branching in <code>invertedMaxBranching</code>.
     * 
     * In fact, the given branching can include cycles. But it is only necessary
     * to disconsider the last edges of each cycle to get the real branching.
     * Thus, we use the array <code>visited</code>.
     * 
     * @param node
     * @param branching
     * @param visited
     * @param invertedMaxBranching
     */
    private void invertMaximumBranching(int node, SparseGraph branching,
                                        boolean[] visited, int[] invertedMaxBranching) {
        visited[node] = true;
        Set<Integer> toNodes = branching.getOutEdges(node);
        if (toNodes == null)
            return;
        for (int to : toNodes) {
            if (visited[to])
                continue;
            invertedMaxBranching[to] = node;
            invertMaximumBranching(to, branching, visited, invertedMaxBranching);
        }
    }

    public static void main(String[] args) {

        // list.addEdge(n1, n4, 100);
        // list.addEdge(n1, n3, 400);
        // list.addEdge(n1, n2, 100);
        // list.addEdge(n2, n3, 100);
        // list.addEdge(n3, n2,  25);
        // list.addEdge(n3, n4,  75);
        // list.addEdge(n4, n3, 300);

        double ifn = Double.NEGATIVE_INFINITY;

        // Should give:
        //   1 <- 0
        //   2 <- 0
        //   3 <- 0
        //        double[][] weights = { 
        //  { ifn, 100, 400, 100 },
        // { ifn, ifn, 100, ifn },
        //   { ifn,  25, ifn,  75 },
        //  { ifn, ifn, 300, ifn },
        //};
        double[][] weights = { 
          { ifn, ifn, ifn, ifn },
          { ifn, ifn, ifn, ifn },
          { ifn, ifn, ifn, ifn },
          { ifn, ifn, ifn, ifn },
        };
        CompleteGraph graph = new CompleteGraph(weights);
        graph.setEdgeWeight(0,1,0.5);
        graph.setEdgeWeight(0,2,0.2);
        graph.setEdgeWeight(1,3,0.5);
        graph.setEdgeWeight(0,3,0.9);
        Edmonds eds = new Edmonds();
        int[] maxBranching = new int[weights.length];
        eds.getMaxBranching(graph, 0, maxBranching);

        // Print maximum branching per node.
        System.out.println("Maximum branching:");
        for (int to = 0; to < maxBranching.length; ++to)
            System.out.println(to + " <- " + maxBranching[to]);
    }
}