package messages;

import common.FileServerHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


public class TransferMsg extends AbstractMsg {
    private String fileName;
    private String path;
    private byte[] data;

    public TransferMsg(Path filePaths) throws IOException {
        this.path = filePaths.toString();
        this.fileName = filePaths.getFileName().toString();
        if (Files.isDirectory(filePaths)) {
            FileServerHelper fileServerHelper = new FileServerHelper();
            List<String> list = fileServerHelper.listDir(filePaths);
        } else {
            this.data = Files.readAllBytes(filePaths);
        }
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getData() {
        return data;
    }

    public String getPath() {
        return path;
    }

}
