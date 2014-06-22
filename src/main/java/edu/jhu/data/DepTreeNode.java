package edu.jhu.data;

import java.util.List;

public interface DepTreeNode {

    public String getLabel();

    public void setLabel(String label);

    public DepTreeNode getParent();

    public List<? extends DepTreeNode> getChildren();

    public List<? extends DepTreeNode> getChildrenToSide(String lr);

    public boolean isWall();

    public String toString();

}