package edu.jhu.hltcoe.data;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

public abstract class AbstractTagReducer {

    private static Logger log = Logger.getLogger(AbstractTagReducer.class);
    private Set<String> unknownTags;

    public AbstractTagReducer() {
        super();
    }

    public void reduceTags(DepTreebank trees) {
        unknownTags = new HashSet<String>();
        for (DepTree tree : trees) {
            reduceTags(tree);
        }
        log.warn("Number of unknown tags: " + unknownTags.size());
        for (String unknownTag : unknownTags) {
            log.warn("Unknown tag: " + unknownTag);
        }
        
        trees.rebuildAlphabet();
    }

    private void reduceTags(DepTree tree) {
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
    }
    
    /**
     * Returns null if the tag is unknown.
     */
    public abstract String reduceTag(String tag);

}