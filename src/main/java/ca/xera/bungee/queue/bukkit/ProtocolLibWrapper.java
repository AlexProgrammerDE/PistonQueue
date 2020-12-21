package ca.xera.bungee.queue.bukkit;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public class ProtocolLibWrapper {
    public static void removeDebug(Player player) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_STATUS);

        packet.getIntegers().write(0, player.getEntityId());

        packet.getBytes().write(0, (byte) 22);

        try {
            manager.sendServerPacket(player, packet);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static void setupProtocolLib(XeraBungeeQueueBukkit plugin) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.MAP_CHUNK) {
            @Override
            public void onPacketSending(PacketEvent event) {
                event.setCancelled(true);
            }
        });
    }
}
