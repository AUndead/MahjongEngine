package doublemoon.mahjongcraft.paper.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketPlayReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import doublemoon.mahjongcraft.paper.MahjongPaperPlugin;
import doublemoon.mahjongcraft.paper.render.DisplayClickAction;
import doublemoon.mahjongcraft.paper.render.TableDisplayRegistry;
import doublemoon.mahjongcraft.paper.table.MahjongTableManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PacketEventsBridge {
    private final MahjongPaperPlugin plugin;
    private final MahjongTableManager tableManager;
    private final SimplePacketListenerAbstract listener;

    public PacketEventsBridge(MahjongPaperPlugin plugin, MahjongTableManager tableManager) {
        this.plugin = plugin;
        this.tableManager = tableManager;
        this.listener = new SimplePacketListenerAbstract(PacketListenerPriority.NORMAL) {
            @Override
            public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
                if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
                    return;
                }
                WrapperPlayClientInteractEntity interaction = new WrapperPlayClientInteractEntity(event);
                if (interaction.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                    return;
                }

                DisplayClickAction action = TableDisplayRegistry.get(interaction.getEntityId());
                if (action == null) {
                    return;
                }

                Player player = event.getPlayer();
                Bukkit.getScheduler().runTask(PacketEventsBridge.this.plugin, () -> {
                    boolean accepted = PacketEventsBridge.this.tableManager.clickTile(
                        player,
                        action.tableId(),
                        action.ownerId(),
                        action.tileIndex()
                    );
                    if (!accepted) {
                        player.sendActionBar(Component.text("现在还不能操作这张牌"));
                    }
                });
            }
        };
    }

    public void enable() {
        PacketEvents.getAPI().getEventManager().registerListener(this.listener);
    }

    public void disable() {
        PacketEvents.getAPI().getEventManager().unregisterListener(this.listener);
    }
}
