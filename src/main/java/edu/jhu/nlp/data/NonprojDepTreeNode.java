package edu.jhu.nlp.data;

import java.util.ArrayList;
import java.util.List;

public class NonprojDepTreeNode implements DepTreeNode {

    private int position = DepTree.EMPTY_POSITION;
    private String label;
    private NonprojDepTreeNode parent;
    private List<NonprojDepTreeNode> children = new ArrayList<NonprojDepTreeNode>();
    
    public NonprojDepTreeNode(String tw, int position) {
        this.label = tw;
        this.position = position;
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

    void setParent(NonprojDepTreeNode parent) {
        this.parent = parent;
    }

    public List<? extends DepTreeNode> getChildren() {
        return children;
    }

    void addChild(NonprojDepTreeNode child) {
        children.add(child);
    }

    public List<DepTreeNode> getChildrenToSide(String lr) {
        List<DepTreeNode> sideChildren = new ArrayList<DepTreeNode>();
        if (lr.equals("l")) {
            for (NonprojDepTreeNode child : children) {
                if (child.position < this.position){
                    sideChildren.add(child);
                }
            }
        } else if (lr.equals("r")) {
            for (NonprojDepTreeNode child : children) {
                if (this.position < child.position){
                    sideChildren.add(child);
                }
            }            
        } else {
            throw new IllegalArgumentException("invalid lr arg: "+ lr);
        }
        return sideChildren;
    }    

    public boolean isWall() {
        return false; 
    }
    
    @Override
    public String toString() {
        int parentPos;
        if (parent == null) {
            parentPos = DepTree.EMPTY_POSITION;
        } else {
            parentPos = parent.position;
        }
        return String.format("%d:%s-->%d", position, label, parentPos);
    }
}
