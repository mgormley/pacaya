package edu.jhu.nlp.data.conll;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import edu.jhu.util.collections.Lists;

/**
 * One token from a CoNNL-2008 formatted file.
 * 
 * From
 * http://barcelona.research.yahoo.net/dokuwiki/doku.php?id=conll2008:format
 * 
 * Columns: ID FORM LEMMA GPOS PPOS SPLIT_FORM SPLIT_LEMMA PPOSS HEAD DEPREL
 * PRED ... ARG
 * 
 * The following columns are predicted output: LEMMA, PPOS, SPLIT_LEMMA, PPOSS.
 * The following are gold output and are not provided at test time: GPOS, HEAD,
 * DEPREL, PRED, ARG
 * 
 * PRED is the same as in the 2009 English data. ARGs correspond to 2009's
 * APREDs.
 * 
 * Important note from the Shared Task description: "Since NomBank uses a
 * sub-word anal- ysis in some hyphenated words (such as finger ARG- pointing
 * PRED), the data for- mat represents the parts in hyphenated words as separate
 * tokens (columns 6–8). However, the format also represents how the parts
 * originally fit together before splitting (columns 2–5). Padding characters (“
 * ”) are used in columns 2–5 to ensure the same number of rows for all columns
 * corresponding to one sentence. All syntactic and semantic dependencies are
 * annotated relative to the split word forms (columns 6–8)."
 * 
 * @author mgormley
 * @author mmitchell
 * 
 */
public class CoNLL08Token {
    
    private static final Pattern whitespace = Pattern.compile("\\s+");
    
    // Field number:    Field name:     Description:
    /** 1    ID  Token counter, starting at 1 for each new sentence. */
    private int id;
    /** 2    FORM    Unsplit word form or punctuation symbol. */
    private String form;
    /** 3    LEMMA   Predicted lemma of FORM. */
    private String lemma;
    /** 4    GPOS  Gold part-of-speech tag from the Treebank (empty at test time). */
    private String gpos;
    /** 5    PPOS   Predicted part-of-speech tag. */
    private String ppos;
    /** 6    SPLIT_FORM   Tokens split at hyphens and slashes. */
    private String splitForm; 
    /** 7    SPLIT_LEMMA   Predicted lemma of SPLIT FORM. */
    private String splitLemma; 
    /** 8    PPOSS    Predicted POS tags of the split forms. */
    private String splitPpos;
    /** 9    PHEAD    Syntactic head of the current token, which is either a value of ID or zero (0). */
    private int head;
    /** 10    DEPREL  Syntactic dependency relation to the HEAD. */
    private String deprel;
    /** 11   PRED     Rolesets of the semantic predicates in this sentence. */
    private String pred;
    /** 12   ARGs     Columns with argument labels for each semantic predicate following textual order. */
    private List<String> apreds;
    
    public CoNLL08Token(String line) {
        //  Columns: ID FORM LEMMA GPOS PPOS SPLIT_FORM SPLIT_LEMMA PPOSS HEAD DEPREL PRED ... ARG

        String[] splits = whitespace.split(line);
        id = Integer.parseInt(splits[0]);
        form = splits[1];
        lemma = fromUnderscoreString(splits[2]);
        gpos = fromUnderscoreString(splits[3]);
        ppos = fromUnderscoreString(splits[4]);
        splitForm = fromUnderscoreString(splits[5]);
        splitLemma = fromUnderscoreString(splits[6]);
        splitPpos = fromUnderscoreString(splits[7]);
        head = Integer.parseInt(splits[8]);
        deprel = fromUnderscoreString(splits[9]);
        pred = fromUnderscoreString(splits[10]);
        apreds = new ArrayList<String>(Arrays.asList(Arrays.copyOfRange(splits, 11, splits.length)));
    }
    
