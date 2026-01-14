package put.ai.games.byczkensplayer;

import java.util.List;
import java.util.concurrent.TimeoutException;
import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;

public class ByczkensPlayer extends Player {

    private static final int INF = 1000000000;

    private static final long TIME_BUFFER_LONG = 500;
    private static final long TIME_BUFFER_MEDIUM = 200;
    private static final long TIME_BUFFER_SHORT = 50;

    private static final int[] D_ROW = { -1, -1, -1, 0, 0, 1, 1, 1 };
    private static final int[] D_COL = { -1, 0, 1, -1, 1, -1, 0, 1 };

    private final int cornerValue = 4927;
    private final int deathZonePenalty = 4109;
    private final int mobilityWeight = 33;
    private final int edgeBonus = 19;
    private final int tempMaterialWeight = 100;

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
                if (System.currentTimeMillis() > deadline) break;

                Move val = negamaxRoot(b, currentDepth, deadline);
                if (val != null) {
                    bestMove = val;
                }
                currentDepth++;
                if (currentDepth > maxDepth) break;
            }
        } catch (TimeoutException e) {
        }

        if (bestMove == null) {
            List<Move> m = b.getMovesFor(getColor());
            if (!m.isEmpty()) bestMove = m.get(0);
        }

        return bestMove;
    }

    private long getLimit(long time) {
        if (time > 5000) return time - TIME_BUFFER_LONG;
        if (time > 1000) return time - TIME_BUFFER_MEDIUM;
        return time - TIME_BUFFER_SHORT;
    }

    private Move negamaxRoot(Board b, int depth, long deadline) throws TimeoutException {
        List<Move> moves = b.getMovesFor(getColor());
        if (moves.isEmpty()) return null;

        Move best = moves.get(0);
        int alpha = -INF;
        int beta = INF;
        int maxVal = -INF;

        for (Move m : moves) {
            if (System.currentTimeMillis() > deadline) throw new TimeoutException();

            Board next = b.clone();
            next.doMove(m);

            int val = -negamax(next, depth - 1, -beta, -alpha, getColor(), deadline);

            if (val > maxVal) {
                maxVal = val;
                best = m;
            }
            alpha = Math.max(alpha, val);
        }
        return best;
    }

    private int negamax(Board b, int depth, int alpha, int beta, Color lastPlayer, long deadline) throws TimeoutException {
        if (System.currentTimeMillis() > deadline) throw new TimeoutException();

        Color currentPlayer = (lastPlayer == Color.PLAYER1) ? Color.PLAYER2 : Color.PLAYER1;
        List<Move> moves = b.getMovesFor(currentPlayer);

        if (depth == 0 || moves.isEmpty()) {
            return evaluate(b, currentPlayer);
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
            if (alpha >= beta) break;
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
                if (s == me) myCount++;
                else if (s == opp) oppCount++;
            }
        }

        int score = 0;
        
        int meat = myCount - oppCount;
        score += meat * tempMaterialWeight;

        int[] cornersR = { 0, 0, size - 1, size - 1 };
        int[] cornersC = { 0, size - 1, 0, size - 1 };

        for (int i = 0; i < 4; i++) {
            Color s = grid[cornersR[i]][cornersC[i]];
            if (s != Color.EMPTY) {
                if (s == me) score += cornerValue;
                else score -= cornerValue;
            }
        }

        int myMob = 0;
        int oppMob = 0;

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
                            if (neighbor == me) adjMe = true;
                            else if (neighbor == opp) adjOpp = true;
                        }
                        if (adjMe && adjOpp) break;
                    }
                    if (adjMe) myMob++;
                    if (adjOpp) oppMob++;

                } else {
                    // Bonus za krawędź
                    if (r == 0 || r == size - 1 || c == 0 || c == size - 1) {
                        if (s == me) score += edgeBonus;
                        else score -= edgeBonus;
                    }
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
                        if (owner == me) score -= deathZonePenalty;
                        else score += deathZonePenalty;
                    }
                }
            }
        }

        int mobDiff = myMob - oppMob;
        score += mobDiff * mobilityWeight;

        return score;
    }

    private boolean isValid(int r, int c, int size) {
        return r >= 0 && r < size && c >= 0 && c < size;
    }
}