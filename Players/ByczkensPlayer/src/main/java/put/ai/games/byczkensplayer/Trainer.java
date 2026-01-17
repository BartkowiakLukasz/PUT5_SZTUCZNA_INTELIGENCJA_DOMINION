package put.ai.games.byczkensplayer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import put.ai.games.engine.BoardFactory;
import put.ai.games.engine.GameEngine;
import put.ai.games.engine.impl.GameEngineImpl;
import put.ai.games.game.Player;
import put.ai.games.rulesprovider.RulesProvider;

import put.ai.games.byczkensbartkowiak.OurPlayer;
import put.ai.games.nastyplayer.NastyPlayer;

public class Trainer {

    private static final int POPULATION_SIZE = 32;
    private static final int ELITISM_COUNT = 4;
    private static final int GAMES_VS_CHAMPION = 2;
    private static final int GENERATIONS = 5000;
    private static final int TIMEOUT_MS = 150;

    private static final String LOG_FILE = "training_log.txt";

    public static void main(String[] args) {
        System.out.println("=== NIGHT TRAINING MODE STARTED === turbo");
        System.out.println("Logs will be saved to: " + LOG_FILE);
        Trainer trainer = new Trainer();
        trainer.trainGenetic();
    }

    private Random random = new Random();

    private static class Genome implements Comparable<Genome> {
        ByczkensWeights weights;
        int score;

        Genome(ByczkensWeights weights) {
            this.weights = weights;
            this.score = 0;
        }

        @Override
        public int compareTo(Genome o) {
            return Integer.compare(o.score, this.score);
        }
    }

    public void trainGenetic() {
        ByczkensWeights championWeights = new ByczkensWeights();
        ByczkensWeights allTimeBestWeights = championWeights;
        int allTimeBestScore = 0;

        List<Genome> population = new ArrayList<>();

        logToFile("STARTING TRAINING SESSION. Initial weights: " + championWeights);

        for (int i = 0; i < POPULATION_SIZE; i++) {
            population.add(new Genome(mutate(championWeights, 0.45)));
        }

        long startTime = System.currentTimeMillis();

        for (int gen = 0; gen < GENERATIONS; gen++) {
            final ByczkensWeights currentChampion = championWeights;

            population.parallelStream().forEach(genome -> {
                int points = 0;
                Player challenger = new ByczkensPlayer(genome.weights);

                points += playSeries(challenger, new ByczkensPlayer(currentChampion), GAMES_VS_CHAMPION);

                points += playSeries(challenger, new OurPlayer(), 2);

                points += playSeries(challenger, new NastyPlayer(), 2);

                genome.score = points;
            });

            Collections.sort(population);
            Genome bestInGen = population.get(0);

            double avgScore = population.stream().mapToInt(g -> g.score).average().orElse(0);
            int maxPossibleScore = GAMES_VS_CHAMPION + 2 + 2;

            System.out.printf("[GEN %d] Best: %d/%d, Avg: %.1f%n", gen, bestInGen.score, maxPossibleScore, avgScore);

            championWeights = bestInGen.weights;

            boolean newRecord = false;
            if (bestInGen.score > allTimeBestScore) {
                allTimeBestScore = bestInGen.score;
                allTimeBestWeights = bestInGen.weights;
                newRecord = true;
                System.out.println(">>> NEW ALL-TIME RECORD!");
            }

            if (newRecord || gen % 10 == 0) {
                saveLog(gen, bestInGen, allTimeBestWeights);
            }

            List<Genome> nextGen = new ArrayList<>();

            for (int i = 0; i < ELITISM_COUNT; i++) {
                nextGen.add(new Genome(population.get(i).weights));
            }

            while (nextGen.size() < POPULATION_SIZE) {
                ByczkensWeights parentA = population.get(random.nextInt(POPULATION_SIZE / 2)).weights;
                ByczkensWeights parentB = population.get(random.nextInt(POPULATION_SIZE / 2)).weights;

                ByczkensWeights childW = crossover(parentA, parentB);

                double mutationRate = (gen < 200) ? 0.2 : 0.12;
                childW = mutate(childW, mutationRate);

                nextGen.add(new Genome(childW));
            }
            population = nextGen;
        }

        long duration = (System.currentTimeMillis() - startTime) / 1000;
        logToFile("TRAINING FINISHED in " + duration + "s.");
        logToFile("FINAL ALL-TIME BEST WEIGHTS: " + allTimeBestWeights);
        System.out.println("Done. Check " + LOG_FILE);
    }

