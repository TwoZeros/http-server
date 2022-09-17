package ru.netology;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {

    private final ExecutorService executorService;
    private final static Map<String, Map<String, Handler>> handlers = new ConcurrentHashMap<>();

    private final Handler notFoundHandler = new Handler() {
        public void handle(Request request, BufferedOutputStream out) {
            try {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    public Server(int thread) {
        this.executorService = Executors.newFixedThreadPool(thread);
    }

    public void listen(int port) {

        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                final var socket = serverSocket.accept();
                executorService.submit(() -> connect(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void addHandler(String method, String path, Handler handler) {
        if (handlers.get(method) == null) {
            handlers.put(method, new ConcurrentHashMap<>());
        }
        handlers.get(method).put(path, handler);

    }
        public void connect(Socket socket) {
            final BufferedOutputStream out;

            try {
                out = new BufferedOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            try (socket;
                 final var in = socket.getInputStream();
                 ) {
                var request = Request.createRequest(in);
                var pathHandlerMap = handlers.get(request.getMethod());
                if (pathHandlerMap == null) {
                    notFoundHandler.handle(request, out);
                    return;
                }
                var handler = pathHandlerMap.get(request.getPath());
                if (handler == null) {
                    notFoundHandler.handle(request, out);
                    return;
                }
                handler.handle(request, out);

            }
            catch (IOException e) {
                e.printStackTrace();
            } catch (BadRequestException e) {
                try {
                    badRequest(out);
                } catch (IOException ex) {
                    System.out.println(e.getMessage());
                }
            }

            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    public static void outWrite(String mimeType, byte[] content, BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + content.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.write(content);
        out.flush();
    }
    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }
    }



