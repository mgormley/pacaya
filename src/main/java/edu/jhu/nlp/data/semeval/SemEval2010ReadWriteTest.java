package edu.jhu.nlp.data.semeval;

import java.io.File;
import java.io.IOException;

public class SemEval2010ReadWriteTest {

    private void readAndWrite(String inFile, String outFile) throws IOException {
        SemEval2010Reader r = new SemEval2010Reader(new File(inFile));
        SemEval2010Writer w = new SemEval2010Writer(new File(outFile));
        for (SemEval2010Sentence sent : r) {
            w.write(sent);
        }
        r.close();
        w.close();
    }
    
    public static void main(String[] args) throws IOException {
        (new SemEval2010ReadWriteTest()).readAndWrite(args[0], args[1]);
    }
}