    public CoNLL08Token(int id, String form, String lemma, String gpos, String pos, String splitForm,
            String splitLemma, String pposs, int head, String deprel, String pred, List<String> apreds) {
        super();
        this.id = id;
        this.form = form;
        this.lemma = lemma;
        this.gpos = gpos;
        this.ppos = pos;
        this.splitForm = splitForm;
        this.splitLemma = splitLemma;
        this.splitPpos = pposs;
        this.head = head;
        this.deprel = deprel;
        this.pred = pred;
        this.apreds = apreds;
    }

    /** Deep copy constructor */
    public CoNLL08Token(CoNLL08Token other) {
        this.id = other.id;
        this.form = other.form;
        this.lemma = other.lemma;
        this.gpos = other.gpos;
        this.ppos = other.ppos;
        this.splitForm = other.splitForm;
        this.splitLemma = other.splitLemma;
        this.splitPpos = other.splitPpos;
        this.head = other.head;
        this.deprel = other.deprel;
        this.pred = other.pred;
        this.apreds = other.apreds == null ? null : new ArrayList<String>(other.apreds);
    }

    /** Constructor which leaves all fields empty. */
    CoNLL08Token() {
        super();
        this.id = -1;
        this.head = -1;
    }
    
    public void intern() {
        //  Columns: ID FORM LEMMA GPOS PPOS SPLIT_FORM SPLIT_LEMMA PPOSS HEAD DEPREL PRED ... ARG
        if (form != null) { form = form.intern(); }
        if (lemma != null) { lemma = lemma.intern(); }
        if (gpos != null) { gpos = gpos.intern(); }
        if (ppos != null) { ppos = ppos.intern(); }
        if (splitForm != null) { splitForm = splitForm.intern(); }
        if (splitLemma != null) { splitLemma = splitLemma.intern(); }
        if (splitPpos != null) { splitPpos = splitPpos.intern(); }
        if (deprel != null) { deprel = deprel.intern(); }
        if (pred != null) { pred = pred.intern(); }
        if (apreds != null) { apreds = Lists.getInternedList(apreds); }
    }

    /**
     * Convert from the underscore representation of an optional string to the null representation.
     */
    private static String fromUnderscoreString(String value) {
        if (value.equals("_")) {
            return null;
        } else {
            return value;
        }
    }


    /**
     * Convert from the null representation of an optional string to the underscore representation.
     */
    private static String toUnderscoreString(String value) {
        if (value == null) {
            return "_";
        } else {
            return value;
        }
    }

    @Override
    public String toString() {
        return "CoNLL08Token [id=" + id + ", form=" + form + ", lemma=" + lemma + ", gpos=" + gpos + ", ppos=" + ppos
                + ", splitForm=" + splitForm + ", splitLemma=" + splitLemma + ", splitPpos=" + splitPpos + ", head="
                + head + ", deprel=" + deprel + ", pred=" + pred + ", apreds=" + apreds + "]";
    }

