package edu.jhu.hltcoe.parse.cky;

import static org.junit.Assert.*;

import org.junit.Test;

public class CkyPcfgParserTest {

    public static final String timeFliesGrammarResource = "/edu/jhu/hltcoe/parse/cky/time-flies.gr";

	@Test
	public void testSimpleSentence() {
		// time flies like an arrow.
		CnfGrammarBuilder builder = new CnfGrammarBuilder();
		builder.loadFromResource(timeFliesGrammarResource);
		
		CnfGrammar grammar = builder.getGrammar();
		
		// TODO:
		//Sentence sentence = new Sentence();
		//CkyPcfgParser.parseSentence(sentence, grammar);
	}

}
