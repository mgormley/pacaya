package edu.jhu.nlp.relations;

import edu.jhu.util.cli.Opt;

public class RelationsOptions {

    public enum EmbFeatType { HEAD_ONLY, HEAD_TYPE, FULL }

    // Relation settings.
    @Opt(hasArg=true, description="Whether to predict the roles for the arguments (i.e. direction of the relation).")
    public static boolean predictArgRoles = true;
    @Opt(hasArg=true, description="Whether to use the relation subtypes.")
    public static boolean useRelationSubtype = false;
    @Opt(hasArg=true, description="The maximum number of intervening entities to allow when generating negative examples.")
    public static int maxInterveningEntities = Integer.MAX_VALUE;
    @Opt(hasArg=true, description="Whether to remove entity types from mentions (odd setting, but needed for compat w/PM13).")
    public static boolean removeEntityTypes = false;
    @Opt(hasArg=true, description="Whether to shorten entity mention spans following Zhou et al. (2005)")
    public static boolean shortenEntityMentions = true;
    
    // Relation features.
    @Opt(hasArg=true, description="Whether to use the standard binary features.")
    public static boolean useZhou05Features = true;
    @Opt(hasArg=true, description="Whether to use the embedding features.")
    public static boolean useEmbeddingFeatures = true;
    @Opt(hasArg=true, description="The feature set for embeddings.")
    public static EmbFeatType embFeatType = EmbFeatType.FULL;
        
    private RelationsOptions() {
        
    }
    
}
