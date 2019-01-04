package com.codeShare;

/**
 * @author Jeffrey Hensel
 * @date 1/4/2018
 */

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.io.InputStream;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.nio.charset.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WebCodeShare {
    private static final int LISTENER_PORT = 8887;

    private String editableData = "// Type code here.\n";
    private Lock dataLock = new ReentrantLock();
    private String spectatorResponse1 = "<HTML>\n<H1>Code Share</H1>\n<meta http-equiv=\"refresh\" content=\"1\">\n" +
            "<div contenteditable=\"true\" id=\"sharedSpace\" style=\"border:1px solid black; height: 80%;\">\n";
    private String editableResponse1 = "<HTML>\n<H1>Code Share</H1>\n" +
            "<div contenteditable=\"true\" id=\"sharedSpace\" style=\"border:1px solid black; height: 80%;\">\n";
    private String editableResponse2 =
            "</div>\n" +
                    "<iframe name=\"hiddenFrame\" width=\"0\" height=\"0\" border=\"0\" style=\"display: none;\"></iframe>\n" +
                    "<form method='POST' action=\"/edit\" enctype='text/plain' id=\"postForm\" target=\"hiddenFrame\">\n" +
                    "  <input id=\"editedInput\" name=\"1\" type=\"hidden\">\n" +
                    "</form>\n" +
                    "<button onclick=\"httpGet()\">Click me</button>" +
                    "<script>\n" +
                    "  document.getElementById(\"sharedSpace\").addEventListener(\"input\", function() {\n" +
                    "    document.getElementById(\"editedInput\").value = document.getElementById(\"sharedSpace\").innerHTML;\n" +
                    "    document.getElementById(\"postForm\").submit();\n" +
                    "  }, false);\n" +
                    "</script>\n" +
                    "<script>\n" +
                    "  setInterval(function httpGet()\n" +
                    "  {\n" +
                    "    var initSharedSpace = document.getElementById(\"sharedSpace\").innerHTML.replace(/\\s+/g, \" \");\n" +
                    "    var xmlHttp = new XMLHttpRequest();\n" +
                    "    xmlHttp.open( \"GET\", \"/textChange\", false ); // false for synchronous request\n" +
                    "    xmlHttp.send( null );\n" +
                    "    if (initSharedSpace != xmlHttp.responseText.replace(/\\s+/g, \" \") && initSharedSpace == document.getElementById(\"sharedSpace\").innerHTML.replace(/\\s+/g, \" \")) {\n" +
                    "      document.getElementById(\"sharedSpace\").innerHTML = xmlHttp.responseText;\n" +
                    "    }\n" +
                    "  }, 1000);\n" +
                    "</script>/n" +
                    "</HTML>\n";

    WebCodeShare() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(LISTENER_PORT), 0);
        server.createContext("/", new BaseHandler());
        server.createContext("/edit", new EditHandler());
        server.createContext("/textChange", new BaseSpectatorHandler());
        server.setExecutor(null);
        server.start();
    }

    // Endpoint matching "/"
    private class BaseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            dataLock.lock();
            String response = editableResponse1 + editableData + editableResponse2;
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // Endpoint matching "/spectator"
    private class BaseSpectatorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            dataLock.lock();
            String response = editableData;
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // Endpoint matching "/edit"
    private class EditHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            dataLock.lock();
            InputStream inputStream = t.getRequestBody();

            StringBuilder textBuilder = new StringBuilder();
            boolean hasFirstEquals = false; // parameter for data comes as "1 = ...." so trim (due to name field)

            try (Reader reader = new BufferedReader(new InputStreamReader
                    (inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
                int c = 0;
                while ((c = reader.read()) != -1) {
                    if (hasFirstEquals || (char)c == '=') {
                        if (hasFirstEquals) {
                            textBuilder.append((char) c);
                        }
                        hasFirstEquals = true;
                    }
                }
            }

            editableData = textBuilder.toString();
            System.out.println(editableData);

            String response = editableResponse1 + editableData + editableResponse2;

            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    public static void main(String[] args) throws Exception {
        /*MyWebHandler handler = */new WebCodeShare();
    }

}
