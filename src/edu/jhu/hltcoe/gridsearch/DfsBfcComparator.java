package edu.jhu.hltcoe.gridsearch;

import java.util.Comparator;

/**
 * This comparator will sort the elements so that a depth first search is
 * performed and among the children of maximal depth, the best first child will
 * be chosen.
 */
public class DfsBfcComparator implements Comparator<ProblemNode> {

    public DfsBfcComparator() { }
    
    @Override
    /** 
     * Head in the PQ will be the least element. In this case, that is the latest node put on the 
     * priority queue, which is the one with the highest parent id. Among those with the
     * highest parent id, it is the one with the highest (since this does maximization) 
     * optimistic bound.
     */
    public int compare(ProblemNode node1, ProblemNode node2) {
        // a negative integer, zero, or a positive integer as the first
        // argument is less than, equal to, or greater than the second.

        // TODO: fix this - it should be comparing parent ids not depths.
        
        // First compare depths
        if (node1.getDepth() > node2.getDepth()) {
            return -1;
        } else if (node1.getDepth() < node2.getDepth()) {
            return 1;
        }

        // If the depths are equal, compare the optimistic bounds
        if (node1.getOptimisticBound() > node2.getOptimisticBound()) {
            return -1;
        } else if (node1.getOptimisticBound() < node2.getOptimisticBound()) {
            return 1;
        } else {
            return 0;
        }
    }
}
