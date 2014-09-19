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
    
    // Relation features.
    @Opt(hasArg=true, description="Whether to use the embedding features.")
    public static boolean useEmbeddingFeatures = true;
    @Opt(hasArg=true, description="The feature set for embeddings.")
    public static EmbFeatType embFeatType = EmbFeatType.FULL;
    
    
    
    // ---------------------------------------------
    // IGNORED OPTIONS << TODO: Support these for relation extraction experiments.
    // ---------------------------------------------
    //
    @Opt(hasArg=true, description="Whether to use the POS tag features.")
    public static boolean usePosTagFeatures = true;
    @Opt(hasArg=true, description="Whether to use the syntactic features.")
    public static boolean useSyntaxFeatures = true;
    @Opt(hasArg=true, description="Whether to use the entity type features.")
    public static boolean useEntityTypeFeatures = true;
    //
    // ---------------------------------------------
    
    private RelationsOptions() {
        
    }
    
}
