package edu.jhu.hltcoe.data;


public class WallDepTreeNode extends DepTreeNode {

    public static final String WALL_ID = "$WALL$";
    public static final Label WALL_LABEL = new TaggedWord(WALL_ID, WALL_ID);
    public static final int WALL_POSITION = -1;
    
    public WallDepTreeNode() {
        super(WALL_LABEL, WALL_POSITION);
    }

    @Override
    public boolean isWall() {
        return true; 
    }
    
}
