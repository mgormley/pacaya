package edu.jhu.nlp.data.conll;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One token from a CoNLL Lite formatted file.
 * 
 * We designed this format for easy annotation of SRL in a spreadsheet editor.
 * Semantic edges are marked on the argument and are indicated by the id of the
 * parent and the label of the argument.
 * 
 * @author mgormley
 * 
 */
public class ConllLiteToken {

    private static Logger log = LoggerFactory.getLogger(ConllLiteToken.class);

    private static final Pattern tab = Pattern.compile("\"?\t\"?");
    private static final Pattern comma = Pattern.compile("\\s*,\\s*");
    private static final Pattern comment = Pattern.compile("^\\s*#");
    
    // Field number:    Field name:     Description:
    /** 1    ID  Unique ID for this sentence no always numeric. */
    private String id;
    /** 2    FORM    Word form or punctuation symbol. */
    private String form;
    /** 3...   EDGEs    Columns containing labeled edges, where the first string is the ID of the parent, and the second is the label on the edge. */
    private List<String[]> edges;
    /** 4...   COMMENTs     Columns containing comments, which are proceeded by a hash tag, #. */
    private List<String> comments;
    
    public ConllLiteToken(String line) {
        String[] splits = tab.split(line);
        if (splits.length < 2) {
            throw new IllegalStateException("Line is incomplete: " + line);
        }
        id = splits[0].trim();
        form = splits[1].trim();
        edges = new ArrayList<String[]>();
        comments = new ArrayList<String>();
        boolean areComments = false;
        for (int i=2; i<splits.length; i++) {
            String col = splits[i].trim();
            if ("".equals(col)) {
                continue;
            }
            if (comment.matcher(col).find()) {
                areComments = true;
            }
            
            if (areComments) {
                comments.add(col);
            } else {
                if (col.length() < 2) {
                    log.warn("Column entry doesn't look like an edge: " + col);
                }
                // This is an edge.
                edges.add(comma.split(col));                
            }
        }
    }
    
    public ConllLiteToken(String id, String form, List<String[]> edges, List<String> comments) {
        super();
        this.id = id;
        this.form = form;
        this.edges = edges;
        this.comments = comments;
    }

    /** Deep copy constructor */
    public ConllLiteToken(ConllLiteToken other) {
        this.id = other.id;
        this.form = other.form;
        this.edges = new ArrayList<String[]>();
        for (String[] edge : other.edges) {
            this.edges.add(Arrays.copyOf(edge, edge.length));
        }
        this.comments = new ArrayList<String>(other.comments);
    }
    
    @Override
    public String toString() {
        return "ConllLiteToken [id=" + id + ", form=" + form + ", edges=" + edges + ", comments=" + comments + "]";
    }

    public void write(Writer writer) throws IOException {
        final String sep = "\t";

        writer.write(String.format("%s", id));
        writer.write(sep);
        writer.write(String.format("%s", form));
        writer.write(sep);
        for (int i=0; i<edges.size(); i++) {
            String[] e = edges.get(i);
            writer.write(StringUtils.join(e, ", "));
            if (i < edges.size() - 1) {
                writer.write(sep);
            }
        }
        for (int i=0; i<comments.size(); i++) {
            String c = comments.get(i);
            writer.write(c);
            if (i < comments.size() - 1) {
                writer.write(sep);
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getForm() {
        return form;
    }

    public List<String[]> getEdges() {
        return edges;
    }

    public List<String> getComments() {
        return comments;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setForm(String form) {
        this.form = form;
    }
    
    public void setEdges(List<String[]> edges) {
        this.edges = edges;
    }
    
    public void setComments(List<String> comments) {
        this.comments = comments;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((comments == null) ? 0 : comments.hashCode());
        result = prime * result + ((edges == null) ? 0 : edges.hashCode());
        result = prime * result + ((form == null) ? 0 : form.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
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
        ConllLiteToken other = (ConllLiteToken) obj;
        if (comments == null) {
            if (other.comments != null)
                return false;
        } else if (!comments.equals(other.comments))
            return false;
        if (edges == null) {
            if (other.edges != null)
                return false;
        } else if (!edges.equals(other.edges))
            return false;
        if (form == null) {
            if (other.form != null)
                return false;
        } else if (!form.equals(other.form))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
        
}
