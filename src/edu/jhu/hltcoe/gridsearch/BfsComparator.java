package edu.jhu.hltcoe.gridsearch;

import java.util.Comparator;

/**
 * This compartor will sort the elements so that a best first search is
 * performed
 */
public class BfsComparator implements Comparator<ProblemNode> {

    @Override
    /** 
     * Head in the PQ will be the least element. In this case, 
     * it is the one with the highest (since this does maximization) 
     * optimistic bound.
     */
    public int compare(ProblemNode node1, ProblemNode node2) {
        // a negative integer, zero, or a positive integer as the first
        // argument is less than, equal to, or greater than the second.

        // Compare the optimistic bounds
        if (node1.getOptimisticBound() > node2.getOptimisticBound()) {
            return -1;
        } else if (node1.getOptimisticBound() < node2.getOptimisticBound()) {
            return 1;
        } else {
            return 0;
        }
    }
}
