package net.pistonmaster.pistonqueue.bungee.utils;

public class ConfigOutdatedException extends Exception {
    public ConfigOutdatedException(String message) {
        super(message + " is missing in the config. Please remove the old config and restart the proxy to get the newest one.");
    }
}
