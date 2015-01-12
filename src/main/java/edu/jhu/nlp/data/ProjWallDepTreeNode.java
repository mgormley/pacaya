package edu.jhu.nlp.data;


public class ProjWallDepTreeNode extends ProjDepTreeNode {
    
    public ProjWallDepTreeNode() {
        super(WallDepTreeNode.WALL_LABEL);
        setPosition(WallDepTreeNode.WALL_POSITION);
    }

    @Override
    public boolean isWall() {
        return true; 
    }
    
}
