package edu.jhu.hltcoe.model.dmv;

import util.Alphabet;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.TaggedWord;
import edu.jhu.hltcoe.model.dmv.DmvModel.Lr;

public class SimpleStaticDmvModel {

    public static final TaggedWord TW_A = new TaggedWord("a", "A");
    public static final TaggedWord TW_B = new TaggedWord("b", "B");

    public static DmvModel getTwoPosTagInstance() {
        DmvModelFactory modelFactory = new UniformDmvModelFactory();
        Alphabet<Label> alphabet = new Alphabet<Label>();
        alphabet.lookupObject(TW_A);
        alphabet.lookupObject(TW_B);
        DmvModel dmvModel = (DmvModel) modelFactory.getInstance(alphabet);

        dmvModel.fill(0.0);
        dmvModel.putRootWeight(TW_A, 0.5);
        dmvModel.putRootWeight(TW_B, 0.5);
        dmvModel.putChildWeight(TW_A, Lr.LEFT, TW_A, 0.5);
        dmvModel.putChildWeight(TW_A, Lr.LEFT, TW_B, 0.5);
        dmvModel.putChildWeight(TW_A, Lr.RIGHT, TW_A, 1.0); // dummy param
        dmvModel.putChildWeight(TW_B, Lr.LEFT, TW_A, 0.5);
        dmvModel.putChildWeight(TW_B, Lr.LEFT, TW_B, 0.5);
        dmvModel.putChildWeight(TW_B, Lr.RIGHT, TW_B, 1.0);
        
        dmvModel.fillStopProbs(1.0);
        dmvModel.putStopProb(TW_A, Lr.LEFT, true, 0.6);
        dmvModel.putStopProb(TW_A, Lr.RIGHT, true, 0.6); 
        dmvModel.putStopProb(TW_B, Lr.LEFT, true, 0.6); 
        dmvModel.putStopProb(TW_B, Lr.RIGHT, true, 0.6);
        
        dmvModel.convertRealToLog();
        dmvModel.assertNormalized(1e-13);
        return dmvModel;
    }

    public static final TaggedWord noun = new TaggedWord("Noun", "N");
    public static final TaggedWord adj = new TaggedWord("Adj", "Adj");
    public static final TaggedWord verb = new TaggedWord("Verb", "V");
    
    public static DmvModel getThreePosTagInstance() {
        DmvModelFactory modelFactory = new UniformDmvModelFactory();
        Alphabet<Label> alphabet = new Alphabet<Label>();
        alphabet.lookupObject(noun);
        alphabet.lookupObject(adj);
        alphabet.lookupObject(verb);
        DmvModel dmvModel = (DmvModel) modelFactory.getInstance(alphabet);

        dmvModel.fill(0.0);
        dmvModel.putRootWeight(verb, 1.0);
        dmvModel.putChildWeight(noun, Lr.LEFT, adj, 1.0);
        dmvModel.putChildWeight(noun, Lr.RIGHT, verb, 1.0);
        dmvModel.putChildWeight(adj, Lr.LEFT, adj, 1.0);
        dmvModel.putChildWeight(adj, Lr.RIGHT, adj, 1.0);
        dmvModel.putChildWeight(verb, Lr.LEFT, noun, 1.0);
        dmvModel.putChildWeight(verb, Lr.RIGHT, noun, 1.0);
        
        dmvModel.fillStopProbs(1.0);
        dmvModel.putStopProb(noun, Lr.LEFT, true, 0.6);
        //dmvModel.putStopWeight(noun, Lr.LEFT, false, 0.8);
        dmvModel.putStopProb(noun, Lr.RIGHT, true, 0.9); // Allow elephant saw cat that saw mouse
        dmvModel.putStopProb(adj, Lr.LEFT, true, 0.8);
        dmvModel.putStopProb(verb, Lr.LEFT, true, 0.0); // Always have a subject and object
        dmvModel.putStopProb(verb, Lr.RIGHT, true, 0.0);
        
        dmvModel.convertRealToLog();
        dmvModel.assertNormalized(1e-13);
        return dmvModel;
    }
    
}
