package edu.jhu.nlp.tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.nlp.AbstractParallelAnnotator;
import edu.jhu.nlp.Annotator;
import edu.jhu.nlp.data.DepTree;
import edu.jhu.nlp.data.DepTreeNode;
import edu.jhu.nlp.data.DepTreebank;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.features.TemplateLanguage.AT;
import edu.jhu.util.collections.Sets;

public abstract class AbstractTagReducer extends AbstractParallelAnnotator implements Annotator {

    private static final Logger log = LoggerFactory.getLogger(AbstractTagReducer.class);
    private static final long serialVersionUID = 1L;
    private Set<String> unknownTags;

    public AbstractTagReducer() {
        super();
    }

    public void annotate(AnnoSentence sent) {
        ArrayList<String> cposTags = new ArrayList<>();
        for (String pos : sent.getPosTags()) {
            cposTags.add(reduceTag(pos));                
        }
        sent.setCposTags(cposTags);
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

    @Override
    public Set<AT> getAnnoTypes() {
        return Sets.getSet(AT.CPOS);
    }
        
    /**
     * Returns null if the tag is unknown.
     */
    public abstract String reduceTag(String tag);

    
}
