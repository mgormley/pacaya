package edu.jhu.nlp.tag;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.jhu.nlp.data.DepTree;
import edu.jhu.nlp.data.DepTreeNode;
import edu.jhu.nlp.data.DepTreebank;

public abstract class AbstractTagReducer {

    private static final Logger log = Logger.getLogger(AbstractTagReducer.class);
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
        // TODO: this is buggy in that the alphabet is left out of sync with
        // the treebank.
    }

    private void reduceTags(DepTree tree) {
        for (DepTreeNode node : tree) {            
            String oldTag = node.getLabel();
            String newTag = reduceTag(oldTag);
            if (newTag != null) {
                node.setLabel(newTag);
            } else {
                unknownTags.add(oldTag);
            }
        }
    }
    
    /**
     * Returns null if the tag is unknown.
     */
    public abstract String reduceTag(String tag);

}