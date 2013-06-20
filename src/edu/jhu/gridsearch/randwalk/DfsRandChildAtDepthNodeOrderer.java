package edu.jhu.gridsearch.randwalk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;

import edu.jhu.gridsearch.AbstractNodeOrderer;
import edu.jhu.gridsearch.NodeOrderer;
import edu.jhu.gridsearch.ProblemNode;
import edu.jhu.util.Prng;

public class DfsRandChildAtDepthNodeOrderer extends AbstractNodeOrderer implements NodeOrderer {

    private static class NodeWithJitter {
        private double jitter;
        private ProblemNode node;

        public NodeWithJitter(ProblemNode node) {
            jitter = Prng.nextDouble();
            this.node = node;
        }

        public double getJitter() {
            return jitter;
        }

        public ProblemNode getNode() {
            return node;
        }
    }

    private static class JitterComparator implements Comparator<NodeWithJitter> {

        @Override
        public int compare(NodeWithJitter node1, NodeWithJitter node2) {
            return Double.compare(node1.getJitter(), node2.getJitter());
        }

    }

    private static final Logger log = Logger.getLogger(DfsRandChildAtDepthNodeOrderer.class);

    private ArrayList<PriorityQueue<NodeWithJitter>> pqs;
    PriorityQueue<NodeWithJitter> others;
    private int maxDepth;
    private int curDepth;
    private int size;
    
    public DfsRandChildAtDepthNodeOrderer(int maxDepth) {
        this.maxDepth = maxDepth; 
        curDepth = 0;
        size = 0;
        pqs = new ArrayList<PriorityQueue<NodeWithJitter>>(maxDepth);
        for (int i=0; i<maxDepth; i++) {
            pqs.add(new PriorityQueue<NodeWithJitter>(11, new JitterComparator()));
        }
        others = new PriorityQueue<NodeWithJitter>(11, new JitterComparator());
    }

    @Override
    public boolean add(ProblemNode node) {
        size++;
        if (node.getDepth() < maxDepth) {
            return pqs.get(node.getDepth()).add(new NodeWithJitter(node));
        } else {
            return others.add(new NodeWithJitter(node));
        }    
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public ProblemNode remove() {
        int pq = curDepth++ % maxDepth;
        for (int i=0; i<maxDepth && pqs.get(pq).isEmpty(); i++) {
            pq = curDepth++ % maxDepth;
        }
        if (!pqs.get(pq).isEmpty()) {
            // Return the next element from the next non-empty priority queue.
            size--;
            return pqs.get(pq).remove().getNode();
        } else if (!others.isEmpty()) {
            // Return one from the "others" list.
            size--;
            return others.remove().getNode();
        }
        throw new NoSuchElementException();
    }

    @Override
    public int size() {
        return size;
    }
    
    public void clear() {
        for (PriorityQueue<NodeWithJitter> pq : pqs) {
            pq.clear();
        }
        others.clear();
    }

    @Override
    public Iterator<ProblemNode> iterator() {
        // TODO: Fix this!
        log.error("Not creating a proper iterator over the entire collection");
        return new PqIterator(others.iterator());
    }
    
    private static class PqIterator implements Iterator<ProblemNode> {

        private Iterator<NodeWithJitter> iterator;

        public PqIterator(Iterator<NodeWithJitter> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public ProblemNode next() {
            NodeWithJitter nwj = iterator.next();
            if (nwj == null) {
                return null;
            }
            return nwj.getNode();
        }

        @Override
        public void remove() {
            throw new RuntimeException("not implemented");
        }
        
    }

}