    private void saveLog(int gen, Genome bestInGen, ByczkensWeights allTimeBest) {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            out.printf("[%s] GEN %d | BestScore: %d%n", time, gen, bestInGen.score);
            out.println("CURRENT_BEST: " + bestInGen.weights);
            out.println("ALL_TIME_BEST: " + allTimeBest);
            out.println("--------------------------------------------------");
        } catch (IOException e) {
            System.err.println("Save failed: " + e.getMessage());
        }
    }

    private void logToFile(String msg) {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            out.println(msg);
        } catch (IOException e) {
        }
    }

    private int playSeries(Player p1, Player p2, int gamesCount) {
        int wins = 0;
        for (int i = 0; i < gamesCount; i++) {
            boolean p1First = (i % 2 == 0);
            double res = playGame(p1First ? p1 : p2, p1First ? p2 : p1, TIMEOUT_MS);
            if (res == 1.0) {
                if (p1First)
                    wins++;
            } else if (res == 0.0) {
                if (!p1First)
                    wins++;
            }
        }
        return wins;
    }

    private ByczkensWeights crossover(ByczkensWeights a, ByczkensWeights b) {
        return new ByczkensWeights(
                random.nextBoolean() ? a.cornerValue : b.cornerValue,
                random.nextBoolean() ? a.mobilityWeight : b.mobilityWeight,
                random.nextBoolean() ? a.cloneBonus : b.cloneBonus,
                random.nextBoolean() ? a.deathZonePenalty : b.deathZonePenalty,
                random.nextBoolean() ? a.materialWeight : b.materialWeight,
                random.nextBoolean() ? a.opponentMobilityWeight : b.opponentMobilityWeight,
                random.nextBoolean() ? a.captureMultiplier : b.captureMultiplier,
                random.nextBoolean() ? a.fullnessThreshold : b.fullnessThreshold,
                random.nextBoolean() ? a.lowFullnessMaterialWeight : b.lowFullnessMaterialWeight,
                random.nextBoolean() ? a.mobilityThreshold : b.mobilityThreshold,
                random.nextBoolean() ? a.edgeBonus : b.edgeBonus,
                random.nextBoolean() ? a.clusteringBonus : b.clusteringBonus);
    }

    private ByczkensWeights mutate(ByczkensWeights w, double intensity) {
        ByczkensWeights newW = new ByczkensWeights(
                w.cornerValue, w.mobilityWeight, w.cloneBonus, w.deathZonePenalty, w.materialWeight,
                w.opponentMobilityWeight, w.captureMultiplier, w.fullnessThreshold, w.lowFullnessMaterialWeight,
                w.mobilityThreshold, w.edgeBonus, w.clusteringBonus);

        int mutationIdx = random.nextInt(12);
        double factor = 1.0 + (random.nextDouble() * 2 * intensity) - intensity;

        switch (mutationIdx) {
            case 0: newW.cornerValue *= factor; break;
            case 1: newW.mobilityWeight *= factor; break;
            case 2: newW.cloneBonus *= factor; break;
            case 3: newW.deathZonePenalty *= factor; break;
            case 4: newW.materialWeight *= factor; break;
            case 5: newW.opponentMobilityWeight *= factor; break;
            case 6: newW.captureMultiplier *= factor; break;
            case 7: newW.fullnessThreshold = clamp(newW.fullnessThreshold * factor); break;
            case 8: newW.lowFullnessMaterialWeight *= factor; break;
            case 9: newW.mobilityThreshold = clamp(newW.mobilityThreshold * factor); break;
            case 10: newW.edgeBonus *= factor; break;
            case 11: newW.clusteringBonus *= factor; break;
        }
        return newW;
    }

    private double clamp(double val) {
        return Math.max(0.01, Math.min(0.99, val));
    }

    private double playGame(Player p1, Player p2, int timeout) {
        try {
            BoardFactory factory = RulesProvider.INSTANCE.getRulesByName("Dominion");
            java.util.Map<String, Object> config = new java.util.HashMap<>();
            config.put(BoardFactory.BOARD_SIZE, 8);
            factory.configure(config);

            GameEngine engine = new GameEngineImpl(factory);
            engine.setTimeout(timeout);
            engine.addPlayer(p1, 0);
            engine.addPlayer(p2, 1);

            put.ai.games.game.Player.Color winner = engine.play(null);
            if (winner == p1.getColor())
                return 1.0;
            if (winner == p2.getColor())
                return 0.0;
            return 0.5;
        } catch (Exception e) {
            return 0.5;
        }
    }
}