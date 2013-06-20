package edu.jhu.hltcoe.model.dmv;

import edu.jhu.hltcoe.util.Alphabet;
import edu.jhu.hltcoe.data.Label;

public class CopyingDmvModelFactory extends AbstractDmvModelFactory {

    private DmvModel dmv;
    public CopyingDmvModelFactory(DmvModel dmv) {
        this.dmv = dmv;
    }

    @Override
    public DmvModel getInstance(Alphabet<Label> alphabet) {
        assert(alphabet == dmv.getTagAlphabet());
        DmvModel dmvCopy = new DmvModel(alphabet);
        dmvCopy.copyFrom(dmv);
        return dmvCopy;
    }

}
