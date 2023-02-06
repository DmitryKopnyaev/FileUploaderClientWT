package com.dimentor.repository;

import com.dimentor.util.Constants;
import com.dimentor.util.HttpMultipart;
import com.dimentor.util.MetaFile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class FileRepository implements ConnectServer {

    public void addDirectoryByUri(String uri) {
        try {
            BufferedReader reader = this.connect(
                    Constants.SERVER_NAME + "/file/dir?uri=" + URLEncoder.encode(uri, StandardCharsets.UTF_8),
                    "POST", null);
            new ObjectMapper().readValue(reader, File.class);
        } catch (Exception e) {
        }
    }

    //add file to server
    public void addFile(String filename, String fileUriOnClient, String serverUri) {
        try {
            HttpMultipart multipart = new HttpMultipart(Constants.SERVER_NAME + "/file/load", "utf-8", null);
            multipart.addFilePart("fileFromClient", new java.io.File(fileUriOnClient));
            multipart.addFormField("filename", filename);
            multipart.addFormField("uri", serverUri);
            String response = multipart.finish();
            System.out.println("response = " + response);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    public void getUploadingFile(String fileSrc, String fileTo) {
        try {
            String urlStr = Constants.SERVER_NAME + "/file/upload?uri=" + URLEncoder.encode(fileSrc, StandardCharsets.UTF_8);
            HttpMultipart.getMultiPart(urlStr, fileTo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getListFilesByUri(String uri) {
        try (BufferedReader reader = this.connect(
                Constants.SERVER_NAME + "/file/list?uri=" + URLEncoder.encode(uri, StandardCharsets.UTF_8),
                "GET", null);){
            return new ObjectMapper().readValue(reader, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public List<String> getTreeFilesByUri(String uri) {
        try (BufferedReader reader = this.connect(
                Constants.SERVER_NAME + "/file/tree?uri=" + URLEncoder.encode(uri, StandardCharsets.UTF_8),
                "GET", null);){
            return new ObjectMapper().readValue(reader, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public void deleteByUri(String uri) {
        try (BufferedReader reader = this.connect(
                Constants.SERVER_NAME + "/file?uri=" + URLEncoder.encode(uri, StandardCharsets.UTF_8),
                "DELETE", null);) {
            new ObjectMapper().readValue(reader, new TypeReference<File>() {
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public String getHexByUri(String uri) {
        System.out.println("URI = " + uri);
        try (BufferedReader reader = this.connect(
                Constants.SERVER_NAME + "/file/hex?uri=" + URLEncoder.encode(uri, StandardCharsets.UTF_8),
                "GET", null);){
            return reader.readLine();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public Map<String, List<MetaFile>> getStructureByUri(String uri) {
        try (BufferedReader reader = this.connect(
                Constants.SERVER_NAME + "/file/structure?uri=" + URLEncoder.encode(uri, StandardCharsets.UTF_8),
                "GET", null);){
            return new ObjectMapper().readValue(reader, new TypeReference<Map<String, List<MetaFile>>>() {
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
}
