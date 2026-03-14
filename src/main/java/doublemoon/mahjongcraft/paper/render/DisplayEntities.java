package doublemoon.mahjongcraft.paper.render;

import doublemoon.mahjongcraft.paper.model.MahjongTile;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class DisplayEntities {
    private static final String ITEM_MODEL_NAMESPACE = "mahjongcraft";
    private static final float TILE_SCALE = 0.15F;

    private DisplayEntities() {
    }

    public static ItemDisplay spawnTileDisplay(
        Plugin plugin,
        Location location,
        float yaw,
        MahjongTile tile,
        boolean faceDown,
        DisplayClickAction clickAction,
        boolean visibleByDefault
    ) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Location world is null");
        }

        ItemDisplay display = world.spawn(location, ItemDisplay.class, spawned -> {
            spawned.setPersistent(false);
            spawned.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
            spawned.setInterpolationDuration(1);
            spawned.setInterpolationDelay(0);
            spawned.setTeleportDuration(1);
            spawned.setViewRange(32.0F);
            spawned.setShadowRadius(0.0F);
            spawned.setShadowStrength(0.0F);
            spawned.setBrightness(new Display.Brightness(15, 15));
            spawned.setDisplayWidth(0.4F);
            spawned.setDisplayHeight(0.6F);
            spawned.setRotation(yaw, 0.0F);
            spawned.setVisibleByDefault(visibleByDefault);
            spawned.setTransformation(new Transformation(
                new Vector3f(),
                new AxisAngle4f((float) Math.toRadians(90), 1.0F, 0.0F, 0.0F),
                new Vector3f(TILE_SCALE, TILE_SCALE, TILE_SCALE),
                new AxisAngle4f()
            ));
            spawned.customName(Component.text(tile.name()));
            spawned.setCustomNameVisible(false);
            spawned.setItemStack(tileItem(tile, faceDown));
        });

        if (clickAction != null) {
            TableDisplayRegistry.register(display.getEntityId(), clickAction);
        }
        return display;
    }

    public static TextDisplay spawnLabel(Location location, Component text, Color color) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Location world is null");
        }

        return world.spawn(location, TextDisplay.class, display -> {
            display.setPersistent(false);
            display.text(text);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setDefaultBackground(false);
            display.setBillboard(Display.Billboard.CENTER);
            display.setLineWidth(120);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setBackgroundColor(color);
        });
    }

    private static ItemStack tileItem(MahjongTile tile, boolean faceDown) {
        String path = faceDown ? "mahjong_tile/back" : tile.itemModelPath();
        ItemStack itemStack = new ItemStack(Material.PAPER);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setItemModel(new NamespacedKey(ITEM_MODEL_NAMESPACE, path));
        meta.displayName(Component.text(tile.name()));
        itemStack.setItemMeta(meta);
        return itemStack;
    }
}
