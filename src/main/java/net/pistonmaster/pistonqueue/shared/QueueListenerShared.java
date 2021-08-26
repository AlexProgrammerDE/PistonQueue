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
package net.pistonmaster.pistonqueue.shared;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class QueueListenerShared {
    @Getter
    protected final List<UUID> noRecoveryMessage = new ArrayList<>();
    @Setter
    @Getter
    protected boolean mainOnline = false;
    @Setter
    protected boolean queueOnline = false;
    @Setter
    protected boolean authOnline = false;
    @Setter
    protected Instant onlineSince = null;

    protected void doRecovery(PlayerWrapper player) {
        QueueType type = QueueType.getQueueType(player::hasPermission);

        if (!type.getQueueMap().containsKey(player.getUniqueId()) && player.getCurrentServer().isPresent() && player.getCurrentServer().get().equals(Config.QUEUESERVER)) {
            type.getQueueMap().putIfAbsent(player.getUniqueId(), Config.MAINSERVER);

            if (!noRecoveryMessage.contains(player.getUniqueId())) {
                noRecoveryMessage.remove(player.getUniqueId());
                player.sendMessage(Config.RECOVERYMESSAGE);
            }
        }
    }

    public void putQueueAuthFirst(PlayerWrapper player) {
        QueueType type = QueueType.getQueueType(player::hasPermission);

        preQueueAdding(player, type.getHeader(), type.getFooter());

        // Store the data concerning the player's original destination
        type.getQueueMap().put(player.getUniqueId(), Config.MAINSERVER);
    }

    protected void preQueueAdding(PlayerWrapper player, List<String> header, List<String> footer) {
        player.sendPlayerListHeaderAndFooter(header, footer);

        player.sendMessage(Config.SERVERISFULLMESSAGE);
    }

    protected boolean isServerFull(PlayerWrapper player) {
        return (isPlayersQueueFull(player) || isAnyoneQueuedOfType(player)) || (!mainOnline && !Config.KICKWHENDOWN);
    }

    protected boolean isPlayersQueueFull(PlayerWrapper player) {
        return isQueueFull(QueueType.getQueueType(player::hasPermission));
    }

    protected boolean isQueueFull(QueueType type) {
        return type.getPlayersWithTypeInMain() >= type.getReservatedSlots();
    }

    protected boolean isAnyoneQueuedOfType(PlayerWrapper player) {
        return !QueueType.getQueueType(player::hasPermission).getQueueMap().isEmpty();
    }
}
