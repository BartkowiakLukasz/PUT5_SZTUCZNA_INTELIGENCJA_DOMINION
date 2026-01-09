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
                if (System.currentTimeMillis() > deadline) {
                    break;
                }

                Move val = negamaxRoot(b, currentDepth, deadline);
                if (val != null) {
                    bestMove = val;
                }
                currentDepth++;
                
                if (currentDepth > maxDepth) {
                    break;
                }
            }
        } catch (TimeoutException e) {
        }

        if (bestMove == null) {
            List<Move> m = b.getMovesFor(getColor());
            if (!m.isEmpty()) {
                bestMove = m.get(0);
            }
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
            if (System.currentTimeMillis() > deadline) {
                throw new TimeoutException();
            }

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
        if (System.currentTimeMillis() > deadline) {
            throw new TimeoutException();
        }

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
            if (alpha >= beta) {
                break;
            }
        }
        return maxVal;
    }

    private int evaluate(Board b, Color player) {
        int size = b.getSize();
        int myCount = 0;
        int oppCount = 0;
        Color opp = (player == Color.PLAYER1) ? Color.PLAYER2 : Color.PLAYER1;

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Color s = b.getState(r, c);
                if (s == player) {
                    myCount++;
                } else if (s == opp) {
                    oppCount++;
                }
            }
        }
        
        return myCount - oppCount;
    }
}