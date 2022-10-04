package com.dimentor.repository;

import com.dimentor.model.Node;
import com.dimentor.util.Constants;
import com.dimentor.util.HttpMultipart;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileRepository implements ConnectServer {

    public File addDirectoryByUri(String uri) {
        try {
            BufferedReader reader = this.connect(
                    Constants.SERVER_NAME + "/file/dir?uri=" + URLEncoder.encode(uri, StandardCharsets.UTF_8),
                    "POST", null);
            return new ObjectMapper().readValue(reader, File.class);
        } catch (Exception e) {
            return null;
        }
    }

    public Node addNodeDirByUri(String uri) {
        try {
            BufferedReader reader = this.connect(
                    Constants.SERVER_NAME + "/node/diruri?uri=" + URLEncoder.encode(uri, StandardCharsets.UTF_8),
                    "POST", null);
            return new ObjectMapper().readValue(reader, Node.class);
        } catch (Exception e) {
            return null;
        }
    }

    //http://localhost:8080/file
    public void addFile(String fileUriOnClient) {
        try {
            Map<String, String> headers = new HashMap<>();
            HttpMultipart multipart = new HttpMultipart("http://localhost:8080/file", "utf-8", headers);
            multipart.addFilePart("fileFromClient", new java.io.File(fileUriOnClient));
            String response = multipart.finish();
            System.out.println("response = " + response);
            //мож вернуть обьект из респонса
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    //+V+
    public List<String> getListFilesByUri(String uri) {
        try {
            BufferedReader reader = this.connect(
                    Constants.SERVER_NAME + "/file/list?uri=" + URLEncoder.encode(uri, StandardCharsets.UTF_8),
                    "GET", null);
            return new ObjectMapper().readValue(reader, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    //+V+
    public List<File> getFilesByUri(String uri) {
        try {
            BufferedReader reader = this.connect(
                    Constants.SERVER_NAME + "/file/files?uri=" + URLEncoder.encode(uri, StandardCharsets.UTF_8),
                    "GET", null);
            return new ObjectMapper().readValue(reader, new TypeReference<List<File>>() {
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    //getParentFilesByDirUri

    public List<Node> getNodesByParentId(long id) {
        try {
            BufferedReader reader = this.connect(
                    Constants.SERVER_NAME + "/node/pid?parentId=" + id,
                    "GET", null);
            return new ObjectMapper().readValue(reader, new TypeReference<List<Node>>() {
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
    }

    //добавление файла
    public void getFileById(long id, String filePath) {
        try {
            String fileName = filePath;
            String url = "http://localhost:8080/node/file?id=" + id;
            HttpMultipart.getMultiPart(url, fileName); //1 - откуда получаем, 2 - куда сохраняем
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Node> getNodesByParentIdAndName(long id, String name) {
        try {
            BufferedReader reader = this.connect(
                    Constants.SERVER_NAME + "/node/pidname?parentId=" + id + "&name=" + URLEncoder.encode(name, StandardCharsets.UTF_8),
                    "GET", null);
            return new ObjectMapper().readValue(reader, new TypeReference<List<Node>>() {
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return new ArrayList<>();
        }
    }

    //http://localhost:8080/file/hex?diruri=&hex=b23591bd5fa74db4ee83db3c655a9b97
    //ToDo
    public File getFileByUriAndChecksum(String diruri, String hex) {
        try {
            BufferedReader reader = this.connect(
                    Constants.SERVER_NAME + "/file/hex?diruri=" + diruri + "&hex=" + URLEncoder.encode(hex, StandardCharsets.UTF_8),
                    "GET", null);
            return new ObjectMapper().readValue(reader, new TypeReference<File>() {
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
}
