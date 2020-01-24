package Leees.Bungee.Queue.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import Leees.Bungee.Queue.QueuePlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * Events
 */
public class Events implements Listener {

    List<UUID> list = new ArrayList<>();
    ServerInfo limbo = ProxyServer.getInstance().getServerInfo("limbo") == null
                    ? ProxyServer.getInstance().getServers().entrySet().iterator().next().getValue()
            : ProxyServer.getInstance().getServerInfo("limbo");

    @EventHandler
    public void on(PostLoginEvent event) {
        if (ProxyServer.getInstance().getOnlineCount()<= Lang.GLOBAL_SLOTS)
            return;
        if (!event.getPlayer().hasPermission(Lang.QUEUE_OVERRIDE_PERMISSION)) {
            // Send the player to the limbo
            list.add(event.getPlayer().getUniqueId());
            return;
        }
        // Send the player to where they want.

    }

    @EventHandler
    public void onSend(ServerConnectEvent e) {
        ProxiedPlayer player = e.getPlayer();
        if (!list.contains(player.getUniqueId()))
            return;
        list.remove(player.getUniqueId());
        if (!player.hasPermission(Lang.QUEUE_OVERRIDE_PERMISSION)) {
            // Keep a copy of the player's original target.
            final String originalTarget = e.getTarget().getName();
            // Send the player to the limbo and send a message.
            e.setTarget(limbo);
            player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
                    Lang.SEND_TO_LIMBO.replace("<global_slots>", Lang.GLOBAL_SLOTS + ""))));
            // Store the data concerning the player's destination
            QueuePlugin.final_destination.put(player.getUniqueId(), originalTarget);
            return;
        }

    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent e) {
        list.remove(e.getPlayer().getUniqueId());
        if (e.getPlayer().getServer().getInfo().getName().equalsIgnoreCase(limbo.getName())) {
            e.getPlayer().setReconnectServer(ProxyServer.getInstance()
                    .getServerInfo(QueuePlugin.final_destination.get(e.getPlayer().getUniqueId())));
        }

        QueuePlugin.final_destination.remove(e.getPlayer().getUniqueId());
    }


    public static void moveQueue() {
        if (Lang.GLOBAL_SLOTS <= ProxyServer.getInstance().getOnlineCount() - QueuePlugin.final_destination.size())
            return;
        if (QueuePlugin.final_destination.isEmpty())
            return;
        Entry<UUID, String> entry = QueuePlugin.final_destination.entrySet().iterator().next();
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entry.getKey());
        player.connect(ProxyServer.getInstance().getServerInfo(entry.getValue()));
        player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
                Lang.LEFT_LIMBO_JOIN_SERVER.replace("<server>", entry.getValue()))));
        QueuePlugin.final_destination.remove(entry.getKey());

    }

}
