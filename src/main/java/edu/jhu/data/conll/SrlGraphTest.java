package edu.jhu.data.conll;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.junit.Test;

public class SrlGraphTest {

    @Test
    public void testConstructionFromCoNLL09Sentence() {
        CoNLL09Sentence sent = getSimpleCoNLL09Sentence();
        
        SrlGraph g = sent.getSrlGraph();
        
        // Check numbers of nodes/edges.
        assertEquals(2, g.getPreds().size());
        assertEquals(3, g.getArgs().size());
        assertEquals(4, g.getEdges().size());
        
        // Check arg positions.
        assertEquals(0, g.getArgs().get(0).getPosition());
        assertEquals(1, g.getArgs().get(1).getPosition());
        assertEquals(3, g.getArgs().get(2).getPosition());
        
        // Check pred positions/labels.
        assertEquals(1,      g.getPreds().get(0).getPosition());
        assertEquals("pres", g.getPreds().get(0).getLabel());
        assertEquals(2,      g.getPreds().get(1).getPosition());
        assertEquals("says", g.getPreds().get(1).getLabel());
        
        // Check first edge.
        assertEquals(0, g.getEdges().get(0).getArg().getPosition());
        assertEquals(1, g.getEdges().get(0).getPred().getPosition());
        assertEquals("A1", g.getEdges().get(0).getLabel());
        
        // Check last edge.
        assertEquals(3, g.getEdges().get(3).getArg().getPosition());
        assertEquals(2, g.getEdges().get(3).getPred().getPosition());
        assertEquals("A2", g.getEdges().get(3).getLabel());
    }

    public static CoNLL09Sentence getSimpleCoNLL09Sentence() {
        ArrayList<CoNLL09Token> tokens = new ArrayList<CoNLL09Token>();
        tokens.add(getTok("_ vice A1 _"));
        tokens.add(getTok("Y pres A0 A1 "));
        tokens.add(getTok("Y says _ _"));
        tokens.add(getTok("_ jump _ A2"));
        CoNLL09Sentence sent = new CoNLL09Sentence(tokens);
        return sent;
    }

    public static CoNLL09Token getTok(String fillpredPredApreds) {
        // Columns: ID FORM LEMMA PLEMMA POS PPOS FEAT PFEAT HEAD PHEAD DEPREL PDEPREL
        // FILLPRED PRED APREDs
        return new CoNLL09Token("0 form lemma plemma pos ppos feat pfeat 0 0 deprel pdeprel " + fillpredPredApreds);
    }

    @Test
    public void testGetSrlGraphOnRealData() throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        
        for (CoNLL09Sentence sent : cr) {
            sent.getSrlGraph();    
        }        
    }

}
