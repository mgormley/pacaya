package edu.jhu.nlp.data.conll;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class CoNLL08ConvertToGoldHeads {

    public static void main(String[] args) throws IOException {
        String c5to8dir = "/Users/mgormley/research/other_lib/srl/conll05_to_08";
        {
            String goldFile = c5to8dir + "/original/test.wsj.GOLD.conll08";
            String sysFile = c5to8dir+ "/pos-sup_tpl_coarse1_trainSimplified/test.wsj.conll";
            String outFile = c5to8dir + "/oracle_tpl_coarse1_trainSimplified/test.wsj.conll";        
            readSystemHeadsWriteGoldHeads(goldFile, sysFile, outFile);
        }
        {
            String goldFile = c5to8dir + "/original/test.wsj.GOLD.conll08";
            String sysFile = c5to8dir+ "/pos-sup_tpl_bjork_ig_trainSimplified/test.wsj.conll";
            String outFile = c5to8dir + "/oracle_tpl_bjork_ig_trainSimplified/test.wsj.conll";        
            readSystemHeadsWriteGoldHeads(goldFile, sysFile, outFile);
        }
        {
            String goldFile = c5to8dir + "/lth/test.wsj.missing.conll";
            String sysFile = c5to8dir+ "/pos-sup_tpl_coarse1_trainSimplified/test.wsj.missing.conll";
            String outFile = c5to8dir + "/oracle_tpl_coarse1_trainSimplified/test.wsj.missing.conll";        
            readSystemHeadsWriteGoldHeads(goldFile, sysFile, outFile);
        }
        {
            String goldFile = c5to8dir + "/lth/test.wsj.missing.conll";
            String sysFile = c5to8dir+ "/pos-sup_tpl_bjork_ig_trainSimplified/test.wsj.missing.conll";
            String outFile = c5to8dir + "/oracle_tpl_bjork_ig_trainSimplified/test.wsj.missing.conll";        
            readSystemHeadsWriteGoldHeads(goldFile, sysFile, outFile);
        }
    }

    private static void readSystemHeadsWriteGoldHeads(String goldFile, String sysFile, String outFile)
            throws UnsupportedEncodingException, FileNotFoundException, IOException {
        CoNLL08FileReader goldCr = new CoNLL08FileReader(new FileInputStream(goldFile));
        CoNLL08FileReader sysCr = new CoNLL08FileReader(new FileInputStream(sysFile));

        CoNLL08Writer cw = new CoNLL08Writer(new FileOutputStream(outFile));
        while(sysCr.hasNext()) {
            CoNLL08Sentence sysSent = sysCr.next();
            CoNLL08Sentence goldSent = goldCr.next();            
            assert goldSent.size() == sysSent.size();
            // Get gold heads.
            sysSent.setHeadsFromParents(goldSent.getParentsFromHead());
            cw.write(sysSent);
        }
        assert !goldCr.hasNext();
        assert !sysCr.hasNext();
        
        cw.close();
        sysCr.close();
        goldCr.close();
    }

}
