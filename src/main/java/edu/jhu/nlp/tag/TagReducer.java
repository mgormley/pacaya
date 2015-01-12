package edu.jhu.nlp.tag;

import edu.jhu.nlp.data.DepTreebank;

public interface TagReducer {

    void reduceTags(DepTreebank trees);

}