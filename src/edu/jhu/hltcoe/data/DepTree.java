package edu.jhu.hltcoe.data;

import java.util.Set;

import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.trees.Dependency;
import edu.stanford.nlp.trees.Tree;

public class DepTree {

    public DepTree(Tree tree) {
        Set<Dependency<Label,Label,Object>> dependencies = tree.taggedDependencies();
        for (Dependency<Label,Label,Object> dependency : dependencies) {
            TaggedWord parent = (TaggedWord) dependency.governor();
            TaggedWord child = (TaggedWord) dependency.dependent();
            Object name = dependency.name();
            
            parent.beginPosition();
            parent.endPosition();
            //TODO:
        }
    }
    
}
