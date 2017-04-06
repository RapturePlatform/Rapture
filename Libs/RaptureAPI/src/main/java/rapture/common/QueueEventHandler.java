package rapture.common;

public interface QueueEventHandler {
    // return true on completion of task
    public boolean handleEvent(String queueIdentifier, byte[] data);

    public boolean handleTask(String queueIdentifier, TaskStatus task);
}
