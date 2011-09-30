package edu.jhu.hltcoe.data;

import java.util.ArrayList;


public class Sentence extends ArrayList<Label> {

    private static final long serialVersionUID = 1L;

    protected Sentence() {
        super();
    }
    
    public Sentence(DepTree tree) {
        super();
        for (DepTreeNode node : tree.getNodes()) {
            if (!node.isWall()) {
                add(node.getLabel());
            }
        }
    }

}
