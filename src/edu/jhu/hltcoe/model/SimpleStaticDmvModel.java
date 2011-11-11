package edu.jhu.hltcoe.model;

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

    public static DmvModel getTwoPosTagInstance() {
        DmvModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(0.1));
        Set<Label> vocab = new HashSet<Label>();
        final TaggedWord twA = new TaggedWord("Noun", "A");
        final TaggedWord twB = new TaggedWord("Verb", "B");
        vocab.add(WallDepTreeNode.WALL_LABEL);
        vocab.add(twA);
        vocab.add(twB);
        DmvModel dmvModel = (DmvModel) modelFactory.getInstance(vocab);

        setAllChooseWeights(dmvModel, 0.0);
        dmvModel.putChooseWeight(WallDepTreeNode.WALL_LABEL, "l", twB, 1.0);
        dmvModel.putChooseWeight(WallDepTreeNode.WALL_LABEL, "r", twA, 0.5);
        dmvModel.putChooseWeight(WallDepTreeNode.WALL_LABEL, "r", twB, 0.5);
        dmvModel.putChooseWeight(twA, "l", twA, 0.5);
        dmvModel.putChooseWeight(twA, "l", twB, 0.5);
        dmvModel.putChooseWeight(twA, "r", twA, 1.0); // dummy param
        dmvModel.putChooseWeight(twB, "l", twA, 0.5);
        dmvModel.putChooseWeight(twB, "l", twB, 0.5);
        dmvModel.putChooseWeight(twB, "r", twB, 1.0);
        
        setAllStopWeights(dmvModel, 1.0);
        dmvModel.putStopWeight(WallDepTreeNode.WALL_LABEL, "r", true, 0.0);
        dmvModel.putStopWeight(twA, "l", true, 0.6);
        dmvModel.putStopWeight(twA, "r", true, 0.6); 
        dmvModel.putStopWeight(twB, "l", true, 0.6); 
        dmvModel.putStopWeight(twB, "r", true, 0.6);
        
        return dmvModel;
    }

    
    public static DmvModel getThreePosTagInstance() {
        DmvModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(0.1));
        Set<Label> vocab = new HashSet<Label>();
        final TaggedWord noun = new TaggedWord("Noun", "N");
        final TaggedWord adj = new TaggedWord("Adj", "Adj");
        final TaggedWord verb = new TaggedWord("Verb", "V");
        vocab.add(WallDepTreeNode.WALL_LABEL);
        vocab.add(noun);
        vocab.add(adj);
        vocab.add(verb);
        DmvModel dmvModel = (DmvModel) modelFactory.getInstance(vocab);

        setAllChooseWeights(dmvModel, 0.0);
        dmvModel.putChooseWeight(WallDepTreeNode.WALL_LABEL, "l", verb, 1.0);
        dmvModel.putChooseWeight(WallDepTreeNode.WALL_LABEL, "r", verb, 1.0);
        dmvModel.putChooseWeight(noun, "l", adj, 1.0);
        dmvModel.putChooseWeight(noun, "r", verb, 1.0);
        dmvModel.putChooseWeight(adj, "l", adj, 1.0);
        dmvModel.putChooseWeight(adj, "r", adj, 1.0);
        dmvModel.putChooseWeight(verb, "l", noun, 1.0);
        dmvModel.putChooseWeight(verb, "r", noun, 1.0);
        
        setAllStopWeights(dmvModel, 1.0);
        dmvModel.putStopWeight(WallDepTreeNode.WALL_LABEL, "r", true, 0.0); // Always generate a verb
        dmvModel.putStopWeight(noun, "l", true, 0.6);
        //dmvModel.putStopWeight(noun, "l", false, 0.8);
        dmvModel.putStopWeight(noun, "r", true, 0.9); // Allow elephant saw cat that saw mouse
        dmvModel.putStopWeight(adj, "l", true, 0.8);
        dmvModel.putStopWeight(verb, "l", true, 0.0); // Always have a subject and object
        dmvModel.putStopWeight(verb, "r", true, 0.0);
        
        return dmvModel;
    }

    private static void setAllChooseWeights(DmvModel dmvModel, double value) {
        for (Entry<Pair<Label, String>, LabeledMultinomial<Label>> entry : dmvModel.getChooseWeights().entrySet()) {
            for (Entry<Label,Double> subEntry : entry.getValue().entrySet()) {
                subEntry.setValue(value);                
            }
        }
    }

    private static void setAllStopWeights(DmvModel dmvModel, double value) {
        for (Entry<Triple<Label, String, Boolean>, Double> entry : dmvModel.getStopWeights().entrySet()) {
            entry.setValue(value);
        }
    }
    
}
