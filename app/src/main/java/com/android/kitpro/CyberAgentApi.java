package com.android.kitpro;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/** Minimal read-only client for the CyberAgent threat-feed backend. */
public final class CyberAgentApi {
    private static final String BASE_URL = "https://cyberagent-api.onrender.com";
    private CyberAgentApi() { }

    public static JSONObject fetchThreatFeed() throws Exception {
        HttpURLConnection connection = (HttpURLConnection)
                new URL(BASE_URL + "/api/v1/threat-feed").openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("Accept", "application/json");
        try {
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String body = read(stream);
            if (code < 200 || code >= 300)
                throw new IllegalStateException("CyberAgent API HTTP " + code);
            return new JSONObject(body);
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) result.append(line);
        reader.close();
        return result.toString();
    }
}
