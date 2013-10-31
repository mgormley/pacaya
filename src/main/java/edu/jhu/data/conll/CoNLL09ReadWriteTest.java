package edu.jhu.data.conll;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.util.files.Files;

public class CoNLL09ReadWriteTest {

    public static final String conll2009Example= "/edu/jhu/data/conll/CoNLL2009-ST-Catalan-trial.txt";
    
    @Test
    public void testReadWrite() throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);

        StringWriter writer = new StringWriter();
        CoNLL09Writer cw = new CoNLL09Writer(writer);
        for (CoNLL09Sentence sent : cr) {
            cw.write(sent);
        }
        cw.close();
        cr.close();
        
        String readSentsStr = Files.getResourceAsString(conll2009Example, "UTF-8");
        String writeSentsStr = writer.getBuffer().toString();
        String[] readSplits = readSentsStr.split("\n");
        String[] writeSplits = writeSentsStr.split("\n");
        
        // Check that it writes the correct number of lines.
        Assert.assertEquals(readSplits.length, writeSplits.length);
        for (int i=0; i<readSplits.length; i++) {
            System.out.println(readSplits[i]);
            System.out.println(writeSplits[i]);
            // Check that everything except for whitespace is identical.
            Assert.assertEquals(canonicalizeWhitespace(readSplits[i]), canonicalizeWhitespace(writeSplits[i]));    
        }
        // Check that whitespace is identical.
        Assert.assertEquals(readSentsStr, writeSentsStr);
    }

    private String canonicalizeWhitespace(String str) {
        return str.trim().replaceAll("[ \t]+", " ");
    }

}
