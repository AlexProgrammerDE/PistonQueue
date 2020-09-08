package Xera.Bungee.Queue.Bungee;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * BungeeEvents
 */
public class BungeeEvents implements Listener {

    List<UUID> regular = new ArrayList<>();
    List<UUID> priority = new ArrayList<>();
    ServerInfo queue = ProxyServer.getInstance().getServerInfo(Config.QUEUESERVER);
    public static boolean mainonline = false;
    public static boolean queueonline = false;
    public static boolean authonline = false;

    @EventHandler
    public void onPreLogin(PreLoginEvent ple) {
        if (!ple.getConnection().getName().matches(Config.REGEX)) {
            ple.setCancelReason(new TextComponent(ChatColor.GOLD + "[XBG] Invalid username please use: " + Config.REGEX));
            ple.setCancelled(true);
        }
    }

    public static void CheckIfMainServerIsOnline() {
        ProxyServer.getInstance().getServerInfo(Config.MAINSERVER).ping((result, error) -> {
            mainonline = error == null;
        });
    }

    public static void CheckIfQueueServerIsOnline() {
        ProxyServer.getInstance().getServerInfo(Config.QUEUESERVER).ping((result, error) -> {
            queueonline = error == null;
        });
    }

    public static void CheckIfAuthServerIsOnline() {
        if (Config.ENABLEAUTHSERVER) {
            ProxyServer.getInstance().getServerInfo(Config.AUTHSERVER).ping((result, error) -> {
                authonline = error == null;
            });
        } else {
            authonline = true;
        }
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        if (Config.ENABLEAUTHSERVER) {
            if (mainonline && queueonline && authonline) {
                if (!Config.ALWAYSQUEUE && ProxyServer.getInstance().getOnlineCount() <= Config.MAINSERVERSLOTS) {
                    return;
                }

                if (event.getPlayer().hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
                    // Send the priority player to the priority queue
                    priority.add(event.getPlayer().getUniqueId());
                } else if (!event.getPlayer().hasPermission(Config.QUEUEBYPASSPERMISSION) && !event.getPlayer().hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
                    // Send the player to the regular queue
                    regular.add(event.getPlayer().getUniqueId());
                }
            } else {
                event.getPlayer().disconnect(new ComponentBuilder(Config.SERVERDOWNKICKMESSAGE.replace("&", "§")).create());
            }
        } else {
            if (mainonline && queueonline) {
                if (!Config.ALWAYSQUEUE && ProxyServer.getInstance().getOnlineCount() <= Config.MAINSERVERSLOTS) {
                    return;
                }

                if (event.getPlayer().hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
                    // Send the priority player to the priority queue
                    priority.add(event.getPlayer().getUniqueId());
                } else if (!event.getPlayer().hasPermission(Config.QUEUEBYPASSPERMISSION) && !event.getPlayer().hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
                    // Send the player to the regular queue
                    regular.add(event.getPlayer().getUniqueId());
                }
            } else {
                event.getPlayer().disconnect(new ComponentBuilder(Config.SERVERDOWNKICKMESSAGE.replace("&", "§")).create());
            }
        }
    }

