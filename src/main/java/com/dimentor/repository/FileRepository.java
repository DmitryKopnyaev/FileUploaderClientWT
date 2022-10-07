package com.dimentor.repository;

import com.dimentor.util.Constants;
import com.dimentor.util.HttpMultipart;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    //http://localhost:8080/file
    public void addFile(String filename, String fileUriOnClient, String serverUri) {
        try {
            Map<String, String> headers = new HashMap<>();
            HttpMultipart multipart = new HttpMultipart("http://localhost:8080/file", "utf-8", headers);
            multipart.addFilePart("fileFromClient", new java.io.File(fileUriOnClient));
            multipart.addFormField("filename", filename);
            multipart.addFormField("uri", serverUri);
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
