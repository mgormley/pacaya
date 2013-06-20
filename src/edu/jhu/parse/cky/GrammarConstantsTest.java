package edu.jhu.parse.cky;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.data.Tag;

public class GrammarConstantsTest {

    @Test
    public void testRemoveFunctionTag() {
        assertEquals(new Tag("-LRB-"), GrammarConstants.removeFunctionTag(new Tag("-LRB-")));
        assertEquals(new Tag("NP"), GrammarConstants.removeFunctionTag(new Tag("NP-SBJ")));
        assertEquals(new Tag("NP-12"), GrammarConstants.removeFunctionTag(new Tag("NP-SBJ-12")));
        assertEquals(new Tag("PP"), GrammarConstants.removeFunctionTag(new Tag("PP-LOC-CLR")));
        assertEquals(new Tag("PP-3"), GrammarConstants.removeFunctionTag(new Tag("PP-LOC-CLR-3")));        
        assertEquals(new Tag("PP=3"), GrammarConstants.removeFunctionTag(new Tag("PP-LOC-CLR=3")));        
    }

}
