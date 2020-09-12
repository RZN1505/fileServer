package messages;
public enum Commands {
    DOWNLOAD(23456789),
    LIST(12345678);

    private int command;

    Commands(int command) {
        this.command = command;
    }

    public int getCommand() {
        return command;
    }
}
