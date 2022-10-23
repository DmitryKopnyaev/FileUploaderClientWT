package com.dimentor;

import com.dimentor.repository.FileRepository;
import com.dimentor.util.VersionUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.MouseEvent;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;

public class MainController {
    public ListView<String> clientListView;
    public ListView<String> serverListView;
    public Button addServerDir;
    public Button addClientDir;
    public Button deleteServerDir;
    public Button deleteClientDir;

    private File clientRoot = new File("C:\\");
    private File currentClientFile;
    private File currentServerFile;
    private List<String> currentServerChildNodes;

    @FXML
    private void initialize() {
        this.serverListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE); //множественный выбор
        this.currentClientFile = clientRoot;
        this.currentServerFile = new File("");

        this.loadClientListView();
        this.loadServerListView();
    }

    public void loadClientListView() {
        String[] fileNames = this.currentClientFile.list();
        this.clientListView.setItems(FXCollections.observableList(Arrays.asList(fileNames)));
    }

    public void loadServerListView() {
        this.currentServerChildNodes = new FileRepository().getListFilesByUri(this.currentServerFile.toString());
        this.serverListView.setItems(FXCollections.observableList(this.currentServerChildNodes));
    }

    public void OnMouseClickedClient(MouseEvent mouseEvent) {
        int clickCount = mouseEvent.getClickCount();
        if (clickCount == 2) {
            int selectedIndex = clientListView.getSelectionModel().getSelectedIndex();
            currentClientFile = currentClientFile.listFiles()[selectedIndex];
            if (currentClientFile.isDirectory()) {
                String[] list = currentClientFile.list();
                this.clientListView.setItems(FXCollections.observableList(Arrays.asList(list)));
            }
        }
    }

    public void OnMouseClickedServer(MouseEvent mouseEvent) {
        int clickCount = mouseEvent.getClickCount();
        if (clickCount == 2) {
            int selectedIndex = serverListView.getSelectionModel().getSelectedIndex();
            String selectedFileName = this.currentServerChildNodes.get(selectedIndex);
            this.currentServerFile = new File(this.currentServerFile, selectedFileName);
            moveToFile(this.currentServerFile);
        }
    }

    public void buttonClientBack(ActionEvent actionEvent) {
        if (!currentClientFile.equals(clientRoot)) {
            currentClientFile = this.currentClientFile.getParentFile();
            String[] list = currentClientFile.list();
            this.clientListView.setItems(FXCollections.observableList(Arrays.asList(list)));
        }
    }

    public void buttonServerBack(ActionEvent actionEvent) {
        Path parent = this.currentServerFile.toPath().getParent();
        if (parent != null) {
            this.currentServerFile = parent.toFile();
            moveToFile(this.currentServerFile);
        }
    }

    public void moveToFile(File toFile) {
        System.out.println(toFile);
        List<String> listFilesByUri = new FileRepository().getListFilesByUri(toFile.toString());
        if (listFilesByUri != null) {
            this.currentServerChildNodes = listFilesByUri;
            this.serverListView.setItems(FXCollections.observableList(listFilesByUri));
        }
    }

    public void buttonSaveOnServer(ActionEvent actionEvent) {
        int selectedIndexClient = this.clientListView.getSelectionModel().getSelectedIndex();
        File[] files = this.currentClientFile.listFiles();
        if (files != null) {
            File file = files[selectedIndexClient];
            if (file == null)
                App.showAlert("Error", "Выберите файлы в окне сервера и клиента", Alert.AlertType.INFORMATION);
            else {
                this.extractOnServer(file, this.currentServerFile.toString());
                this.loadServerListView();
            }
        }
    }

    public void buttonSaveOnClient(ActionEvent actionEvent) {
        ObservableList<Integer> selectedIndicesServer = this.serverListView.getSelectionModel().getSelectedIndices(); //нумерация с нуля
        if (selectedIndicesServer.size() == 0)
            App.showAlert("Error", "Выберите файлы в окне сервера", Alert.AlertType.INFORMATION);

        for (Integer i : selectedIndicesServer) {
            Path path = Path.of(this.currentServerFile.toString(), this.currentServerChildNodes.get(i)); // серверный файл/папка для сохранения на клиенте с полным относит адресом \main\big\b2
            extractOnClient(path.toString(), this.currentClientFile);
        }
        this.loadClientListView();
    }

    public void extractOnClient(String servFile, File clFile) {
        Path parent = Path.of("/").relativize(Path.of(servFile).getParent());
        List<String> listFilesByUri = new FileRepository().getTreeFilesByUri(servFile);
        if (listFilesByUri != null) {
            for (String s : listFilesByUri) {
                Path pathS = Path.of(s);
                Path relativize = parent.relativize(pathS);
                String hexByUri = new FileRepository().getHexByUri(s); //null если директория

                if (hexByUri == null) {
                    new File(clFile, relativize.toString()).mkdirs();
                } else
                    new FileRepository().getUploadingFile(s, new File(clFile, relativize.toString()).toString());
            }
        } else {
            new FileRepository().getUploadingFile(servFile, clFile.toPath().resolve(Path.of(servFile).getFileName()).toString());
        }
        this.loadClientListView();
    }

    public void extractOnServer(File clFile, String servFile) {
//clFile - файл/директория на клиенте которая переносится целиком на сервер
//servFile - директория на клиенте в которую извлекается clFile
        try {
            Files.walkFileTree(clFile.toPath(), new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    Path parent = Paths.get(clFile.getParent());
                    Path relativizeSrcClient = parent.relativize(dir);

                    Path servPath = Paths.get(servFile, relativizeSrcClient.toString());
                    new FileRepository().addDirectoryByUri(servPath.toString());

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

                    Path serverFileSrc = Path.of(servFile);
                    if (file.getFileName().toString().endsWith(".zip")) {
                        Path dirForZipFilesOnClient = Path.of(file.toString().replace(".zip", ""));
                        Path dirForZipFileOnServer = serverFileSrc.resolve(clFile.toPath().getParent().relativize(dirForZipFilesOnClient));

                        ZipFile zipFile = new ZipFile(file.toString());
                        try {
                            zipFile.extractAll(dirForZipFilesOnClient.toString());
                        } catch (ZipException e) {
                            System.out.println(e.getMessage());
                        }

                        extractOnServer(dirForZipFilesOnClient.toFile(), dirForZipFileOnServer.getParent().toString());
                        try {
                            FileUtils.deleteDirectory(dirForZipFilesOnClient.toFile());
                        } catch (IOException e) {
                            System.out.println(e.getMessage());
                        }
                    } else {
                        Path dirOnServer = serverFileSrc.resolve(clFile.toPath().getParent().relativize(file.getParent())); // относительный \qwe\big\ar
                        try {
                            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file.toString()))) {
                                String md5Hex = DigestUtils.md5Hex(bis);
                                List<File> filesByServerUri = new FileRepository().getFilesByUri(dirOnServer.toString());
                                String clearClientFileName = VersionUtil.getClearFileName(file.getFileName().toString());
                                int version = 1;
                                for (File f : filesByServerUri) {
                                    String serverFileName = f.getName();
                                    if (VersionUtil.getClearFileName(serverFileName).equals(clearClientFileName)) {
                                        try (BufferedInputStream bis2 = new BufferedInputStream(new FileInputStream(f.toString()))) {
                                            String md5HexServerFile = DigestUtils.md5Hex(bis2);
                                            if (!md5Hex.equalsIgnoreCase(md5HexServerFile))
                                                version = VersionUtil.getVersion(serverFileName) + 1;
                                            else return FileVisitResult.CONTINUE;
                                        }
                                    }
                                }
                                String newFileNameWithVersion = VersionUtil.getFileNameWithVersion(clearClientFileName, version);
                                new FileRepository().addFile(newFileNameWithVersion, file.toString(), dirOnServer.toString());
                            }
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void addServerDir(ActionEvent actionEvent) {
        String s = App.inputText();
        if (s != null && !s.isEmpty()) {
            new FileRepository().addDirectoryByUri(new File(this.currentServerFile, s).toString());
            this.loadServerListView();
        }

    }

    public void addClientDir(ActionEvent actionEvent) {
        String s = App.inputText();
        if (s != null && !s.isEmpty()) {
            new File(this.currentClientFile, s).mkdirs();
            this.loadClientListView();
        }
    }

    public void deleteServerDir(ActionEvent actionEvent) {
        String selectedItem = this.serverListView.getSelectionModel().getSelectedItem();
        File file = new File(this.currentServerFile, selectedItem);
        new FileRepository().deleteDirectoryByUri(file.toString());
        this.loadServerListView();
    }

    public void deleteClientDir(ActionEvent actionEvent) {
        String selectedItem = this.clientListView.getSelectionModel().getSelectedItem();
        File file = new File(this.currentClientFile, selectedItem);
        try {
            FileUtils.deleteDirectory(file);
            this.loadClientListView();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}