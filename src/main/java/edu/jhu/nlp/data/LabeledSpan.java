package edu.jhu.nlp.data;

public class LabeledSpan extends Span {
        
    protected String label;

    public LabeledSpan(int start, int end, String label) {
        super(start, end);
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + end;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + start;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        LabeledSpan other = (LabeledSpan) obj;
        if (end != other.end)
            return false;
        if (label == null) {
            if (other.label != null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        if (start != other.start)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "LabeledSpan [start=" + start + ", end=" + end + ", label=" + label + "]";
    }    
    
}
