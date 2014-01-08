package edu.jhu.data.conll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import edu.jhu.util.collections.Maps;

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
        sent2.setColsFromSrlGraph(srlGraph);
        
        assertEquals(sent1, sent2);
    }
    

    public static final String conll2008Example= "/edu/jhu/data/conll/conll-08-example.conll";
    
    @Test
    public void testToC09() throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(conll2008Example);
        CoNLL08FileReader cr = new CoNLL08FileReader(inputStream);
        for (CoNLL08Sentence sent : cr) {
            System.out.println(sent.toCoNLL09Sent(true));
        }
        cr.close();
    }
    
    @Test
    public void testRemoveNominal() throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(conll2008Example);
        CoNLL08FileReader cr = new CoNLL08FileReader(inputStream);
        for (CoNLL08Sentence sent : cr) {
            sent.removeNominalPreds();
            System.out.println(sent);
        }
        cr.close();
    }

    @Test
    public void testAllTagsWithPreds() throws IOException {
        String f = "data/LDC/LDC2009T12/data/train/train.closed";
        //String f = "/Users/mgormley/research/other_lib/srl/conll05_to_08/gold/test.wsj.GOLD.simplified.conll08";
        InputStream inputStream = new FileInputStream(f);
        CoNLL08FileReader cr = new CoNLL08FileReader(inputStream);
        HashSet<String> set = new HashSet<String>();
        HashMap<String,Integer> count = new HashMap<String,Integer>();
        for (CoNLL08Sentence sent : cr) {
            for (int i=0; i<sent.size(); i++) {
                CoNLL08Token t = sent.get(i);
                if (t.getPred() != null && !t.getPred().equals("_")) {
                    // Is pred
                    String gpos = t.getPpos();
                    //if (gpos == null || (!gpos.startsWith("V") && !gpos.startsWith("N"))) {
                        //System.out.println(t);
                    //}
                    set.add(gpos);
                    Maps.increment(count, gpos, 1);
                }
            }
            //sent.removeNominalPreds();
            //System.out.println(sent);
        }
        cr.close();
        
        System.out.println(set);
        System.out.println(count);
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
