package edu.jhu.srl;

import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.data.simple.AnnoSentenceCollection;
import edu.jhu.eval.DepParseEvaluator;
import edu.jhu.gm.app.IdxLoss;
import edu.jhu.gm.data.LFgExample;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.srl.JointNlpDecoder.JointNlpDecoderPrm;

public class DepParseIdxLoss implements IdxLoss {
    
    private AnnoSentenceCollection goldSents;
    private JointNlpDecoderPrm dePrm;
    
    public DepParseIdxLoss(AnnoSentenceCollection goldSents, JointNlpDecoderPrm dePrm) {
        this.goldSents = goldSents;
        this.dePrm = dePrm;
    }
    
    @Override
    public double getLoss(int i, LFgExample ex, FgInferencer infLatPred) {
        JointNlpDecoder decoder = new JointNlpDecoder(dePrm);
        decoder.decode(infLatPred, ex);
        AnnoSentence pred = new AnnoSentence();
        pred.setParents(decoder.getParents());
        AnnoSentence gold = goldSents.get(i);
        DepParseEvaluator loss = new DepParseEvaluator("NO_NAME");
        return loss.loss(pred, gold);
    }

}
