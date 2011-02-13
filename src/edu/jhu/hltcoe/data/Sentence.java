package edu.jhu.hltcoe.data;

import java.util.ArrayList;


public class Sentence extends ArrayList<TaggedWord> {

    public Sentence(DepTree tree) {
        super();
        for (DepTreeNode node : tree.getNodes()) {
            add(node.getLabel());
        }
    }

}
