package ca.xera.queue.bungee;

@SuppressWarnings({"unused"})
public final class QueueAPI {
    private QueueAPI() {
    }

    public static int getVeteranSize() {
        return XeraBungeeQueue.veteranQueue.size();
    }

    public static int getPrioritySize() {
        return XeraBungeeQueue.priorityQueue.size();
    }

    public static int getRegularSize() {
        return XeraBungeeQueue.regularQueue.size();
    }
}
