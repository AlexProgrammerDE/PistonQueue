/*
 * #%L
 * PistonQueue
 * %%
 * Copyright (C) 2021 AlexProgrammerDE
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package net.pistonmaster.pistonqueue.bukkit;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;

public final class ProtocolLibWrapper {
    private ProtocolLibWrapper() {
    }

    public static void removeDebug(Player player) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_STATUS);

        packet.getIntegers().write(0, player.getEntityId());

        packet.getBytes().write(0, (byte) 22);

        manager.sendServerPacket(player, packet);
    }

    public static void setupProtocolLib(PistonQueueBukkit plugin) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        if (plugin.isNoChunkPackets()) {
            manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.MAP_CHUNK) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    event.setCancelled(true);
                }
            });
        }

        if (plugin.isNoTimePackets()) {
            manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.UPDATE_TIME) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    event.setCancelled(true);
                }
            });
        }

        if (plugin.isNoHealthPackets()) {
            manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.UPDATE_HEALTH) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    event.setCancelled(true);
                }
            });
        }

        if (plugin.isNoAdvancementPackets()) {
            manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ADVANCEMENTS) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    event.setCancelled(true);
                }
            });
        }

        if (plugin.isNoExperiencePackets()) {
            manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.EXPERIENCE) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    event.setCancelled(true);
                }
            });
        }

        if (plugin.isShowHeadPacket()) {
            manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_METADATA) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    event.setCancelled(true);
                }
            });
        }
    }
}
