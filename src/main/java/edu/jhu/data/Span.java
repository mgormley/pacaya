package edu.jhu.data;


/**
 * Mutable span with start and end indices.
 * 
 * @author mgormley
 */
public class Span {

    protected int start;
    protected int end;
        
    public Span(int start, int end) {
        super();
        this.start = start;
        this.end = end;
    }
    
    public boolean isBefore(Span other) {
        if (this.start < other.start) {
            return true;
        }
        return false;
    }

    /**
     * Gets the span intervening between two spans.
     */
    public static Span getSpanBtwn(Span span1, Span span2) {
        Span btwn;
        if (span1.isBefore(span2)) {
            btwn = new Span(span1.end(), span2.start());
        } else {
            btwn = new Span(span2.end(), span1.start());
        }
        return btwn;
    }
    
    public int start() {
        return start;
    }
    public void setStart(int start) {
        this.start = start;
    }
    public int end() {
        return end;
    }
    public void setEnd(int end) {
        this.end = end;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + end;
        result = prime * result + start;
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
        Span other = (Span) obj;
        if (end != other.end)
            return false;
        if (start != other.start)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Span [start=" + start + ", end=" + end + "]";
    }    
    
}
