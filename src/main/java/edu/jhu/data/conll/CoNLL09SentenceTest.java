package edu.jhu.data.conll;

import static edu.jhu.util.Utilities.getList;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class CoNLL09SentenceTest {

    @Test
    public void testEquals() {
        CoNLL09Sentence sent1 = getSent();
        CoNLL09Sentence sent2 = getSent();        
        assertEquals(sent1, sent2);
    }

    @Test
    public void testSetSrlGraph() {
        CoNLL09Sentence sent1 = getSent();
        CoNLL09Sentence sent2 = getEmptySent();        
        assertFalse(sent1.equals(sent2));
        
        SrlGraph srlGraph = sent1.getSrlGraph();
        sent2.setColsFromSrlGraph(srlGraph);
        
        assertEquals(sent1, sent2);
    }
    
    public CoNLL09Sentence getSent() {
        List<CoNLL09Token> tokens = new ArrayList<CoNLL09Token>();

        tokens.add(getTok("the", "_ _ _ _"));
        tokens.add(getTok("dog", "_ _ arg0 arg0"));
        tokens.add(getTok("ate", "Y ate.1 _ _"));
        tokens.add(getTok("food", "_ _ arg1 _"));
        tokens.add(getTok("while", "_ _ _ _"));
        tokens.add(getTok("watching", "Y watch.1 _ _"));
        tokens.add(getTok("the", "_ _ _ _"));
        tokens.add(getTok("cat", "_ _ _ arg1"));
        
        CoNLL09Sentence sent = new CoNLL09Sentence(tokens);
        return sent;
    }
    
    public CoNLL09Sentence getEmptySent() {
        List<CoNLL09Token> tokens = new ArrayList<CoNLL09Token>();

        tokens.add(getTok("the", "_ _"));
        tokens.add(getTok("dog", "_ _"));
        tokens.add(getTok("ate", "_ _"));
        tokens.add(getTok("food", "_ _"));
        tokens.add(getTok("while", "_ _"));
        tokens.add(getTok("watching", "_ _"));
        tokens.add(getTok("the", "_ _"));
        tokens.add(getTok("cat", "_ _"));
        
        CoNLL09Sentence sent = new CoNLL09Sentence(tokens);
        return sent;
    }

    public static CoNLL09Token getTok(String form, String fillpredPredApreds) {
        // Columns: ID FORM LEMMA PLEMMA POS PPOS FEAT PFEAT HEAD PHEAD DEPREL PDEPREL
        // FILLPRED PRED APREDs
        return new CoNLL09Token("0 " + form + " lemma plemma " + form + " ppos feat pfeat 0 0 deprel pdeprel " + fillpredPredApreds);
    }

}
