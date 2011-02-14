package edu.jhu.hltcoe.data;

import java.util.ArrayList;
import java.util.List;

public class DepTreeNode {

    public static final DepTreeNode WALL = null; 
    
    private TaggedWord label;
    private DepTreeNode parent = WALL;
    private List<DepTreeNode> children = new ArrayList<DepTreeNode>();
    
    public DepTreeNode(String word, String tag, int position) {
        this.label = new TaggedWord(word, tag, position);
    }

    public DepTreeNode(TaggedWord tw) {
        this.label = tw;
    }

    public TaggedWord getLabel() {
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

}
