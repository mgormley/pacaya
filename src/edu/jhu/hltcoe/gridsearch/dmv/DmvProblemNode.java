package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.hltcoe.gridsearch.ProblemNode;
import edu.jhu.hltcoe.gridsearch.Solution;

public class DmvProblemNode implements ProblemNode {

    private DmvBounds bounds;
    private DmvBoundsFactory boundsFactory;
    
    public DmvProblemNode(DmvBounds bounds, DmvBoundsFactory boundsFactory) {
        this.bounds = bounds;
        this.boundsFactory = boundsFactory;
    }
    
    @Override
    public double getOptimisticBound() {
        // Run the Dantzig-Wolfe algorithm on the relaxation of the main problem

        // Cache the solution values for getFeasibleSolution() 

        // Cache the optimistic bound value
        
        return 0;
    }

    @Override
    public Solution getFeasibleSolution() {
        // Project the Dantzig-Wolfe model parameters back into the bounded 
        // sum-to-exactly-one space
        // TODO: must use bounds here
        
        // Do randomized rounding on the Dantzig-Wolfe parse solution
        
        // Then run Viterbi EM starting from the randomly rounded solution
        // and respecting the bounds.
        // TODO: write a new DmvMStep that stays in the bounded parameter space
                
        return null;
    }
    
    @Override
    public List<ProblemNode> branch() {
        List<DmvBounds> boundsForChildren = boundsFactory.getDmvBounds(this);
        ArrayList<ProblemNode> children = new ArrayList<ProblemNode>(boundsForChildren.size());
        for (DmvBounds boundsForChild : boundsForChildren) {
            children.add(new DmvProblemNode(boundsForChild, boundsFactory));
        }
        return children;
    }

    @Override
    public int getId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getParentId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getDepth() {
        // TODO Auto-generated method stub
        return 0;
    }

}
