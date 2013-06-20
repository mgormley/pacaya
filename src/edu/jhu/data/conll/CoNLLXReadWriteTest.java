package edu.jhu.data.conll;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.util.Files;

public class CoNLLXReadWriteTest {

    private static final String conllXExample= "/edu/jhu/hltcoe/data/conll/conll-x-example.conll";
    
    @Test
    public void testReadWrite() throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(conllXExample);
        CoNLLXFileReader cr = new CoNLLXFileReader(inputStream);

        StringWriter writer = new StringWriter();
        CoNLLXWriter cw = new CoNLLXWriter(writer);
        for (CoNLLXSentence sent : cr) {
            cw.write(sent);
        }
        cw.close();
        cr.close();
        
        String readSentsStr = Files.getResourceAsString(conllXExample);
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
