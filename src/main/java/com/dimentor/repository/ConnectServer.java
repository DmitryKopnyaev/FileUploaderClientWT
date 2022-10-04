package com.dimentor.repository;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.rmi.ServerException;

public interface ConnectServer {
    default BufferedReader connect(String urlS, String method, String data) throws IOException {
        URL url = new URL(urlS);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");

        if (data != null) {
            connection.setDoOutput(true);
            try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(connection.getOutputStream())) {
                byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
                bufferedOutputStream.write(bytes, 0, bytes.length);
            }
        }

        if (connection.getResponseCode() != 200) {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                throw new ServerException(bufferedReader.readLine());
            }
        } else
            return new BufferedReader(new InputStreamReader(connection.getInputStream()));
    }
}
