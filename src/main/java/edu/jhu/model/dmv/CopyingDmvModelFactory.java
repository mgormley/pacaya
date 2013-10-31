package edu.jhu.model.dmv;

import edu.jhu.data.Label;
import edu.jhu.util.Alphabet;

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
