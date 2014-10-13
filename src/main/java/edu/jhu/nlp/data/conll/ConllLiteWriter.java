package edu.jhu.nlp.data.conll;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.log4j.Logger;

/**
 * Writes a single CoNLL-2009 format file.
 * 
 * @author mgormley
 *
 */
public class ConllLiteWriter implements Closeable {

    private static final Logger log = Logger.getLogger(ConllLiteWriter.class);
    private Writer writer;
    private int count;
    
    public ConllLiteWriter(File path) throws IOException {
        this(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8")));
    }
    public ConllLiteWriter(Writer writer) throws IOException {
        this.writer = writer;
        this.count = 0;
    }
    
    public void write(ConllLiteSentence sentence) throws IOException {
        for (ConllLiteToken token : sentence) {
            token.write(writer);
            writer.write("\n");
        }
        writer.write("\n");
        count++;
        writer.flush();
    }
    
    public void close() throws IOException {
        writer.close();
    }
    
}
