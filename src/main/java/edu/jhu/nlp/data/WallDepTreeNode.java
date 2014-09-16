package edu.jhu.nlp.data;


public class WallDepTreeNode extends NonprojDepTreeNode {

    public static final String WALL_ID = "__WALL__";
    public static final String WALL_LABEL = WALL_ID;
    public static final int WALL_POSITION = -1;
    
    public WallDepTreeNode() {
        super(WALL_LABEL, WALL_POSITION);
    }

    @Override
    public boolean isWall() {
        return true; 
    }
    
}
