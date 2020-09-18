package ca.xera.xerabungeequeue.bungee.queue.bungee;

@SuppressWarnings({"unused"})
public class API {
    public static int getRegularSize() {
        return XeraBungeeQueue.regularqueue.size();
    }

    public static int getPrioritySize() {
        return XeraBungeeQueue.priorityqueue.size();
    }
}
