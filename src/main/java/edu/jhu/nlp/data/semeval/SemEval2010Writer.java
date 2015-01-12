package edu.jhu.nlp.data.semeval;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.nlp.data.NerMention;
import edu.jhu.util.collections.Lists;

/**
 * Writes a single CoNLL-X format file.
 * 
 * @author mgormley
 *
 */
public class SemEval2010Writer implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(SemEval2010Writer.class);
    private Writer writer;
    private int count;
    
    public SemEval2010Writer(File path) throws IOException {
        this(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8")));
    }
    public SemEval2010Writer(Writer writer) throws IOException {
        this.writer = writer;
        this.count = 0;
    }
    
    public void write(SemEval2010Sentence sent) throws IOException {
        // Line 1.
        writer.write(sent.id);
        writer.write("\t");
        writer.write('"'); // quotes
        writer.write(sentStr(sent));
        writer.write('"'); // quotes
        writer.write("\n");
        
        // Line 2.
        writer.write(sent.relation);
        writer.write("\n");
        
        // Line 3.

        if (sent.comments != null) { writer.write(sent.comments); }
        else { writer.write("Comment:"); }
        writer.write("\n");
        
        // Line 4.
        writer.write("\n");
        
        count++;
        writer.flush();
    }

    public String sentStr(SemEval2010Sentence sent) {
        List<String> words = sent.words;
        List<NerMention> ments = Lists.getList(sent.e1, sent.e2);
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            if (i != 0) {
                sb.append(" ");
            }
            for (int j = 0; j < ments.size(); j++) {
                NerMention s = ments.get(j);
                if (s.getSpan().start() == i) {
                    sb.append(String.format("<e%d>", j+1));
                }
            }
            sb.append(words.get(i));
            for (int j = 0; j < ments.size(); j++) {
                NerMention s = ments.get(j);
                if (s.getSpan().end() == i + 1) {
                    sb.append(String.format("</e%d>", j+1));
                }
            }
        }
        return sb.toString();
    }
    
    public void close() throws IOException {
        writer.close();
    }
    
    public int getCount() {
        return count;
    }
    
}
