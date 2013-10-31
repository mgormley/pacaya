package edu.jhu.data;


/** 
 * Implements the hashCode, equals, and compareTo methods based on the getLabel() method.
 * @author mgormley
 */
// TODO: maybe this should be removed?
public abstract class AbstractLabel implements Label {

    private static final long serialVersionUID = -4701339749849686569L;

    public abstract String getLabel();
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getLabel() == null) ? 0 : getLabel().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Label))
            return false;
        Label other = (Label) obj;
        if (getLabel() == null) {
            if (other.getLabel() != null)
                return false;
        } else if (!getLabel().equals(other.getLabel()))
            return false;
        return true;
    }

    @Override
    public int compareTo(Label arg0) {
        return getLabel().compareTo(arg0.getLabel());
    }

}