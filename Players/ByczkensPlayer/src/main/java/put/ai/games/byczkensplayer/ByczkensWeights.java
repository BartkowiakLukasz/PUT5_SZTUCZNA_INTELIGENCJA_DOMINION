package put.ai.games.byczkensplayer;

public class ByczkensWeights {
    public int cornerValue;
    public int mobilityWeight;
    public int cloneBonus;
    public int deathZonePenalty;
    public int materialWeight;
    public int opponentMobilityWeight;

    public int captureMultiplier;
    public double fullnessThreshold;
    public int lowFullnessMaterialWeight;
    public double mobilityThreshold;
    public int edgeBonus;
    public int clusteringBonus;

    public ByczkensWeights() {
        this.cornerValue = 4927; 
        this.deathZonePenalty = 4109; 


        this.materialWeight = 1525; 
        this.lowFullnessMaterialWeight = 100;
        this.fullnessThreshold = 0.68; 

        this.mobilityWeight = 33; 
        this.mobilityThreshold = 0.98; 

        this.edgeBonus = 19;
        this.clusteringBonus = 16; 

        this.cloneBonus = 203; 
        this.captureMultiplier = 74; 

        this.opponentMobilityWeight = 23;
    }

    public ByczkensWeights(int cornerValue, int mobilityWeight, int cloneBonus, int deathZonePenalty,
            int materialWeight, int opponentMobilityWeight, int captureMultiplier, double fullnessThreshold,
            int lowFullnessMaterialWeight, double mobilityThreshold, int edgeBonus, int clusteringBonus) {
        this.cornerValue = cornerValue;
        this.mobilityWeight = mobilityWeight;
        this.cloneBonus = cloneBonus;
        this.deathZonePenalty = deathZonePenalty;
        this.materialWeight = materialWeight;
        this.opponentMobilityWeight = opponentMobilityWeight;
        this.captureMultiplier = captureMultiplier;
        this.fullnessThreshold = fullnessThreshold;
        this.lowFullnessMaterialWeight = lowFullnessMaterialWeight;
        this.mobilityThreshold = mobilityThreshold;
        this.edgeBonus = edgeBonus;
        this.clusteringBonus = clusteringBonus;
    }

    @Override
    public String toString() {
        return String.format(
                "Weights{corner=%d, mob=%d, clone=%d, death=%d, mat=%d, oppMob=%d, capMult=%d, fullTh=%.2f, lowMat=%d, mobTh=%.2f, edge=%d, clust=%d}",
                cornerValue, mobilityWeight, cloneBonus, deathZonePenalty, materialWeight, opponentMobilityWeight,
                captureMultiplier, fullnessThreshold, lowFullnessMaterialWeight, mobilityThreshold, edgeBonus,
                clusteringBonus);
    }
}