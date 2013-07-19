package edu.jhu.gm;

import java.util.List;

import edu.jhu.data.WallDepTreeNode;
import edu.jhu.gm.BeliefPropagation.Messages;
import edu.jhu.gm.FactorGraph.FgEdge;
import edu.jhu.gm.FactorGraph.FgNode;
import edu.jhu.gm.Var.VarType;
import edu.jhu.parse.dep.ProjectiveDependencyParser;
import edu.jhu.parse.dep.ProjectiveDependencyParser.DepIoChart;
import edu.jhu.util.Utilities;

/**
 * Global factor which constrains O(n^2) variables to form a projective
 * dependency tree, following Smith & Eisner (2008).
 * 
 * @author mgormley
 */
public class ProjDepTreeFactor extends Factor implements GlobalFactor {
        
    /**
     * Link variable. When true it indicates that there is an edge between its
     * parent and child.
     * 
     * @author mgormley
     */
    public static class LinkVar extends Var {

        // The variable states.
        public static final int TRUE = 1;
        public static final int FALSE = 0;
        
        private static final List<String> BOOLEANS = Utilities.getList("FALSE", "TRUE");
        private int parent;
        private int child;     
        
        public LinkVar(VarType type, String name, int parent, int child) {
            super(type, BOOLEANS.size(), name, BOOLEANS);
            this.parent = parent;
            this.child = child;
        }

        public int getParent() {
            return parent;
        }

        public int getChild() {
            return child;
        }

        public static String getDefaultName(int i, int j) {
            return String.format("Link_%d_%d", i, j);
        }
        
    }
    
    /** The sentence length. */
    private final int n;
    private int iterAtLastCreateMessagesCall = -1;
    private LinkVar[] rootVars;
    private LinkVar[][] childVars;
    
    /**
     * Constructor.
     * @param n The length of the sentence.
     */
    public ProjDepTreeFactor(int n, VarType type) {
        super(createVarSet(n, type));
        this.n = n;

        // TODO: We create the VarSet statically and then find extract the vars
        // again from the VarSet only because we're subclassing Factor. In the
        // future, we should drop this.
        LinkVar[] rootVars = new LinkVar[n];
        LinkVar[][] childVars = new LinkVar[n][n];
        VarSet vars = this.getVars();
        for (Var var : vars) {
            LinkVar link = (LinkVar) var;
            if (link.getParent() == -1) {
                rootVars[link.getChild()] = link;
            } else {
                childVars[link.getParent()][link.getChild()] = link;
            }
        }
    }
    
    /**
     * Get the link var corresponding to the specified parent and child position.
     * 
     * @param parent The parent word position, or -1 to indicate the wall node.
     * @param child The child word position.
     * @return The link variable.
     */
    public LinkVar getLinkVar(int parent, int child) {
        if (parent == -1) {
            return rootVars[child];
        } else {
            return childVars[parent][child];
        }
    }

    private static VarSet createVarSet(int n, VarType type) {
        VarSet vars = new VarSet();
        // Add a variable for each pair of tokens.
        for (int i=0; i<n; i++) {
            for (int j=0; j<n; j++) {
                if (i != j) {
                    String name = LinkVar.getDefaultName(i, j);
                    vars.add(new LinkVar(type, name, i, j));
                }
            }
        }
        // Add a variable for each variable being connected to the wall node.
        for (int j=0; j<n; j++) {
            String name = String.format("Link_%d_%d", WallDepTreeNode.WALL_POSITION, j);
            vars.add(new LinkVar(type, name, WallDepTreeNode.WALL_POSITION, j));
        }        
        return vars;
    }
    
    @Override
    public void createMessages(FgNode parent, Messages[] msgs, boolean logDomain, int iter) {
        if (iterAtLastCreateMessagesCall < iter) {
            createMessages(parent, msgs, logDomain);
            iterAtLastCreateMessagesCall = iter;
        }
    }
    
    public void createMessages(FgNode parent, Messages[] msgs, boolean logDomain) {
        assert (this == parent.getFactor());        
        double[] root = new double[n];
        double[][] child = new double[n][n];

        // Compute the odds ratios of the messages for each edge in the tree.
        Utilities.fill(root, Double.NEGATIVE_INFINITY);
        Utilities.fill(child, Double.NEGATIVE_INFINITY);
        for (FgEdge nbEdge : parent.getOutEdges()) {
            LinkVar link = (LinkVar) nbEdge.getVar();
            Factor nbMsg = msgs[nbEdge.getId()].message;
            double oddsRatio;
            if (logDomain) {
                oddsRatio = nbMsg.getValue(LinkVar.TRUE) - nbMsg.getValue(LinkVar.FALSE);
            } else {
                oddsRatio = nbMsg.getValue(LinkVar.TRUE) / nbMsg.getValue(LinkVar.FALSE);
                // We still need the log of this ratio since the parsing algorithm works in the log domain.
                oddsRatio = Utilities.log(oddsRatio);
            }
            
            if (link.getParent() == -1) {
                root[link.getChild()] = oddsRatio;
            } else {
                child[link.getParent()][link.getChild()] = oddsRatio;
            }       
        }

        // Compute the dependency tree marginals, summing over all projective
        // spanning trees via the inside-outside algorithm.
        DepIoChart chart = ProjectiveDependencyParser.insideOutsideAlgorithm(root, child);

        // Precompute the product of all the "false" messages.
        double pi = logDomain ? 0.0 : 1.0;
        for (FgEdge nbEdge : parent.getOutEdges()) {
            Factor nbMsg = msgs[nbEdge.getId()].message;
            if (logDomain) {
                pi += nbMsg.getValue(LinkVar.FALSE);
            } else {
                pi *= nbMsg.getValue(LinkVar.FALSE);
            }
        }
        
        double partition = logDomain ? pi + chart.getLogPartitionFunction() :
            pi * Utilities.exp(chart.getLogPartitionFunction());
        
        // Create the messages and stage them in the Messages containers.
        for (FgEdge nbEdge : parent.getOutEdges()) {
            LinkVar link = (LinkVar) nbEdge.getVar();
            
            double beliefTrue;
            double beliefFalse;
            if (logDomain) {
                beliefTrue = pi + chart.getLogSumOfPotentials(link.getParent(), link.getChild());
                beliefFalse = Utilities.logSubtract(partition, beliefTrue);
            } else {
                beliefTrue = pi * Utilities.exp(chart.getLogSumOfPotentials(link.getParent(), link.getChild()));
                beliefFalse = partition - beliefTrue;
            }

            msgs[nbEdge.getId()].newMessage.setValue(LinkVar.FALSE, beliefFalse);
            msgs[nbEdge.getId()].newMessage.setValue(LinkVar.TRUE, beliefTrue);
        }
        
    }

    public LinkVar[] getRootVars() {
        return rootVars;
    }

    public LinkVar[][] getChildVars() {
        return childVars;
    }

}
