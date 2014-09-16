package edu.jhu.nlp.data.conll;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.util.files.Files;

public class CoNLL08ReadWriteTest {

    public static final String conll2008Example= "/edu/jhu/data/conll/conll-08-example.conll";
    
    @Test
    public void testReadWrite() throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(conll2008Example);
        CoNLL08FileReader cr = new CoNLL08FileReader(inputStream);

        StringWriter writer = new StringWriter();
        CoNLL08Writer cw = new CoNLL08Writer(writer);
        for (CoNLL08Sentence sent : cr) {
            cw.write(sent);
        }
        cw.close();
        cr.close();
        
        String readSentsStr = Files.getResourceAsString(conll2008Example, "UTF-8");
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
