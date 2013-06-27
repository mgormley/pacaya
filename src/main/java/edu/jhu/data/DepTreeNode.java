package edu.jhu.data;

import java.util.List;

public interface DepTreeNode {

    public Label getLabel();

    public DepTreeNode getParent();

    public List<? extends DepTreeNode> getChildren();

    public List<? extends DepTreeNode> getChildrenToSide(String lr);

    public boolean isWall();

    public String toString();

}