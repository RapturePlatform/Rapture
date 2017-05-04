package rapture.common;

public interface QueueEventHandler {
    // return true on completion of task
    public boolean handleEvent(byte[] data);

    public boolean handleTask(TaskStatus task);
}
