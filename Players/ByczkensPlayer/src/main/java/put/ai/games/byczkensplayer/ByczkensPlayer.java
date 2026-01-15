package put.ai.games.byczkensplayer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.ArrayList;
import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;
import put.ai.games.game.moves.MoveMove;
import put.ai.games.game.moves.PlaceMove;
import put.ai.games.game.moves.SkipMove;

public class ByczkensPlayer extends Player {

    private static final int INF = 1000000000;

    private static final long TIME_BUFFER_LONG = 500;
    private static final long TIME_BUFFER_MEDIUM = 200;
    private static final long TIME_BUFFER_SHORT = 50;

    private static final int[] D_ROW = { -1, -1, -1, 0, 0, 1, 1, 1 };
    private static final int[] D_COL = { -1, 0, 1, -1, 1, -1, 0, 1 };

    private final ByczkensWeights weights;

    public ByczkensPlayer() {
        this.weights = new ByczkensWeights();
    }

    public ByczkensPlayer(ByczkensWeights weights) {
        this.weights = weights;
    }

    @Override
    public String getName() {
        return "Lukasz Bartkowiak 160219 Michal Byczko 160141";
    }

    @Override
    public Move nextMove(Board b) {
        long startTime = System.currentTimeMillis();
        long limit = getLimit(getTime());
        long deadline = startTime + limit;

        Move bestMove = null;
        int currentDepth = 1;
        int maxDepth = 100;

        try {
            while (true) {
                if (System.currentTimeMillis() > deadline)
                    break;

                Move val = negamaxRoot(b, currentDepth, deadline);
                if (val != null) {
                    bestMove = val;
                }
                currentDepth++;
                if (currentDepth > maxDepth)
                    break;
            }
        } catch (TimeoutException e) {
        }

        if (bestMove == null) {
            List<Move> m = b.getMovesFor(getColor());
            if (!m.isEmpty())
                bestMove = m.get(0);
        }

        return bestMove;
    }

    private long getLimit(long time) {
        if (time > 5000)
            return time - TIME_BUFFER_LONG;
        if (time > 1000)
            return time - TIME_BUFFER_MEDIUM;
        return time - TIME_BUFFER_SHORT;
    }

    private Move negamaxRoot(Board b, int depth, long deadline) throws TimeoutException {
        List<Move> moves = getSmartMoves(b, getColor());
        if (moves.isEmpty())
            return null;

        Color myColor = getColor();
        sortMoves(moves, b, myColor);

        Move best = moves.get(0);
        int alpha = -INF;
        int beta = INF;
        int maxVal = -INF;

        for (Move m : moves) {
            if (System.currentTimeMillis() > deadline)
                throw new TimeoutException();

            Board next = b.clone();
            next.doMove(m);

            int val = -negamax(next, depth - 1, -beta, -alpha, myColor, deadline);

            if (val > maxVal) {
                maxVal = val;
                best = m;
            }
            alpha = Math.max(alpha, val);
        }
        return best;
    }

    private int negamax(Board b, int depth, int alpha, int beta, Color lastPlayer, long deadline)
            throws TimeoutException {

        if (System.currentTimeMillis() > deadline)
            throw new TimeoutException();

        Color currentPlayer = (lastPlayer == Color.PLAYER1) ? Color.PLAYER2 : Color.PLAYER1;

        if (depth == 0) {
            return evaluate(b, currentPlayer);
        }

        List<Move> moves = getSmartMoves(b, currentPlayer);
        if (moves.isEmpty()) {
            return evaluate(b, currentPlayer);
        }

        if (depth > 1) {
            sortMoves(moves, b, currentPlayer);
        }

        int maxVal = -INF;
        for (Move m : moves) {
            Board next = b.clone();
            next.doMove(m);

            int val = -negamax(next, depth - 1, -beta, -alpha, currentPlayer, deadline);

            if (val > maxVal) {
                maxVal = val;
            }
            alpha = Math.max(alpha, val);
            if (alpha >= beta)
                break;
        }
        return maxVal;
    }