    public void write(Writer writer) throws IOException {
        final String sep = "\t";
        //  Columns: ID FORM LEMMA GPOS PPOS SPLIT_FORM SPLIT_LEMMA PPOSS HEAD DEPREL PRED ... ARG
        writer.write(String.format("%d", id));
        writer.write(sep);
        writer.write(String.format("%s", form));
        writer.write(sep);
        writer.write(String.format("%s", toUnderscoreString(lemma)));
        writer.write(sep);
        writer.write(String.format("%s", toUnderscoreString(gpos)));
        writer.write(sep);
        writer.write(String.format("%s", toUnderscoreString(ppos)));
        writer.write(sep);
        writer.write(String.format("%s", toUnderscoreString(splitForm)));
        writer.write(sep);
        writer.write(String.format("%s", toUnderscoreString(splitLemma)));
        writer.write(sep);
        writer.write(String.format("%s", toUnderscoreString(splitPpos)));
        writer.write(sep);
        writer.write(String.format("%s", Integer.toString(head)));
        writer.write(sep);
        writer.write(String.format("%s", toUnderscoreString(deprel)));
        writer.write(sep);
        writer.write(String.format("%s", toUnderscoreString(pred)));
        if (apreds.size() > 0) {
            writer.write(sep);
        }
        for (int i=0; i<apreds.size(); i++) {
            String apred = apreds.get(i);
            writer.write(String.format("%s", toUnderscoreString(apred)));
            if (i < apreds.size() - 1) {
                writer.write(sep);
            }
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public String getGpos() {
        return gpos;
    }

    public void setGpos(String gpos) {
        this.gpos = gpos;
    }

    public String getPpos() {
        return ppos;
    }

    public void setPpos(String ppos) {
        this.ppos = ppos;
    }

    public String getSplitForm() {
        return splitForm;
    }

    public void setSplitForm(String splitForm) {
        this.splitForm = splitForm;
    }

    public String getSplitLemma() {
        return splitLemma;
    }

    public void setSplitLemma(String splitLemma) {
        this.splitLemma = splitLemma;
    }

    public String getSplitPpos() {
        return splitPpos;
    }

    public void setSplitPpos(String splitPpos) {
        this.splitPpos = splitPpos;
    }

    public int getHead() {
        return head;
    }

    public void setHead(int head) {
        this.head = head;
    }

    public String getDeprel() {
        return deprel;
    }

    public void setDeprel(String deprel) {
        this.deprel = deprel;
    }

    public String getPred() {
        return pred;
    }

    public void setPred(String pred) {
        this.pred = pred;
    }

    public List<String> getApreds() {
        return apreds;
    }

    public void setApreds(List<String> apreds) {
        this.apreds = apreds;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((apreds == null) ? 0 : apreds.hashCode());
        result = prime * result + ((deprel == null) ? 0 : deprel.hashCode());
        result = prime * result + ((form == null) ? 0 : form.hashCode());
        result = prime * result + ((gpos == null) ? 0 : gpos.hashCode());
        result = prime * result + head;
        result = prime * result + id;
        result = prime * result + ((lemma == null) ? 0 : lemma.hashCode());
        result = prime * result + ((ppos == null) ? 0 : ppos.hashCode());
        result = prime * result + ((pred == null) ? 0 : pred.hashCode());
        result = prime * result + ((splitForm == null) ? 0 : splitForm.hashCode());
        result = prime * result + ((splitLemma == null) ? 0 : splitLemma.hashCode());
        result = prime * result + ((splitPpos == null) ? 0 : splitPpos.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CoNLL08Token other = (CoNLL08Token) obj;
        if (apreds == null) {
            if (other.apreds != null)
                return false;
        } else if (!apreds.equals(other.apreds))
            return false;
        if (deprel == null) {
            if (other.deprel != null)
                return false;
        } else if (!deprel.equals(other.deprel))
            return false;
        if (form == null) {
            if (other.form != null)
                return false;
        } else if (!form.equals(other.form))
            return false;
        if (gpos == null) {
            if (other.gpos != null)
                return false;
        } else if (!gpos.equals(other.gpos))
            return false;
        if (head != other.head)
            return false;
        if (id != other.id)
            return false;
        if (lemma == null) {
            if (other.lemma != null)
                return false;
        } else if (!lemma.equals(other.lemma))
            return false;
        if (ppos == null) {
            if (other.ppos != null)
                return false;
        } else if (!ppos.equals(other.ppos))
            return false;
        if (pred == null) {
            if (other.pred != null)
                return false;
        } else if (!pred.equals(other.pred))
            return false;
        if (splitForm == null) {
            if (other.splitForm != null)
                return false;
        } else if (!splitForm.equals(other.splitForm))
            return false;
        if (splitLemma == null) {
            if (other.splitLemma != null)
                return false;
        } else if (!splitLemma.equals(other.splitLemma))
            return false;
        if (splitPpos == null) {
            if (other.splitPpos != null)
                return false;
        } else if (!splitPpos.equals(other.splitPpos))
            return false;
        return true;
    }
    
}