package edu.jhu.nlp.eval;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class DepParseAccuracyTest {

    char[] asciiPunct = new char[] { '!', '"', '#', '$', '%', '&', '\'', '}', '(', ')', '*', '+', ',', '-', '.', '/',
            ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']', '\'', '^', '_', '`', '{', '|', '}', '~' };
    
    @Test
    public void testUnicodePunctuationMatching() {
        // http://www.fileformat.info/info/unicode/char/05c6/index.htm
        String hebrewNunHafukha = "\u05C6";
        assertTrue(DepParseAccuracy.PUNCT_RE.matcher(hebrewNunHafukha).matches());
        assertTrue(DepParseAccuracy.PUNCT_RE.matcher("-"+hebrewNunHafukha).matches());
        //assertTrue(DepParseEvaluator.PUNCT_RE.matcher("!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~").matches());
        assertTrue(DepParseAccuracy.PUNCT_RE.matcher("-"+hebrewNunHafukha+".!?,").matches());
        assertFalse(DepParseAccuracy.PUNCT_RE.matcher(",M.").matches());
        assertFalse(DepParseAccuracy.PUNCT_RE.matcher("012341").matches());
    }
    
    @Test
    public void testAsciiPunctuationMatching() {
        for (int i=0; i<asciiPunct.length; i++) {
            String cs = asciiPunct[i] + "";
            System.out.println(cs);
            assertTrue(Pattern.compile("^\\p{Punct}+$").matcher(cs).matches());
            //assertTrue(Pattern.compile("^[\\p{IsPunctuation}]+$").matcher(cs).matches());
            //assertTrue(DepParseEvaluator.PUNCT_RE.matcher(cs).matches());
        }
    }

}
