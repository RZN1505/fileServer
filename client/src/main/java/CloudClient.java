import com.sun.scenario.effect.impl.sw.java.JSWBlend_SRC_OUTPeer;
import common.FileServerHelper;
import messages.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class CloudClient {

    private FileServerHelper fileServerHelper;

    private List<String> filesList;
    private String rootDir;
    String address;
    int port;

    /*
     * Класс клиента: read & send msg
     * */

    /*
     * Создаем Socket к серверу, открываем потоки ввода/вывода
     * Объект класса FileServerHelper, который предназначе для работы с файловой системой.
     * */
    public CloudClient(String address, int port) {
        this.address = address;
        this.port = port;
        rootDir = "ClientStorage" + File.separator;
        fileServerHelper = new FileServerHelper(rootDir);
    }

    public void connect() {
        NetworkClient.getInstance().connect(address, port);
    }


    //Чтение входящих сообщений и их обработка будут в отдельном потоке
    public void startReadingThread(ControllerFX controllerFX) {
        Thread thread = new Thread(() -> {

            while (NetworkClient.getInstance().isConnected()) {
                //считываем в объект поступающие сообщения
                Object msg = NetworkClient.getInstance().readObject();

                if (msg != null) {
                    System.out.println("Received msg" + msg.toString());

                    if (msg instanceof AbstractMsg) {
                        AbstractMsg incomingMsg = (AbstractMsg) msg;
                        //recieved msg типа Command - handle его
                        if (incomingMsg instanceof CommandMsg) {

                            CommandMsg cmdMsg = (CommandMsg) incomingMsg;

                            if (cmdMsg.getCommand() == CommandMsg.LOGIN) {
                                System.out.println("");
                                controllerFX.loginOk();
                            } else if (cmdMsg.getCommand() == CommandMsg.CREATE_DIR) {
                                System.out.println("CREATE_DIR");
                                createDirectory(cmdMsg);
                            } else if (cmdMsg.getCommand() == CommandMsg.CREATE_DIR_LOCAL) {
                                System.out.println("CREATE_DIR_LOCAL");
                                createDirectoryLocal(cmdMsg);
                            }
                        }

                        //вытаскиваем из recieved msg список файлов
                        if (incomingMsg instanceof FileListMsg) {
                            filesList = ((FileListMsg) incomingMsg).getFileList();
                            controllerFX.setCloudFilesList(filesList);
                        }

                        //сохраняем файл в локал. хранилище
                        if (incomingMsg instanceof TransferMsg) {
                            saveFileToLocalStorage((TransferMsg) incomingMsg);
                        }
                    }
                }
            }

        });

        //делаем демоном для завершения работы сразу после закрытия приложения
        thread.setDaemon(true);
        thread.start();
    }

    //Преобразование путей и создание каталога
    private void createDirectory(CommandMsg cmdMsg) {

        Path rootPath = Paths.get(rootDir);
        Object inObj1 = cmdMsg.getObject()[0];

        if (inObj1 instanceof String) {
            Path tempPath1 = Paths.get((String) inObj1);
            Path newPath = Paths.get(rootPath.toString(), File.separator,
                    tempPath1.subpath(1, tempPath1.getNameCount()).toString());

            fileServerHelper.mkDir(newPath);
        }

    }

    //Преобразование путей и создание каталога
    private void createDirectoryLocal(CommandMsg cmdMsg) {

        Path rootPath = Paths.get(rootDir);
        Object inObj1 = cmdMsg.getObject()[0];

        if (inObj1 instanceof String) {
            Path tempPath1 = Paths.get((String) inObj1);
            Path newPath = Paths.get(rootPath.toString(), File.separator,
                    tempPath1.subpath(1, tempPath1.getNameCount()).toString());

            fileServerHelper.mkDir(newPath);
        }

    }

    //При получении соответствующего сообщения сохраняем его в локальную папку
    //Если файл существует, то он будет перезаписан
    private void saveFileToLocalStorage(TransferMsg fileMsg) {
        System.out.println("saveFileToLocalStorage" + fileMsg.toString());
        Path newFilePath = Paths.get(rootDir +
                fileMsg.getFileName());
        if (Files.isDirectory(newFilePath)) {
            System.out.println("WTFsaveFileToLocalStorage");
        }else {
            fileServerHelper.mkFile(newFilePath, fileMsg.getData());
       }

    }

    //getter для списка файлов
    public List<String> getCloudFilesList() {
        return filesList;
    }

    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String newRootDir) {
        rootDir = newRootDir;
    }

    /*
     * Список файлов и каталогов из локального хранилища
     * */
    public List<String> getLocalFilesList() {
        return fileServerHelper.listDir(Paths.get(rootDir));
    }

    /*
     *Отправка каталога или файла в облачное хранилище
    * */
    public void uploadFileOrFolder(String itemName) {
        Path path = Paths.get(getRootDir(), itemName);
        if (Files.isDirectory(path)) {
            //отправляем директорию со всем ее содержимым в облачное хранилище
            try {
                sendFolder(path);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Ошибка выгрузки директории" + itemName + "в облачное хранилище");
            }
        } else {
            try {
                uploadFile(path);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Ошибка выгрузки файла" + itemName + "в облачное хранилище");
            }
        }
    }

    //Отправка каталога и всех файлов в cloud хранилище
    private void sendFolder(Path folderPath) throws IOException {
        Files.walkFileTree(folderPath, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                System.out.println("Найдена директорию" + dir.toString());
                NetworkClient.getInstance()
                        .sendObject(new CommandMsg(
                                CommandMsg.CREATE_DIR,
                                dir.toString(), dir.getParent().toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println("Отправили файл " + file);
                uploadFile(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /*
     * Реализуем отправку файлов из локального хранилища в облачное
     * */
    private void uploadFile(Path filePath) throws IOException {
        System.out.println("Отправка файла - " + filePath.getFileName().toString());
        NetworkClient.getInstance()
                .sendObject(new TransferMsg(filePath));
    }

    /*
     * Реализуем отправку файла в облачное хранилище по имени
     * */
    public void sendFileToStorage(String fileName) {
        Path filePath = Paths.get(getRootDir(), fileName);
        try {
            NetworkClient.getInstance()
                        .sendObject(new TransferMsg(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Отправка каталога или файла в локальное хранилище
     * */
    public void downloadFileOrFolder(String itemName) {
        System.out.println("downloadFileOrFolder" + itemName);
        Path path = Paths.get(getRootDir(), itemName);
        if (Files.isDirectory(path)) {
            //отправляем директорию со всем ее содержимым в локальное хранилище
            try {
                downloadFolder(path);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Ошибка загрузки директории" + itemName + "в локальное хранилище");
            }
        } else {
            downloadFile(itemName);
        }
    }

    //Отправка каталога и всех файлов в local хранилище
    private void downloadFolder(Path folderPath) throws IOException {
        Files.walkFileTree(folderPath, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                System.out.println("Найдена директория локального хранилища" + dir.toString());
                NetworkClient.getInstance()
                        .sendObject(new CommandMsg(
                                CommandMsg.CREATE_DIR_LOCAL,
                                dir.toString(), dir.getParent().toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println("Отправили файл " + file);
                downloadFile(file.toString());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /*
     * Реализуем отправку на сервер запроса на закачку файла в локальное хранилище
     * */
    public void downloadFile(String fileName) {
        NetworkClient.getInstance()
                .sendObject(new CommandMsg(CommandMsg.DOWNLOAD_FILE, fileName));
    }

    /*
     * Реализуем отправку команды на удаление файла в облачном хранилище по имени
     * */
    public void deleteCloudFilesObj(String fileName) {
        NetworkClient.getInstance()
                .sendObject(new CommandMsg(CommandMsg.DELETE, fileName));
    }

    /*
     * Реализуем удаление файла из локального хранилища по имени
     * */
    public void deleteLocalFile(String fileName) {
        Path newFilePath = Paths.get(getRootDir(), fileName);
        System.out.println("deleteLocalFile" + fileName);
        fileServerHelper.deleteFileFromStorage(newFilePath);
    }

    /*
     * Реализуем отправку аутентификационного сообщения с заданными логином - паролем
     * */
    public void doLogin(String login, String pwd) {
        NetworkClient.getInstance()
                .sendObject(new LoginMsg(login, pwd));
    }

    /*
     * Реализуем отправку команды на получение списка файлов в облачном хранилище
     * */
    public void listCloudFiles(String itemName) {
        CommandMsg cmd;

        if (itemName != null) {
            if (itemName.equals(".."))
                cmd = new CommandMsg(CommandMsg.LIST_FILES, "..");
            else
                cmd = (new CommandMsg(CommandMsg.LIST_FILES, itemName));
        } else
            cmd = (new CommandMsg(CommandMsg.LIST_FILES, ""));

        NetworkClient.getInstance().sendObject(cmd);
    }
}
