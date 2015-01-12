package edu.jhu.nlp.data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ProjDepTreeNode implements DepTreeNode {

    private String label;
    private ProjDepTreeNode parent;
    private LinkedList<ProjDepTreeNode> leftChildren = new LinkedList<ProjDepTreeNode>();
    private ArrayList<ProjDepTreeNode> rightChildren = new ArrayList<ProjDepTreeNode>();
    private int position = DepTree.EMPTY_POSITION;

    public ProjDepTreeNode(String tw) {
        this.label = tw;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public DepTreeNode getParent() {
        return parent;
    }

    void setParent(ProjDepTreeNode parent) {
        this.parent = parent;
    }

    public List<? extends DepTreeNode> getChildren() {
        List<DepTreeNode> children = new ArrayList<DepTreeNode>();
        children.addAll(leftChildren);
        children.addAll(rightChildren);
        return children;
    }

    // TODO: adding to leftChildren in this way is inefficient
    public void addChildToOutside(ProjDepTreeNode projDepTreeNode, String leftRight) {
        projDepTreeNode.parent = this;
        if (leftRight.equals("l")) {
            leftChildren.addFirst(projDepTreeNode);
        } else {
            rightChildren.add(projDepTreeNode);
        }
    }

    /**
     * Returns the children ordered from left to right.
     */
    public List<? extends DepTreeNode> getChildrenToSide(String leftRight) {
        if (leftRight.equals("l")) {
            return leftChildren;
        } else {
            return rightChildren;
        }
    }

    public boolean isWall() {
        return false;
    }

    int getPosition() {
        return position;
    }
    
    public void setPosition(int i) {
        position = i;
    }
    
    List<? extends DepTreeNode> getInorderTraversal() {
        ArrayList<ProjDepTreeNode> nodes = new ArrayList<ProjDepTreeNode>();
        ProjDepTreeNode wall = this;
        while (wall.parent != null) {
            wall = wall.parent;
        }
        getInorderTraversalHelper(wall, nodes);
        return nodes;
    }
    
    private void getInorderTraversalHelper(ProjDepTreeNode current, ArrayList<ProjDepTreeNode> nodes) {
        // Traverse left subtree
        for (ProjDepTreeNode child : current.leftChildren) {
            getInorderTraversalHelper(child, nodes);
        }
        // Visit current node
        nodes.add(current);
        
        // Traverse right subtree
        for (ProjDepTreeNode child : current.rightChildren) {
            getInorderTraversalHelper(child, nodes);
        }
    }

    @Override
    public String toString() {
        int parentPos;
        if (parent == null) {
            parentPos = DepTree.EMPTY_POSITION;
        } else {
            parentPos = parent.getPosition();
        }
        return String.format("%d:%s-->%d", getPosition(), label, parentPos);
    }

}
