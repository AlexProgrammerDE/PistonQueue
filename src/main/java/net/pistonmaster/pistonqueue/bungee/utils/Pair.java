package net.pistonmaster.pistonqueue.bungee.utils;

import lombok.Data;

@Data
public class Pair<L,R> {
    private final L left;
    private final R right;
}