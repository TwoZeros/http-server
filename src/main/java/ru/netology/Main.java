package ru.netology;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    public static final int PORT = 8080;
    public static final int THREADS = 64;

    public static void main(String[] args) {

      Server server = new Server(THREADS);
      // добавление handler'ов (обработчиков)
      server.addHandler("GET", "/classic.html", new Handler() {
        public void handle(Request request, BufferedOutputStream out) {
          try {
            var filePath = Path.of(".", "public", request.getPath());
            var mimeType = Files.probeContentType(filePath);
            final var template = Files.readString(filePath);
            final var content = template.replace("{time}",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                    .getBytes();
            Server.outWrite(mimeType, content, out);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });
      server.addHandler("POST", "/events.html", new Handler() {
        public void handle(Request request, BufferedOutputStream out) {
          try {
            final var filePath = Path.of(".", "public", request.getPath());
            final var mimeType = Files.probeContentType(filePath);
            final var content = Files.readAllBytes(filePath);
            Server.outWrite(mimeType, content, out);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });

      server.addHandler("GET", "/index.html", new Handler() {
        public void handle(Request request, BufferedOutputStream out) {
          try {
            final var filePath = Path.of(".", "public", request.getPath());
            final var mimeType = Files.probeContentType(filePath);
            var template = Files.readString(filePath);
            template = template.replace("{name}", request.getQueryParam("name")
                    .orElse("World"));
            template = template.replace("{params}",request.getQueryParams().toString());
            final var content = template.getBytes();
            Server.outWrite(mimeType, content, out);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });

      server.addHandler("GET", "/links.html", new Handler() {
        public void handle(Request request, BufferedOutputStream out) {
          try {
            final var filePath = Path.of(".", "public", request.getPath());
            final var mimeType = Files.probeContentType(filePath);

            final var content = Files.readAllBytes(filePath);
            Server.outWrite(mimeType, content, out);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });

      server.listen(PORT);

    }



}




