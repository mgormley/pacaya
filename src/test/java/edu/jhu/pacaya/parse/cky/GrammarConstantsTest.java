package edu.jhu.pacaya.parse.cky;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GrammarConstantsTest {

    @Test
    public void testRemoveFunctionTag() {
        assertEquals("-LRB-", GrammarConstants.removeFunctionTag("-LRB-"));
        assertEquals("NP", GrammarConstants.removeFunctionTag("NP-SBJ"));
        assertEquals("NP-12", GrammarConstants.removeFunctionTag("NP-SBJ-12"));
        assertEquals("PP", GrammarConstants.removeFunctionTag("PP-LOC-CLR"));
        assertEquals("PP-3", GrammarConstants.removeFunctionTag("PP-LOC-CLR-3"));        
        assertEquals("PP=3", GrammarConstants.removeFunctionTag("PP-LOC-CLR=3"));        
    }

}
