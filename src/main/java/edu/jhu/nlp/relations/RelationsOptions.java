package edu.jhu.nlp.relations;

import edu.jhu.util.cli.Opt;

public class RelationsOptions {

    // ---------------------------------------------
    // IGNORED OPTIONS << TODO: Support these for relation extraction experiments.
    // ---------------------------------------------
    @Opt(hasArg=true, description="Whether to include the path in the embedding template.")
    public static boolean embTmplPath = true;
    @Opt(hasArg=true, description="Whether to include the entity types in the embedding template.")
    public static boolean embTmplType = true;
    @Opt(hasArg=true, description="Whether to include the path words as slots.")
    public static boolean embSlotPath = true;
    @Opt(hasArg=true, description="Whether to include the entity head words as slots.")
    public static boolean embSlotHead = true;    
    // Relation settings.
    @Opt(hasArg=true, description="Whether to predict the roles for the arguments (i.e. direction of the relation).")
    public static boolean predictArgRoles = true;
    //
    @Opt(hasArg=true, description="Whether to use the POS tag features.")
    public static boolean usePosTagFeatures = true;
    @Opt(hasArg=true, description="Whether to use the syntactic features.")
    public static boolean useSyntaxFeatures = true;
    @Opt(hasArg=true, description="Whether to use the entity type features.")
    public static boolean useEntityTypeFeatures = true;
    @Opt(hasArg=true, description="The maximum number of intervening entities to allow when generating negative examples.")
    public static int maxInterveningEntities = Integer.MAX_VALUE;
    //
    @Opt(hasArg=true, description="Whether to use the embedding features.")
    public static boolean useEmbeddingFeatures = true;
    // ---------------------------------------------
    
    private RelationsOptions() {
        
    }
    
}
