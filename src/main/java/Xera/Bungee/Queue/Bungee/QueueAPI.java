package Xera.Bungee.Queue.Bungee;

@SuppressWarnings({"unused"})
public final class QueueAPI {
    public static int getRegularSize() {
        return XeraBungeeQueue.regularQueue.size();
    }

    public static int getPrioritySize() {
        return XeraBungeeQueue.priorityQueue.size();
    }

    public static int getVeteranSize() {
        return XeraBungeeQueue.veteranQueue.size();
    }
}
