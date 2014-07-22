package edu.jhu.nlp.tag;

import edu.jhu.data.DepTreebank;

public interface TagReducer {

    void reduceTags(DepTreebank trees);

}