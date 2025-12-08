package ai.synalix.synalixai.enums;

/**
 * Model type enumeration
 * Defines the types of models in the system
 */
public enum ModelType {
    
    /**
     * Base model from HuggingFace, used as foundation for fine-tuning
     */
    BASE,
    
    /**
     * Fine-tuned model checkpoint created by training
     */
    CHECKPOINT
}