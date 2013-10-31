package edu.jhu.gm.model;

import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.data.WallDepTreeNode;
import edu.jhu.gm.inf.BeliefPropagation.Messages;
import edu.jhu.gm.model.FactorGraph.FgEdge;
import edu.jhu.gm.model.FactorGraph.FgNode;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.parse.dep.ProjectiveDependencyParser;
import edu.jhu.parse.dep.ProjectiveDependencyParser.DepIoChart;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.math.FastMath;

/**
 * Global factor which constrains O(n^2) variables to form a projective
 * dependency tree, following Smith & Eisner (2008).
 * 
 * @author mgormley
 */
public class ProjDepTreeFactor extends AbstractGlobalFactor implements GlobalFactor {

    private static final long serialVersionUID = 1L;
 
    /**
     * Link variable. When true it indicates that there is an edge between its
     * parent and child.
     * 
     * @author mgormley
     */
    public static class LinkVar extends Var {

        private static final long serialVersionUID = 1L;

        // The variable states.
        public static final int TRUE = 1;
        public static final int FALSE = 0;
        
        private static final List<String> BOOLEANS = Lists.getList("FALSE", "TRUE");
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
    
    private static final Logger log = Logger.getLogger(ProjDepTreeFactor.class);
    
    private final VarSet vars;
    /** The sentence length. */
    private final int n;
    private LinkVar[] rootVars;
    private LinkVar[][] childVars;
    
    /**
     * Constructor.
     * @param n The length of the sentence.
     */
    public ProjDepTreeFactor(int n, VarType type) {    
        super();
        this.vars = createVarSet(n, type);
        this.n = n;

        // TODO: We created the VarSet statically and then find extract the vars
        // again from the VarSet only because we're subclassing Factor. In the
        // future, we should drop this.
        rootVars = new LinkVar[n];
        childVars = new LinkVar[n][n];
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
        VarSet vars = new VarSet() {
            @Override
            public int calcNumConfigs() {
                throw new RuntimeException("This should never be called on a global factor.");
            }
        };
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
    protected void createMessages(FgNode parent, Messages[] msgs, boolean logDomain) {
        assert (this == parent.getFactor());        
        double[] root = new double[n];
        double[][] child = new double[n][n];

        // Compute the odds ratios of the messages for each edge in the tree.
        DoubleArrays.fill(root, Double.NEGATIVE_INFINITY);
        DoubleArrays.fill(child, Double.NEGATIVE_INFINITY);
        for (FgEdge inEdge : parent.getInEdges()) {
            LinkVar link = (LinkVar) inEdge.getVar();
            DenseFactor inMsg = msgs[inEdge.getId()].message;
            double oddsRatio;
            if (logDomain) {
                oddsRatio = inMsg.getValue(LinkVar.TRUE) - inMsg.getValue(LinkVar.FALSE);
            } else {
                oddsRatio = inMsg.getValue(LinkVar.TRUE) / inMsg.getValue(LinkVar.FALSE);
                // We still need the log of this ratio since the parsing algorithm works in the log domain.
                oddsRatio = FastMath.log(oddsRatio);
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
        for (FgEdge inEdge : parent.getInEdges()) {
            DenseFactor inMsg = msgs[inEdge.getId()].message;
            if (logDomain) {
                pi += inMsg.getValue(LinkVar.FALSE);
            } else {
                pi *= inMsg.getValue(LinkVar.FALSE);
            }
        }

        double partition = logDomain ? pi + chart.getLogPartitionFunction() :
            pi * FastMath.exp(chart.getLogPartitionFunction());

        if (log.isTraceEnabled()) {
            log.trace(String.format("partition: %.2f", partition));
        }
        
        // Create the messages and stage them in the Messages containers.
        for (FgEdge outEdge : parent.getOutEdges()) {
            LinkVar link = (LinkVar) outEdge.getVar();
            
            double beliefTrue;
            double beliefFalse;
            if (logDomain) {
                beliefTrue = pi + chart.getLogSumOfPotentials(link.getParent(), link.getChild());
                if (partition < beliefTrue) {
                    // TODO: This is a hack to get around the floating point
                    // error. We want beliefFalse to be effectively 0.0 in this
                    // case, but we use the floating point error to determine
                    // how small zero should be.
                    beliefFalse = FastMath.log(Math.abs(partition - beliefTrue));
                } else {
                    beliefFalse = FastMath.logSubtract(partition, beliefTrue);
                }
            } else {
                beliefTrue = pi * FastMath.exp(chart.getLogSumOfPotentials(link.getParent(), link.getChild()));
                beliefFalse = partition - beliefTrue;
            }

            if (log.isTraceEnabled()) {
                log.trace(String.format("beliefTrue: %d %d = %.2f", link.getParent(), link.getChild(), beliefTrue));
                log.trace(String.format("beliefFalse: %d %d = %.2f", link.getParent(), link.getChild(), beliefFalse));
            }
            
            // Divide out the incoming message to obtain the outgoing message from the belief. 
            FgEdge inEdge = outEdge.getOpposing();
            DenseFactor inMsg = msgs[inEdge.getId()].message;
            if (logDomain) {
                beliefTrue -= inMsg.getValue(LinkVar.TRUE);
                beliefFalse -= inMsg.getValue(LinkVar.FALSE);
            } else {
                beliefTrue /= inMsg.getValue(LinkVar.TRUE);
                beliefFalse /= inMsg.getValue(LinkVar.FALSE);                
            }
            
            // Set the outgoing messages.
            msgs[outEdge.getId()].newMessage.setValue(LinkVar.FALSE, beliefFalse);
            msgs[outEdge.getId()].newMessage.setValue(LinkVar.TRUE, beliefTrue);
        }
                
    }

    public LinkVar[] getRootVars() {
        return rootVars;
    }

    public LinkVar[][] getChildVars() {
        return childVars;
    }
    
    @Override
    public VarSet getVars() {
        return vars;
    }
    
    @Override
    public Factor getClamped(VarConfig clmpVarConfig) {
        if (clmpVarConfig.size() > 0) {
            throw new IllegalStateException("Unable to clamp these variables.");
        }
        return this;
    }

    @Override
    public Object getTemplateKey() {
        return ProjDepTreeFactor.class;
    }

}
