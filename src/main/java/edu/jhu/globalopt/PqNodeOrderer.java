package edu.jhu.globalopt;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

public class PqNodeOrderer extends AbstractNodeOrderer implements NodeOrderer {

    private PriorityQueue<ProblemNode> pq;
    
    public PqNodeOrderer(Comparator<ProblemNode> leafComparator) {
        this.pq = new PriorityQueue<ProblemNode>(11, leafComparator);
    }

    @Override
    public boolean add(ProblemNode node) {
        return pq.add(node);
    }

    @Override
    public boolean isEmpty() {
        return pq.isEmpty();
    }

    @Override
    public ProblemNode remove() {
        return pq.remove();
    }

    @Override
    public int size() {
        return pq.size();
    }

    @Override
    public void clear() {
        pq.clear();
    }

    @Override
    public Iterator<ProblemNode> iterator() {
        return pq.iterator();
    }
    
}
