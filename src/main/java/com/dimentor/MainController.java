package com.dimentor;

import com.dimentor.repository.FileRepository;
import com.dimentor.util.FileUtil;
import com.dimentor.util.MetaFile;
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
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MainController {
    public ListView<String> clientListView;
    public ListView<String> serverListView;
    public Button addServerDir;
    public Button addClientDir;
    public Button deleteServerDir;
    public Button deleteClientDir;

    private final File clientRoot = new File(System.getenv("SystemDrive") + "\\");
    private File currentClientFile = clientRoot;
    private File currentServerFile = new File("");
    private List<String> currentServerChildNodes;

    @FXML
    private void initialize() {
        this.serverListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE); //множественный выбор
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
        if (mouseEvent.getClickCount() == 2) {
            int selectedIndex = clientListView.getSelectionModel().getSelectedIndex();
            this.currentClientFile = this.currentClientFile.listFiles()[selectedIndex];
            if (this.currentClientFile!= null && this.currentClientFile.isDirectory()) {
                String[] list = currentClientFile.list();
                this.clientListView.setItems(FXCollections.observableList(Arrays.asList(list)));
            }
        }
    }

    public void OnMouseClickedServer(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            int selectedIndex = serverListView.getSelectionModel().getSelectedIndex();
            String selectedFileName = this.currentServerChildNodes.get(selectedIndex);
            this.currentServerFile = new File(this.currentServerFile, selectedFileName);
            if (new FileRepository().getHexByUri(String.valueOf(this.currentServerFile)) == null)
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
        List<String> listFilesByUri = new FileRepository().getListFilesByUri(toFile.toString());
        if (listFilesByUri != null) {
            this.currentServerChildNodes = listFilesByUri;
            this.serverListView.setItems(FXCollections.observableList(listFilesByUri));
        }
    }

    public void buttonSaveOnServer(ActionEvent actionEvent) throws IOException {
        int selectedIndexClient = this.clientListView.getSelectionModel().getSelectedIndex();
        File[] files = this.currentClientFile.listFiles();
        if (files != null) {
            File file = files[selectedIndexClient]; // -> C:\JavaProjects\FileUploader\dir
            if (file == null)
                App.showAlert("Error", "Выберите файлы в окне сервера и клиента", Alert.AlertType.INFORMATION);
            else {
                if (file.isFile()) {
                    Map<String, List<MetaFile>> map = new FileRepository().getStructureByUri(this.currentServerFile.toString());
                    List<MetaFile> metaFiles = map.get("");
                    String filenameWithVersion = FileUtil.getFilenameWithVersion(file, metaFiles);
                    if (filenameWithVersion != null)
                        new FileRepository().addFile(filenameWithVersion, String.valueOf(file), this.currentServerFile.toString());
                } else {
                    if (new File("zip.zip").exists()) new File("zip.zip").delete();
                    try (ZipFile zip = FileUtil.createRightZip(file.toString(), this.currentServerFile.toString())) {
                        new FileRepository().addFile("zip.zip", zip.getFile().toString(), this.currentServerFile.toString());
                        zip.getFile().delete();
                    } catch (IOException e) {
                        System.out.println(e.getMessage());                    }
                }
                this.loadServerListView();
            }
        }
    }

    public void buttonSaveOnClient(ActionEvent actionEvent) {
        ObservableList<Integer> selectedIndicesServer = this.serverListView.getSelectionModel().getSelectedIndices(); //нумерация с нуля
        if (selectedIndicesServer.size() == 0)
            App.showAlert("Error", "Выберите файлы в окне сервера", Alert.AlertType.INFORMATION);

        for (Integer i : selectedIndicesServer) {
            Path path = Path.of(this.currentServerFile.toString(), this.currentServerChildNodes.get(i));
            extractOnClient(path.toString(), this.currentClientFile);
        }
        this.loadClientListView();
    }

    // servFile - серверный файл/папка для сохранения на клиенте с полным относит адресом \main\big\b2
    // clFile - файл/папка на клиенте
    public void extractOnClient(String servFile, File clFile) {
        Path root = Path.of("/");
        Path servFilePath = Path.of(servFile);
        Path servFileParentPath = servFilePath.getParent();
        Path parent = root.relativize(servFileParentPath);

        List<String> listFilesByUri = new FileRepository().getTreeFilesByUri(servFile);
        if (listFilesByUri != null) {
            for (String s : listFilesByUri) {
                Path pathS = Path.of(s);
                Path relativize = parent.relativize(pathS);
                String hexByUri = new FileRepository().getHexByUri(s);

                if (hexByUri == null) { //null если директория
                    new File(clFile, relativize.toString()).mkdirs();
                } else //если файл
                    new FileRepository().getUploadingFile(s, new File(clFile, relativize.toString()).toString());
            }
        } else {
            new FileRepository().getUploadingFile(servFile, clFile.toPath().resolve(servFilePath.getFileName()).toString());
        }
        this.loadClientListView();
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

    public void deleteServerFile(ActionEvent actionEvent) {
        String selectedItem = this.serverListView.getSelectionModel().getSelectedItem();
        File file = new File(this.currentServerFile, selectedItem);
        new FileRepository().deleteByUri(file.toString());
        this.loadServerListView();
    }

    public void deleteClientFile(ActionEvent actionEvent) {
        String selectedItem = this.clientListView.getSelectionModel().getSelectedItem();
        File file = new File(this.currentClientFile, selectedItem);
        try {
            if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
                this.loadClientListView();
            } else //isFile
                if (file.delete()) this.loadClientListView();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}