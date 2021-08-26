package net.pistonmaster.pistonqueue.shared;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
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
}
