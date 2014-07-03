package edu.jhu.parse.cky;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import edu.jhu.parse.cky.CkyPcfgParser.LoopOrder;

public class CnfGrammarReaderTest {

    public static final String timeFliesGrammarResource = "/edu/jhu/parse/cky/time-flies.gr";

    @Test
    public void testLoadGrammarFromFile() throws IOException {

        CnfGrammarReader builder = new CnfGrammarReader();
        builder.loadFromResource(timeFliesGrammarResource);
        
        CnfGrammar grammar = builder.getGrammar(LoopOrder.CARTESIAN_PRODUCT);
        
        System.out.println(grammar);
        assertEquals(5, grammar.getNumLexicalTypes());
        assertEquals(8, grammar.getNumNonTerminals());
        
        // Only X -> NP VP rule is S -> NP VP
        assertEquals(
                1,
                grammar.getBinaryRulesWithChildren(
                        grammar.getNtAlphabet().lookupIndex("NP"),
                        grammar.getNtAlphabet().lookupIndex("VP")).length);
    }

}
