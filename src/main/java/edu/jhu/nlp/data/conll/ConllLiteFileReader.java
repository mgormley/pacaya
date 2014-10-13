package edu.jhu.nlp.data.conll;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import edu.jhu.nlp.data.simple.CloseableIterable;

/**
 * Reads a single file in CoNLL-2009 format.
 * 
 * @author mgormley
 *
 */
public class ConllLiteFileReader implements CloseableIterable<ConllLiteSentence>, Iterator<ConllLiteSentence> {

    private static final Pattern commentLine = Pattern.compile("^\\s*#");

    private ConllLiteSentence sentence;
    private BufferedReader reader;

    public ConllLiteFileReader(File file) throws IOException {
        this(new FileInputStream(file));
    }

    public ConllLiteFileReader(InputStream inputStream) throws UnsupportedEncodingException {
        this(new BufferedReader(new InputStreamReader(inputStream, "UTF-8")));
    }
    
    public ConllLiteFileReader(BufferedReader reader) {
        this.reader = reader;
        next();
    }

    public static ConllLiteSentence readConllLiteSentence(BufferedReader reader) throws IOException {
        // The current token.
        String line;
        // The tokens for one sentence.
        ArrayList<String> tokens = new ArrayList<String>();

        while ((line = reader.readLine()) != null) {
            if (commentLine.matcher(line).find()) {
                // Full line comment.
                continue;
            }
            if (line.trim().equals("")) {
                // End of sentence marker.
                break;
            } else {
                // Regular token.
                tokens.add(line);
            }
        }
        if (tokens.size() > 0) {
            return ConllLiteSentence.getInstanceFromTokenStrings(tokens);
        } else {
            return null;
        }
    }

    @Override
    public boolean hasNext() {
        return sentence != null;
    }

    @Override
    public ConllLiteSentence next() {
        try {
            ConllLiteSentence curSent = sentence;
            sentence = readConllLiteSentence(reader);
            return curSent;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void remove() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Iterator<ConllLiteSentence> iterator() {
        return this;
    }

    public void close() throws IOException {
        reader.close();
    }

    public List<ConllLiteSentence> readAll() {
        ArrayList<ConllLiteSentence> sents = new ArrayList<ConllLiteSentence>();
        for (ConllLiteSentence sent : this) {
            sents.add(sent);
        }
        return sents;
    }

    public List<ConllLiteSentence> readSents(int maxSents) {
        ArrayList<ConllLiteSentence> sents = new ArrayList<ConllLiteSentence>();
        for (ConllLiteSentence sent : this) {
            if (sents.size() > maxSents) {
                break;
            }
            sents.add(sent);
        }
        return sents;
    }

}