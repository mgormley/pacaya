package edu.jhu.hltcoe.gridsearch;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver.NodeResult;

/**
 * This class implements Depth First Search for branch-and-bound.
 *  
 * @author mgormley
 * 
 */
public class DfsNodeOrderer implements NodeOrderer {

    public static class DfsNodeOrdererPrm {
        public ChildOrderer childOrderer = new RelaxSolChildOrderer();
    }

    private DfsNodeOrdererPrm prm;
    private Stack<ProblemNode> stack;
    private RelaxedSolution rootSol;

    public DfsNodeOrderer(DfsNodeOrdererPrm prm) {
        this.prm = prm;
        stack = new Stack<ProblemNode>();
    }

    public void addRoot(ProblemNode root) {
        stack.push(root);
    }

    public void addChildrenOfResult(NodeResult result, double globalUb, double globalLb, boolean isRoot) {
        if (isRoot) {
            this.rootSol = result.relaxSol;
        }

        List<ProblemNode> children = prm.childOrderer.orderChildren(result.relaxSol, rootSol, result.children);
        
        // Add the new children to the stack for plunging.
        for (ProblemNode node : children) {
            stack.push(node);
        }
    }

    @Override
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    @Override
    public ProblemNode remove() {
        return stack.pop();
    }

    @Override
    public int size() {
        return stack.size();
    }

    @Override
    public void clear() {
        stack.clear();
    }

    @Override
    public Iterator<ProblemNode> iterator() {
        return stack.iterator();
    }

}
