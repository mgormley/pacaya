package edu.jhu.data;

import java.io.Serializable;

public interface Label extends Comparable<Label>, Serializable {
    
    String getLabel();
    
}
