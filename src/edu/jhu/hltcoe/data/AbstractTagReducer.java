package edu.jhu.hltcoe.data;

import org.apache.log4j.Logger;

public abstract class AbstractTagReducer {

    private static Logger log = Logger.getLogger(AbstractTagReducer.class);

    public AbstractTagReducer() {
        super();
    }

    public void reduceTags(DepTreebank trees) {
        for (DepTree tree : trees) {
            reduceTags(tree);
        }
    }

    public void reduceTags(DepTree tree) {
        for (DepTreeNode node : tree) {
            if (node.getLabel() instanceof TaggedWord) {
                TaggedWord tw = (TaggedWord) node.getLabel();
                String newTag = reduceTag(tw.getTag());
                if (newTag != null) {
                    tw.setTag(newTag);
                } else {
                    log.warn("Unknown tag: " + tw.getTag());
                }
            }
        }
    }
    
    /**
     * Returns null if the tag is unknown.
     */
    public abstract String reduceTag(String tag);

}