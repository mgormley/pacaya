package edu.jhu.hltcoe.data;

import java.util.HashSet;
import java.util.Set;

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
        trees.rebuildAlphabet();
    }

    public void reduceTags(DepTree tree) {
        Set<String> unknownTags = new HashSet<String>();
        for (DepTreeNode node : tree) {
            if (node.getLabel() instanceof TaggedWord) {
                TaggedWord tw = (TaggedWord) node.getLabel();
                String newTag = reduceTag(tw.getTag());
                if (newTag != null) {
                    tw.setTag(newTag);
                } else {
                    unknownTags.add(tw.getTag());
                }
            }
        }
        for (String unknownTag : unknownTags) {
            log.warn("Unknown tag: " + unknownTag);
        }
    }
    
    /**
     * Returns null if the tag is unknown.
     */
    public abstract String reduceTag(String tag);

}