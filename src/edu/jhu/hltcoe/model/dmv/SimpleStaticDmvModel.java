package edu.jhu.hltcoe.model.dmv;

import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.TaggedWord;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.math.LabeledMultinomial;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Triple;

public class SimpleStaticDmvModel {

    public static final TaggedWord TW_A = new TaggedWord("a", "A");
    public static final TaggedWord TW_B = new TaggedWord("b", "B");

    public static DmvModel getTwoPosTagInstance() {
        DmvModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(0.1));
        Set<Label> vocab = new HashSet<Label>();
        vocab.add(WallDepTreeNode.WALL_LABEL);
        vocab.add(TW_A);
        vocab.add(TW_B);
        DmvModel dmvModel = (DmvModel) modelFactory.getInstance(vocab);

        dmvModel.setAllChooseWeights(0.0);
        dmvModel.putChooseWeight(WallDepTreeNode.WALL_LABEL, "l", TW_B, 1.0);
        dmvModel.putChooseWeight(WallDepTreeNode.WALL_LABEL, "r", TW_A, 0.5);
        dmvModel.putChooseWeight(WallDepTreeNode.WALL_LABEL, "r", TW_B, 0.5);
        dmvModel.putChooseWeight(TW_A, "l", TW_A, 0.5);
        dmvModel.putChooseWeight(TW_A, "l", TW_B, 0.5);
        dmvModel.putChooseWeight(TW_A, "r", TW_A, 1.0); // dummy param
        dmvModel.putChooseWeight(TW_B, "l", TW_A, 0.5);
        dmvModel.putChooseWeight(TW_B, "l", TW_B, 0.5);
        dmvModel.putChooseWeight(TW_B, "r", TW_B, 1.0);
        
        dmvModel.setAllStopWeights(1.0);
        dmvModel.putStopWeight(WallDepTreeNode.WALL_LABEL, "r", true, 0.0);
        dmvModel.putStopWeight(TW_A, "l", true, 0.6);
        dmvModel.putStopWeight(TW_A, "r", true, 0.6); 
        dmvModel.putStopWeight(TW_B, "l", true, 0.6); 
        dmvModel.putStopWeight(TW_B, "r", true, 0.6);
        
        return dmvModel;
    }

    public static final TaggedWord noun = new TaggedWord("Noun", "N");
    public static final TaggedWord adj = new TaggedWord("Adj", "Adj");
    public static final TaggedWord verb = new TaggedWord("Verb", "V");
    
    public static DmvModel getThreePosTagInstance() {
        DmvModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(0.1));
        Set<Label> vocab = new HashSet<Label>();
        vocab.add(WallDepTreeNode.WALL_LABEL);
        vocab.add(noun);
        vocab.add(adj);
        vocab.add(verb);
        DmvModel dmvModel = (DmvModel) modelFactory.getInstance(vocab);

        dmvModel.setAllChooseWeights(0.0);
        dmvModel.putChooseWeight(WallDepTreeNode.WALL_LABEL, "l", verb, 1.0);
        dmvModel.putChooseWeight(WallDepTreeNode.WALL_LABEL, "r", verb, 1.0);
        dmvModel.putChooseWeight(noun, "l", adj, 1.0);
        dmvModel.putChooseWeight(noun, "r", verb, 1.0);
        dmvModel.putChooseWeight(adj, "l", adj, 1.0);
        dmvModel.putChooseWeight(adj, "r", adj, 1.0);
        dmvModel.putChooseWeight(verb, "l", noun, 1.0);
        dmvModel.putChooseWeight(verb, "r", noun, 1.0);
        
        dmvModel.setAllStopWeights(1.0);
        dmvModel.putStopWeight(WallDepTreeNode.WALL_LABEL, "r", true, 0.0); // Always generate a verb
        dmvModel.putStopWeight(noun, "l", true, 0.6);
        //dmvModel.putStopWeight(noun, "l", false, 0.8);
        dmvModel.putStopWeight(noun, "r", true, 0.9); // Allow elephant saw cat that saw mouse
        dmvModel.putStopWeight(adj, "l", true, 0.8);
        dmvModel.putStopWeight(verb, "l", true, 0.0); // Always have a subject and object
        dmvModel.putStopWeight(verb, "r", true, 0.0);
        
        return dmvModel;
    }
    
}
