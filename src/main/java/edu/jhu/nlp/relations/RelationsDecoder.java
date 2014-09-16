package edu.jhu.nlp.relations;

import java.util.Arrays;
import java.util.List;

import edu.jhu.gm.app.Decoder;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.decode.MbrDecoder;
import edu.jhu.gm.decode.MbrDecoder.MbrDecoderPrm;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.nlp.data.NerMention;
import edu.jhu.nlp.data.RelationMention;
import edu.jhu.nlp.data.RelationMentions;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.relations.RelationsFactorGraphBuilder.RelVar;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.collections.Lists;

public class RelationsDecoder implements Decoder<AnnoSentence, RelationMentions> {
    
    public static class RelationsDecoderPrm {
        // TODO: Set to non-null values.
        public MbrDecoderPrm mbrPrm = null;
    }
    
    private RelationsDecoderPrm prm;
    
    public RelationsDecoder(RelationsDecoderPrm prm) {
        this.prm = prm;
    }
    
    @Override
    public RelationMentions decode(FgInferencer inf, UFgExample ex, AnnoSentence sent) {
        MbrDecoder mbrDecoder = new MbrDecoder(prm.mbrPrm);
        mbrDecoder.decode(inf, ex);
        VarConfig mbrVarConfig = mbrDecoder.getMbrVarConfig();
        // Get the Relations graph.
        return RelationsDecoder.getRelationsGraphFromVarConfig(mbrVarConfig);
    }

    public static RelationMentions getRelationsGraphFromVarConfig(VarConfig mbrVarConfig) {
        int relVarCount = 0;
        RelationMentions rels = new RelationMentions();
        for (Var v : mbrVarConfig.getVars()) {
           if (v instanceof RelVar) {
               RelVar rv = (RelVar) v;
               String relation = mbrVarConfig.getStateName(rv);
               String[] splits = relation.split(":");
               assert splits.length == 3 : Arrays.toString(splits);
               String type = splits[0];               
               List<Pair<String, NerMention>> args = Lists.getList(
                       new Pair<String,NerMention>(splits[1], rv.ment1), 
                       new Pair<String,NerMention>(splits[2], rv.ment2));
               rels.add(new RelationMention(type, null, args, null));
               relVarCount++;
           }
        }

        if (relVarCount > 0) {
            return rels;
        } else {
            return null;
        }
    }

}
