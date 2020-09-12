
package messages;
public class CommandMsg extends AbstractMsg {
    private int command;
    public static final int LIST_FILES = 12345678;
    public static final int DOWNLOAD_FILE = 23456789;
    public static final int DELETE = 34567890;
    public static final int LOGIN = 456789012;
    public static final int CREATE_DIR = 56789012;
    public static final int CREATE_DIR_LOCAL = 67890123;
    //public static final int DELETE_DIR = 678901234;

    private Object[] object;

    public CommandMsg(int command, Object ... objects) {
        this.command = command;
        this.object = objects;
    }

    public int getCommand() {
        return command;
    }

    public Object[] getObject() {
        return object;
    }
}
