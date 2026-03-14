package doublemoon.mahjongcraft.paper.render;

import java.util.UUID;

public record DisplayClickAction(String tableId, UUID ownerId, int tileIndex) {
}
