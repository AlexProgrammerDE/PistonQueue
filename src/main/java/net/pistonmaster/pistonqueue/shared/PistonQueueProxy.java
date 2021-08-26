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

import net.pistonmaster.pistonqueue.velocity.PistonQueueVelocity;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public interface PistonQueueProxy {
    default String replacePosition(String text, int position, QueueType type) {
        if (type.getDurationToPosition().containsKey(position)) {
            Duration duration = type.getDurationToPosition().get(position);

            return SharedChatUtils.formatDuration(text, duration, position);
        } else {
            AtomicInteger biggestPositionAtomic = new AtomicInteger();
            AtomicReference<Duration> bestDurationAtomic = new AtomicReference<>(Duration.ZERO);

            type.getDurationToPosition().forEach((integer, instant) -> {
                if (integer > biggestPositionAtomic.get()) {
                    biggestPositionAtomic.set(integer);
                    bestDurationAtomic.set(instant);
                }
            });

            int biggestPosition = biggestPositionAtomic.get();
            Duration biggestDuration = bestDurationAtomic.get();

            int difference = position - biggestPosition;

            Duration imaginaryDuration = biggestDuration.plus(difference, ChronoUnit.MINUTES);

            return SharedChatUtils.formatDuration(text, imaginaryDuration, position);
        }
    }

    default void processConfig(File dataDirectory) {
        try {
            if (!dataDirectory.exists() && !dataDirectory.mkdir())
                return;

            File file = new File(dataDirectory, "config.yml");

            if (!file.exists()) {
                try {
                    Files.copy(Objects.requireNonNull(PistonQueueVelocity.class.getClassLoader().getResourceAsStream("proxyconfig.yml")), file.toPath());
                    loadConfig(dataDirectory);
                    return;
                } catch (IOException ie) {
                    ie.printStackTrace();
                }
            }

            loadConfig(dataDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    default void loadConfig(File dataDirectory) throws IOException {
        ConfigurationNode config = YamlConfigurationLoader.builder().path(new File(dataDirectory, "config.yml").toPath()).build().load();

        Arrays.asList(Config.class.getDeclaredFields()).forEach(it -> {
            try {
                it.setAccessible(true);

                if (List.class.isAssignableFrom(it.getType())) {
                    it.set(Config.class, config.node(it.getName()).getList(String.class));
                } else {
                    it.set(Config.class, config.node(it.getName()).get(it.getType()));
                }
            } catch (SecurityException | IllegalAccessException | SerializationException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                String[] text = e.getMessage().split(" ");
                String value = "";

                for (String str : text) {
                    if (str.toLowerCase().startsWith(PistonQueueVelocity.class.getPackage().getName().toLowerCase())) {
                        value = str;
                    }
                }

                String[] packageSplit = value.split("\\.");

                new ConfigOutdatedException(packageSplit[packageSplit.length - 1]).printStackTrace();
            }
        });
    }
}
