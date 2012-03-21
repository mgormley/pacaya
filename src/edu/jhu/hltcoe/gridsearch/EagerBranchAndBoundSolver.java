package edu.jhu.hltcoe.gridsearch;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;

/**
 * For a maximization problem, this performs eager (as opposed to lazy) branch
 * and bound.
 * 
 * The SCIP thesis section 6.3 notes that "Usually, the child nodes inherit the dual bound of their parent node", 
 * so maybe we should switch to lazy branch and bound. 
 */
public class EagerBranchAndBoundSolver {
    
    private static Logger log = Logger.getLogger(EagerBranchAndBoundSolver.class);

    public enum SearchStatus {
        OPTIMAL_SOLUTION_FOUND, NON_OPTIMAL_SOLUTION_FOUND
    }

    private static final double WORST_SCORE = Double.NEGATIVE_INFINITY;
    private static final double BEST_SCORE = Double.POSITIVE_INFINITY;
    private double incumbentScore;
    private Solution incumbentSolution;
    private double optimisticBound;

    private SearchStatus status;

    // Storage of active nodes
    private PriorityQueue<ProblemNode> leafNodePQ;
    private ProblemNode activeNode;

    public SearchStatus runBranchAndBound(ProblemNode rootNode, double epsilon, Comparator<ProblemNode> comparator) {
        // Initialize
        this.incumbentSolution = null;
        this.incumbentScore = WORST_SCORE;
        optimisticBound = BEST_SCORE;
        status = SearchStatus.NON_OPTIMAL_SOLUTION_FOUND;
        leafNodePQ = new PriorityQueue<ProblemNode>(11, comparator);

        optimisticBound = rootNode.getOptimisticBound();
        incumbentSolution = rootNode.getFeasibleSolution();
        if (incumbentSolution != null) {
            incumbentScore = incumbentSolution.getScore();
        }

        addToLeafNodes(rootNode);

        while (hasNextLeafNode()) {
            if (positiveDiff(optimisticBound, incumbentScore) <= epsilon) {
                status = SearchStatus.OPTIMAL_SOLUTION_FOUND;
                break;
            }
            // TODO: else if, ran out of memory or disk space, break

            ProblemNode curNode = getNextLeafNode();
            setActiveNode(curNode);
            
            List<ProblemNode> children = curNode.branch();
            for (ProblemNode childNode : children) {
                setActiveNode(childNode);
                
                if (worseThan(childNode.getOptimisticBound(), incumbentScore)) {
                    // fathom (i.e. prune) this child node
                }

                // Check if the child node offers a better feasible solution
                Solution childSolution = childNode.getFeasibleSolution();
                if (childSolution != null && betterThan(childSolution.getScore(), incumbentScore)) {
                    incumbentScore = childSolution.getScore();
                    incumbentSolution = childSolution;
                    // TODO: pruneActiveNodes();
                    // We could store a priority queue in the opposite order (or
                    // just a sorted list)
                    // and remove nodes from it while their optimisticBound is
                    // worse than the
                    // new incumbentScore.
                }

                addToLeafNodes(childNode);
            }
        }

        log.info("B&B search status: " + status);
        
        activeNode.end();
        // Return epsilon optimal solution
        return status;
    }

    private void setActiveNode(ProblemNode nextActive) {
        nextActive.setAsActiveNode(activeNode);
        activeNode = nextActive;
    }

    private boolean hasNextLeafNode() {
        return !leafNodePQ.isEmpty();
    }

    private ProblemNode getNextLeafNode() {
        return leafNodePQ.remove();
    }

    private void addToLeafNodes(ProblemNode rootNode) {
        leafNodePQ.add(rootNode);
    }

    private static double positiveDiff(double optimisticBound, double incumbentScore) {
        return Math.abs(optimisticBound - incumbentScore);
    }

    private static boolean betterThan(double optimisticBound, double incumbentScore) {
        return optimisticBound > incumbentScore;
    }

    private static boolean worseThan(double optimisticBound, double incumbentScore) {
        return optimisticBound <= incumbentScore;
    }

    public Solution getIncumbentSolution() {
        return incumbentSolution;
    }

}