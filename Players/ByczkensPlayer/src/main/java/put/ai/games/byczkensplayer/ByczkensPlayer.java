package put.ai.games.byczkensplayer;

import java.util.List;
import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;

public class ByczkensPlayer extends Player {

    private static final int INF = 1000000000;

    @Override
    public String getName() {
        return "Lukasz Bartkowiak 160219 Michal Byczko 160141";
    }

    @Override
    public Move nextMove(Board b) {
        List<Move> moves = b.getMovesFor(getColor());
        if (moves.isEmpty()) {
            return null;
        }

        Move bestMove = moves.get(0);
        int alpha = -INF;
        int beta = INF;
        int maxVal = -INF;
        int depth = 4;

        for (Move m : moves) {
            Board next = b.clone();
            next.doMove(m);
            
            int val = -negamax(next, depth - 1, -beta, -alpha, getOpponent(getColor()));
            
            if (val > maxVal) {
                maxVal = val;
                bestMove = m;
            }
            alpha = Math.max(alpha, val);
        }

        return bestMove;
    }

    private int negamax(Board b, int depth, int alpha, int beta, Color currentPlayer) {
        List<Move> moves = b.getMovesFor(currentPlayer);
        
        if (depth == 0 || moves.isEmpty()) {
            return evaluate(b, currentPlayer);
        }

        int maxVal = -INF;
        for (Move m : moves) {
            Board next = b.clone();
            next.doMove(m);
            
            int val = -negamax(next, depth - 1, -beta, -alpha, getOpponent(currentPlayer));
            
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
        Color opp = getOpponent(player);

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

    private Color getOpponent(Color player) {
        return (player == Color.PLAYER1) ? Color.PLAYER2 : Color.PLAYER1;
    }
}