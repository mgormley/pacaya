package edu.jhu.hltcoe.data;

import java.util.ArrayList;


public class Sentence extends ArrayList<Label> {

    public Sentence(DepTree tree) {
        super();
        for (DepTreeNode node : tree.getNodes()) {
            add(node.getLabel());
        }
    }

}
