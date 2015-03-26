package edu.jhu.nlp.data.simple;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.nlp.data.DepEdgeMask;

public class DepEdgeMaskWriter implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(DepEdgeMaskWriter.class);
    private Writer writer;
    
    public DepEdgeMaskWriter(File path) throws IOException {
        this(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8")));
    }
    public DepEdgeMaskWriter(Writer writer) throws IOException {
        this.writer = writer;
    }
    
    public void write(AnnoSentence sent) throws IOException {        
        writer.write(StringUtils.join(sent.getWords(), " "));
        DepEdgeMask mask = sent.getDepEdgeMask();    
        if (mask != null) {
            writer.write(mask.toString());
        }
        writer.flush();
    }
    
    public void close() throws IOException {
        writer.close();
    }
    
}
