package doublemoon.mahjongcraft.paper.render;

import doublemoon.mahjongcraft.paper.model.MahjongTile;
import java.util.List;

public record MeldView(List<MahjongTile> tiles, List<Boolean> faceDownFlags) {
    public boolean faceDownAt(int index) {
        return index >= 0 && index < this.faceDownFlags.size() && this.faceDownFlags.get(index);
    }
}
