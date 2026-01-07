package put.ai.games.byczkensplayer;

import java.util.List;
import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;

public class ByczkensPlayer extends Player {

    @Override
    public String getName() {
        return "Lukasz Bartkowiak 160219 Michal Byczko 160141";
    }

    @Override
    public Move nextMove(Board b) {
        List<Move> moves = b.getMovesFor(getColor());
        
        if (!moves.isEmpty()) {
            return moves.get(0);
        }
        
        return null;
    }
}