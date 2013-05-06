package edu.jhu.hltcoe.data.conll;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.log4j.Logger;

public class CoNLLXWriter {

    private static final Logger log = Logger.getLogger(CoNLLXWriter.class);
    private BufferedWriter writer;
    
    public CoNLLXWriter(File path) throws IOException {
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"));
    }
    
    public void write(CoNLLXSentence sentence) throws IOException {
        for (CoNLLXToken token : sentence) {
            token.write(writer);
            writer.write("\n");
        }
        writer.write("\n");
    }
    
    public void close() throws IOException {
        writer.close();
    }
    
}
