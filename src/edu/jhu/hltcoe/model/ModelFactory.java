package edu.jhu.hltcoe.model;

import edu.jhu.hltcoe.data.SentenceCollection;

public interface ModelFactory {

    Model getInstance(SentenceCollection sentences);

}
