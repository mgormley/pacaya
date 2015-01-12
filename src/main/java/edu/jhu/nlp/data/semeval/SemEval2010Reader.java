package edu.jhu.nlp.data.semeval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.jhu.nlp.data.NerMention;
import edu.jhu.nlp.data.Span;
import edu.jhu.nlp.data.simple.CloseableIterable;

/**
 * Reader of training files from the SemEval-2010 shared task for relation classification. <br>
 * <br>
 * The file consists of a set of sentences. Each sentence is represented by four consecutive lines. An example sentence
 * is given below. <br>
 * <br>
 * 
 * <pre>
 * 1    "The <e1>man</e2> jumped over the <e2>moon</e2>."
 * Was-Jumped(e2,e1)
 * Comment: He jumped very high.
 * 
 * </pre>
 * 
 * Note that the fourth line above is empty. And the sentence in the first line is quoted and (generally) not tokenized.
 * However, this reader will assume all the input is already tokenized by whitespace.
 *
 * WARNING: This will not correctly process the original SemEval-2010 files, because of the tokenization assumption above. 
 * 
 * @author mgormley
 */
public class SemEval2010Reader implements CloseableIterable<SemEval2010Sentence>, Iterator<SemEval2010Sentence> {

    private SemEval2010Sentence sentence;
    private BufferedReader reader;

    public SemEval2010Reader(File file) throws IOException {
        this(new FileInputStream(file));
    }

    public SemEval2010Reader(InputStream inputStream) throws UnsupportedEncodingException {
        this(new BufferedReader(new InputStreamReader(inputStream, "UTF-8")));
    }

    public SemEval2010Reader(BufferedReader reader) {
        this.reader = reader;
        next();
    }

    private static final Pattern TAB_RE = Pattern.compile("\t");
    private static final Pattern SPACE_RE = Pattern.compile(" ");
    //private static final Pattern PAREN_COMMA_RE = Pattern.compile("[(),]");
    private static final Pattern E1S_RE = Pattern.compile("<e1>(.+)");
    private static final Pattern E1E_RE = Pattern.compile("(.+)</e1>");
    private static final Pattern E2S_RE = Pattern.compile("<e2>(.+)");
    private static final Pattern E2E_RE = Pattern.compile("(.+)</e2>");


    public static SemEval2010Sentence readSentence(BufferedReader reader) throws IOException {
        String line1 = reader.readLine(); // Sentence w/entities.
        String line2 = reader.readLine(); // Relation and arguments.
        String line3 = reader.readLine(); // Comments.
        String line4 = reader.readLine(); // Empty line.
        
        if (line1 == null) {
            // End of reader reached.
            return null;
        }
        line1 = line1.trim();
        line2 = line2.trim();
        line3 = line3.trim();
        line4 = line4.trim();
        if (!line4.equals("")) {
            // line4 should mark the end of a sentence.
            throw new RuntimeException("The fourth line should be empty");
        }

        SemEval2010Sentence sent = new SemEval2010Sentence();
        
        String[] l1Splits = TAB_RE.split(line1);
        sent.id = l1Splits[0];

        // Trim the quotes to get the sentence.
        String sentStr = l1Splits[1].substring(1, l1Splits[1].length()-1);
        // Split into tokens by whitespace.
        List<String> words = Arrays.asList(SPACE_RE.split(sentStr));
        
        // Find e1 and e2 in the tokens.
        int e1s = -1;
        int e1e = -1;
        int e2s = -1;
        int e2e = -1;
        for (int i=0; i<words.size(); i++) {
            Matcher e1sm = E1S_RE.matcher(words.get(i));
            if (e1sm.find()) {
                if (e1s != -1) { throw new RuntimeException("Multiple matches for <e1></e1>: " + sentStr);}
                e1s = i;
                words.set(i, e1sm.group(1));
            }
            Matcher e1em = E1E_RE.matcher(words.get(i));
            if (e1em.find()) {
                if (e1e != -1) { throw new RuntimeException("Multiple matches for <e1></e1>: " + sentStr);}
                e1e = i;
                words.set(i, e1em.group(1));
            }
            Matcher e2sm = E2S_RE.matcher(words.get(i));
            if (e2sm.find()) {
                if (e2s != -1) { throw new RuntimeException("Multiple matches for <e2></e2>: " + sentStr);}
                e2s = i;
                words.set(i, e2sm.group(1));
            }
            Matcher e2em = E2E_RE.matcher(words.get(i));
            if (e2em.find()) {
                if (e2e != -1) { throw new RuntimeException("Multiple matches for <e2></e2>: " + sentStr);}
                e2e = i;
                words.set(i, e2em.group(1));
            }
        }
        
        if (e1s == -1 || e1e == -1 || e2s == -1 || e2e == -1) {
            throw new RuntimeException("Unable to find e1 or e2: " + sentStr);
        }
        if (e1s > e1e || e2s > e2e) {
            throw new RuntimeException(String.format("Invalid indices: %d %d %d %d", e1s, e1e, e2s, e2e));
        }
        
        sent.words = words;
        sent.comments = line3;
        sent.relation = line2;
        sent.e1 = new NerMention(new Span(e1s, e1e+1), "NONE", "NONE", "NONE", -1, "e1"+sent.id);
        sent.e2 = new NerMention(new Span(e2s, e2e+1), "NONE", "NONE", "NONE", -1, "e2"+sent.id);
        
        return sent;
    }

    @Override
    public boolean hasNext() {
        return sentence != null;
    }

    @Override
    public SemEval2010Sentence next() {
        try {
            SemEval2010Sentence curSent = sentence;
            sentence = readSentence(reader);
            if (curSent != null) {
                curSent.intern();
            }
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
    public Iterator<SemEval2010Sentence> iterator() {
        return this;
    }

    public void close() throws IOException {
        reader.close();
    }

}
