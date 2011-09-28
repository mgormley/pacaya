package edu.jhu.hltcoe.data;

public interface TagReducer {

    public abstract void reduceTags(DepTreebank trees);

    public abstract void reduceTags(DepTree tree);

}