    @EventHandler
    public void onSend(ServerConnectEvent e) {
        ProxiedPlayer player = e.getPlayer();

        if (player.hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
            if (!priority.contains(player.getUniqueId())) {
                return;
            }

            priority.remove(player.getUniqueId());

            // Send the player to the queue and send a message.
            String originalTarget = e.getTarget().getName();

            e.setTarget(queue);

            StringBuilder headerprio = new StringBuilder();

            for (int i = 0; i < Config.HEADERPRIORITY.size(); i++) {
                if (i == (Config.HEADERPRIORITY.size() - 1)) {
                    headerprio.append(Config.HEADERPRIORITY.get(i).replace("&", "§").replace("%position%", "None").replace("%wait%", "None"));
                } else {
                    headerprio.append(Config.HEADERPRIORITY.get(i).replace("&", "§").replace("%position%", "None").replace("%wait%", "None")).append("\n");
                }
            }

            StringBuilder footerprio = new StringBuilder();

            for (int i = 0; i < Config.FOOTERPRIORITY.size(); i++) {
                if (i == (Config.FOOTERPRIORITY.size() - 1)) {
                    footerprio.append(Config.FOOTERPRIORITY.get(i).replace("&", "§").replace("%position%", "None").replace("%wait%", "None"));
                } else {
                    footerprio.append(Config.FOOTERPRIORITY.get(i).replace("&", "§").replace("%position%", "None").replace("%wait%", "None")).append("\n");
                }
            }

            player.setTabHeader(
                    new ComponentBuilder(headerprio.toString()).create(),
                    new ComponentBuilder(footerprio.toString()).create());
            player.sendMessage(new ComponentBuilder(Config.SERVERISFULLMESSAGE.replace("&", "§")).color(ChatColor.GOLD).create());

            // Store the data concerning the player's destination
            XeraBungeeQueue.priorityqueue.put(player.getUniqueId(), originalTarget);
        } else if (!e.getPlayer().hasPermission(Config.QUEUEBYPASSPERMISSION) && !e.getPlayer().hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
            if (!regular.contains(player.getUniqueId())) {
                return;
            }

            regular.remove(player.getUniqueId());

            // Send the player to the queue and send a message.
            String originalTarget = e.getTarget().getName();

            e.setTarget(queue);

            StringBuilder header = new StringBuilder();

            for (int i = 0; i < Config.HEADER.size(); i++) {
                if (i == (Config.HEADER.size() - 1)) {
                    header.append(Config.HEADER.get(i).replace("&", "§").replace("%position%", "None").replace("%wait%", "None"));
                } else {
                    header.append(Config.HEADER.get(i).replace("&", "§").replace("%position%", "None").replace("%wait%", "None")).append("\n");
                }
            }

            StringBuilder footer = new StringBuilder();

            for (int i = 0; i < Config.FOOTER.size(); i++) {
                if (i == (Config.FOOTER.size() - 1)) {
                    footer.append(Config.FOOTER.get(i).replace("&", "§").replace("%position%", "None").replace("%wait%", "None"));
                } else {
                    footer.append(Config.FOOTER.get(i).replace("&", "§").replace("%position%", "None").replace("%wait%", "None")).append("\n");
                }
            }

            player.setTabHeader(
                    new ComponentBuilder(header.toString()).create(),
                    new ComponentBuilder(footer.toString()).create());
            player.sendMessage(new ComponentBuilder(Config.SERVERISFULLMESSAGE.replace("&", "§")).color(ChatColor.GOLD).create());

            // Store the data concerning the player's destination
            XeraBungeeQueue.regularqueue.put(player.getUniqueId(), originalTarget);
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent e) {
        // if a play in queue logs off it removes them and their position so when they relog
        // they get sent to the back of the line and have to wait through the queue again yeah
        try {
            if (e.getPlayer().getServer().getInfo().getName().equalsIgnoreCase(Config.QUEUESERVER)) {
                e.getPlayer().setReconnectServer(ProxyServer.getInstance().getServerInfo(XeraBungeeQueue.priorityqueue.get(e.getPlayer().getUniqueId())));
                e.getPlayer().setReconnectServer(ProxyServer.getInstance().getServerInfo(XeraBungeeQueue.regularqueue.get(e.getPlayer().getUniqueId())));
            }

            XeraBungeeQueue.priorityqueue.remove(e.getPlayer().getUniqueId());
            XeraBungeeQueue.regularqueue.remove(e.getPlayer().getUniqueId());

            priority.remove(e.getPlayer().getUniqueId());
            regular.remove(e.getPlayer().getUniqueId());
        } catch(NullPointerException ignored) {

        }
    }

    public static void moveQueue() {
        // checks if priority queue is empty if it is a non priority user always gets in
        // if it has people in it then it gives a chance for either a priority or non
        // priority user to get in when someone logs off the main server
        // gets a random number then if the number is less then or equal to the odds set in
        // this bungeeconfig.yml it will add a priority player if its anything above the odds then
        // a non priority player gets added to the main server
        // Random rn = new Random();
        // for (int i = 0; i < 100; i++) {
        // int answer = rn.nextInt(10) + 1;
        if (!XeraBungeeQueue.priorityqueue.isEmpty()) {
            if (Config.MAINSERVERSLOTS <= ProxyServer.getInstance().getOnlineCount() - XeraBungeeQueue.regularqueue.size() - XeraBungeeQueue.priorityqueue.size()) {
                return;
            }

            if (XeraBungeeQueue.priorityqueue.isEmpty()) {
                return;
            }

            Entry<UUID, String> entry2 = XeraBungeeQueue.priorityqueue.entrySet().iterator().next();
            ProxiedPlayer player2 = ProxyServer.getInstance().getPlayer(entry2.getKey());

            player2.connect(ProxyServer.getInstance().getServerInfo(entry2.getValue()));
            player2.sendMessage(ChatMessageType.CHAT, TextComponent.fromLegacyText(Config.JOININGMAINSERVER.replace("&", "§").replace("<server>", entry2.getValue())));

            player2.resetTabHeader();

            XeraBungeeQueue.priorityqueue.remove(entry2.getKey());
        } else {
            if (Config.MAINSERVERSLOTS <= ProxyServer.getInstance().getOnlineCount() - XeraBungeeQueue.regularqueue.size() - XeraBungeeQueue.priorityqueue.size()) {
                return;
            }

            if (XeraBungeeQueue.regularqueue.isEmpty()) {
                return;
            }

            Entry<UUID, String> entry = XeraBungeeQueue.regularqueue.entrySet().iterator().next();
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entry.getKey());

            player.connect(ProxyServer.getInstance().getServerInfo(entry.getValue()));
            player.sendMessage(ChatMessageType.CHAT, TextComponent.fromLegacyText(Config.JOININGMAINSERVER.replace("&", "§").replace("<server>", entry.getValue())));

            player.resetTabHeader();

            XeraBungeeQueue.regularqueue.remove(entry.getKey());
        }
    }

    @EventHandler
    public void onKick(ServerKickEvent event) {
        if (Config.ENABLEKICKMESSAGE) {
            event.setKickReasonComponent(new ComponentBuilder(Config.KICKMESSAGE.replace("&", "§")).create());
        }
    }
}
