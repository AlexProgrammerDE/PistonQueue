package net.pistonmaster.pistonqueue.bungee;

@SuppressWarnings({"unused"})
public final class QueueAPI {
    private QueueAPI() {
    }

    public static int getVeteranSize() {
        return PistonQueue.veteranQueue.size();
    }

    public static int getPrioritySize() {
        return PistonQueue.priorityQueue.size();
    }

    public static int getRegularSize() {
        return PistonQueue.regularQueue.size();
    }
}
