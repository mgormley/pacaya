package edu.jhu.tag;

import edu.jhu.data.DepTreebank;

public interface TagReducer {

    void reduceTags(DepTreebank trees);

}