package edu.jhu.data.conll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class CoNLL08SentenceTest {

    @Test
    public void testEquals() {
        CoNLL08Sentence sent1 = getSent();
        CoNLL08Sentence sent2 = getSent();        
        assertEquals(sent1, sent2);
    }

    @Test
    public void testSetSrlGraph() {
        CoNLL08Sentence sent1 = getSent();
        CoNLL08Sentence sent2 = getEmptySent();        
        assertFalse(sent1.equals(sent2));
        
        SrlGraph srlGraph = sent1.getSrlGraph();
        sent2.setPredApredFromSrlGraph(srlGraph, true);
        
        assertEquals(sent1, sent2);
    }
    
    public CoNLL08Sentence getSent() {
        List<CoNLL08Token> tokens = new ArrayList<CoNLL08Token>();

        tokens.add(getTok("the", "_ _ _"));
        tokens.add(getTok("dog", "_ arg0 arg0"));
        tokens.add(getTok("ate", "ate.1 _ _"));
        tokens.add(getTok("food", "_ arg1 _"));
        tokens.add(getTok("while", "_ _ _"));
        tokens.add(getTok("watching", "watch.1 _ _"));
        tokens.add(getTok("the", "_ _ _"));
        tokens.add(getTok("cat", "_ _ arg1"));
        
        CoNLL08Sentence sent = new CoNLL08Sentence(tokens);
        return sent;
    }
    
    public CoNLL08Sentence getEmptySent() {
        List<CoNLL08Token> tokens = new ArrayList<CoNLL08Token>();

        tokens.add(getTok("the", "_"));
        tokens.add(getTok("dog", "_"));
        tokens.add(getTok("ate", "_"));
        tokens.add(getTok("food", "_"));
        tokens.add(getTok("while", "_"));
        tokens.add(getTok("watching", "_"));
        tokens.add(getTok("the", "_"));
        tokens.add(getTok("cat", "_"));
        
        CoNLL08Sentence sent = new CoNLL08Sentence(tokens);
        return sent;
    }

    public static CoNLL08Token getTok(String form, String predArgs) {
        //  Columns: ID FORM LEMMA GPOS PPOS SPLIT_FORM SPLIT_LEMMA PPOSS HEAD DEPREL PRED ... ARG
        return new CoNLL08Token("0 " + form + " lemma gpos ppos " + form + " split_lemma pposs 0 deprel " + predArgs);
    }

}
