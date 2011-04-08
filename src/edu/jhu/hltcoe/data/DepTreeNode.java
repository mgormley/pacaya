package edu.jhu.hltcoe.data;

import java.util.ArrayList;
import java.util.List;

public class DepTreeNode {

    public static final DepTreeNode WALL = null; 

    private int position;
    private Label label;
    private DepTreeNode parent = WALL;
    private List<DepTreeNode> children = new ArrayList<DepTreeNode>();
    
    public DepTreeNode(String word, String tag, int position) {
        this.label = new TaggedWord(word, tag);
        this.position = position;
    }

    public DepTreeNode(Label tw, int position) {
        this.label = tw;
        this.position = position;
    }

    public Label getLabel() {
        return label;
    }

    public DepTreeNode getParent() {
        return parent;
    }

    void setParent(DepTreeNode parent) {
        this.parent = parent;
    }

    public List<DepTreeNode> getChildren() {
        return children;
    }

    void addChild(DepTreeNode child) {
        children.add(child);
    }

    public List<DepTreeNode> getChildrenToSide(String lr) {
        List<DepTreeNode> sideChildren = new ArrayList<DepTreeNode>();
        if (lr.equals("l")) {
            for (DepTreeNode child : children) {
                if (child.position < this.position){
                    sideChildren.add(child);
                }
            }
        } else if (lr.equals("r")) {
            for (DepTreeNode child : children) {
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
        if (parent == WALL) {
            parentPos = DepTree.WALL_IDX;
        } else {
            parentPos = parent.position;
        }
        return String.format("%d:%s-->%d", position, label.getLabel(), parentPos);
    }
}
