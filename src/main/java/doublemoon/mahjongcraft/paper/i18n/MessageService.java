package doublemoon.mahjongcraft.paper.i18n;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MessageService {
    private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("zh-CN");

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<Locale, ResourceBundle> bundles = new ConcurrentHashMap<>();

    public Component render(CommandSender sender, String key, TagResolver... placeholders) {
        return this.render(this.resolveLocale(sender), key, placeholders);
    }

    public Component render(Locale locale, String key, TagResolver... placeholders) {
        ResourceBundle bundle = this.bundle(locale);
        String template = bundle.containsKey(key) ? bundle.getString(key) : this.bundle(DEFAULT_LOCALE).getString(key);
        return this.miniMessage.deserialize(template, TagResolver.resolver(placeholders));
    }

    public void send(CommandSender sender, String key, TagResolver... placeholders) {
        sender.sendMessage(this.render(sender, key, placeholders));
    }

    public void actionBar(Player player, String key, TagResolver... placeholders) {
        player.sendActionBar(this.render(player, key, placeholders));
    }

    public String plain(Locale locale, String key, TagResolver... placeholders) {
        return PlainTextComponentSerializer.plainText().serialize(this.render(locale, key, placeholders));
    }

    public Locale resolveLocale(CommandSender sender) {
        if (sender instanceof Player player) {
            return normalizeLocale(player.getLocale());
        }
        return DEFAULT_LOCALE;
    }

    public TagResolver tag(String key, String value) {
        return Placeholder.unparsed(key, value);
    }

    public TagResolver number(Locale locale, String key, Number value) {
        return Placeholder.unparsed(key, NumberFormat.getIntegerInstance(locale).format(value));
    }

    private ResourceBundle bundle(Locale locale) {
        return this.bundles.computeIfAbsent(locale, resolved -> ResourceBundle.getBundle("messages", resolved));
    }

    private static Locale normalizeLocale(String rawLocale) {
        if (rawLocale == null || rawLocale.isBlank()) {
            return DEFAULT_LOCALE;
        }
        String normalized = rawLocale.replace('_', '-');
        Locale locale = Locale.forLanguageTag(normalized);
        return locale.getLanguage().isBlank() ? DEFAULT_LOCALE : locale;
    }
}
