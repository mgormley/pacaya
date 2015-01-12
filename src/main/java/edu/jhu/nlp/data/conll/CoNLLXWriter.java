package edu.jhu.nlp.data.conll;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes a single CoNLL-X format file.
 * 
 * @author mgormley
 *
 */
public class CoNLLXWriter implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(CoNLLXWriter.class);
    private Writer writer;
    private int count;
    
    public CoNLLXWriter(File path) throws IOException {
        this(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8")));
    }
    public CoNLLXWriter(Writer writer) throws IOException {
        this.writer = writer;
        this.count = 0;
    }
    
    public void write(CoNLLXSentence sentence) throws IOException {
        if (count != 0) {
            writer.write("\n");
        }
        for (CoNLLXToken token : sentence) {
            token.write(writer);
            writer.write("\n");
        }
        count++;
        writer.flush();
    }
    
    public void close() throws IOException {
        writer.close();
    }
    
}