    private int evaluate(Board b, Color player) {
        int size = b.getSize();
        int myCount = 0;
        int oppCount = 0;

        Color me = player;
        Color opp = (player == Color.PLAYER1) ? Color.PLAYER2 : Color.PLAYER1;

        Color[][] grid = new Color[size][size];

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Color s = b.getState(r, c);
                grid[r][c] = s;
                if (s == me)
                    myCount++;
                else if (s == opp)
                    oppCount++;
            }
        }

        int score = 0;

        int meat = myCount - oppCount;
        double fullness = (double) (myCount + oppCount) / (size * size);
        int meatWeight = (fullness > weights.fullnessThreshold) ? weights.materialWeight
                : weights.lowFullnessMaterialWeight;
        score += meat * meatWeight;

        int[] cornersR = { 0, 0, size - 1, size - 1 };
        int[] cornersC = { 0, size - 1, 0, size - 1 };

        for (int i = 0; i < 4; i++) {
            Color s = grid[cornersR[i]][cornersC[i]];
            if (s != Color.EMPTY) {
                if (s == me)
                    score += weights.cornerValue;
                else
                    score -= weights.cornerValue;
            }
        }

        int myMob = 0;
        int oppMob = 0;
        int myStructure = 0;
        int oppStructure = 0;

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Color s = grid[r][c];

                if (s == Color.EMPTY) {
                    boolean adjMe = false;
                    boolean adjOpp = false;
                    for (int k = 0; k < 8; k++) {
                        int tr = r + D_ROW[k];
                        int tc = c + D_COL[k];
                        if (isValid(tr, tc, size)) {
                            Color neighbor = grid[tr][tc];
                            if (neighbor == me)
                                adjMe = true;
                            else if (neighbor == opp)
                                adjOpp = true;
                        }
                        if (adjMe && adjOpp)
                            break;
                    }
                    if (adjMe)
                        myMob++;
                    if (adjOpp)
                        oppMob++;

                } else {
                    int pieceVal = 0;
                    if (r == 0 || r == size - 1 || c == 0 || c == size - 1)
                        pieceVal += weights.edgeBonus;

                    for (int k = 0; k < 8; k++) {
                        int tr = r + D_ROW[k];
                        int tc = c + D_COL[k];
                        if (isValid(tr, tc, size) && grid[tr][tc] == s) {
                            pieceVal += weights.clusteringBonus;
                        }
                    }

                    if (s == me)
                        myStructure += pieceVal;
                    else
                        oppStructure += pieceVal;
                }
            }
        }

        for (int k = 0; k < 4; k++) {
            int cr = cornersR[k];
            int cc = cornersC[k];

            if (grid[cr][cc] == Color.EMPTY) {
                int rDir = (cr == 0) ? 1 : -1;
                int cDir = (cc == 0) ? 1 : -1;

                int[][] dangerSpots = {
                        { cr + rDir, cc },
                        { cr, cc + cDir },
                        { cr + rDir, cc + cDir }
                };

                for (int[] d : dangerSpots) {
                    int dr = d[0];
                    int dc = d[1];
                    if (isValid(dr, dc, size) && grid[dr][dc] != Color.EMPTY) {
                        Color owner = grid[dr][dc];
                        boolean winningBig = (myCount - oppCount) > 10;
                        if (!winningBig) {
                            if (owner == me)
                                score -= weights.deathZonePenalty;
                            else
                                score += weights.deathZonePenalty;
                        }
                    }
                }
            }
        }

        int mobDiff = myMob - oppMob;
        int structDiff = myStructure - oppStructure;

        if (fullness < weights.mobilityThreshold)
            score += mobDiff * weights.mobilityWeight;

        score += structDiff;

        return score;
    }

    private boolean isValid(int r, int c, int size) {
        return r >= 0 && r < size && c >= 0 && c < size;
    }

    private void sortMoves(List<Move> moves, Board b, Color player) {
        Collections.sort(moves, (m1, m2) -> {
            int v1 = estimateMoveValue(m1, b, player);
            int v2 = estimateMoveValue(m2, b, player);
            return v2 - v1; 
        });
    }

    private int estimateMoveValue(Move m, Board b, Color player) {
        int val = 0;
        int rDst = -1, cDst = -1;

        if (m instanceof PlaceMove) {
            PlaceMove pm = (PlaceMove) m;
            rDst = pm.getX();
            cDst = pm.getY();
            val += weights.cloneBonus;
        } else if (m instanceof MoveMove) {
            MoveMove mm = (MoveMove) m;
            rDst = mm.getDstX();
            cDst = mm.getDstY();
        }

        if (rDst != -1) {
            int size = b.getSize();
            if ((rDst == 0 || rDst == size - 1) && (cDst == 0 || cDst == size - 1)) {
                val += weights.cornerValue / 5;
            }

            int caps = countCaptures(b, rDst, cDst, player);
            val += caps * weights.captureMultiplier;
        }
        return val;
    }

    private int countCaptures(Board b, int r, int c, Color player) {
        int size = b.getSize();
        int count = 0;
        Color myself = player;
        Color enemy = (player == Color.PLAYER1) ? Color.PLAYER2 : Color.PLAYER1;

        for (int i = 0; i < 8; i++) {
            int tr = r + D_ROW[i];
            int tc = c + D_COL[i];
            if (isValid(tr, tc, size)) {
                if (b.getState(tr, tc) == enemy) {
                    count++;
                }
            }
        }
        for (int i = 0; i < 8; i++) {
            int dr = D_ROW[i];
            int dc = D_COL[i];
            int k = 1;
            int foundFriendlyK = -1;

            while (true) {
                int tr = r + k * dr;
                int tc = c + k * dc;
                if (!isValid(tr, tc, size))
                    break;

                Color s = b.getState(tr, tc);
                if (s == myself) {
                    foundFriendlyK = k;
                    break;
                }
                if (s == Color.EMPTY)
                    break;

                k++;
            }

            if (foundFriendlyK >= 3) {
                boolean allEnemies = true;
                for (int j = 2; j < foundFriendlyK; j++) {
                    int tr = r + j * dr;
                    int tc = c + j * dc;
                    if (b.getState(tr, tc) != enemy) {
                        allEnemies = false;
                        break;
                    }
                }
                if (allEnemies) {
                    count += (foundFriendlyK - 2);
                }
            }
        }

        return count;
    }
    private List<Move> getSmartMoves(Board b, Color color) {
        List<Move> myMoves = b.getMovesFor(color);

        Color opp = (color == Color.PLAYER1) ? Color.PLAYER2 : Color.PLAYER1;
        List<Move> oppMoves = b.getMovesFor(opp);

        boolean isOpponentStuck = oppMoves.isEmpty() ||
                (oppMoves.size() == 1 && oppMoves.get(0) instanceof SkipMove);

        if (isOpponentStuck) {
            List<Move> onlyClones = new ArrayList<>();
            for (Move m : myMoves) {
                if (m instanceof PlaceMove) {
                    onlyClones.add(m);
                }
            }
            if (!onlyClones.isEmpty()) {
                return onlyClones;
            }
        }

        return myMoves;
    }
}