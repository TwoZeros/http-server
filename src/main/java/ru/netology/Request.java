package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Request {
    public static final String GET = "GET";
    public static final String POST = "POST";
    private final InputStream in;
    private final String method;
    private List<NameValuePair> queryParams;
    private String body;
    private final String path;
    private final List<String> headers;
    final static List<String> allowedMethods = List.of(GET, POST);

    public Request(String method, String path, List<String> headers,List<NameValuePair> queryParams, InputStream in) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.queryParams = queryParams;
        this.in = in;
    }
    public Request(String method, String path, List<String> headers,InputStream in) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.in = in;
    }

    public static Request createRequest(InputStream inputStream ) throws IOException, BadRequestException {
        // лимит на request line + заголовки
        final var limit = 4096;
        final var in = new BufferedInputStream(inputStream);

        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        // ищем request line
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
           throw  new BadRequestException("Invalid requestLine");
        }

        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
           throw new BadRequestException("Invalid request line");
        }

        final var method = requestLine[0];
        if (!allowedMethods.contains(method)) {
            throw new BadRequestException("Invalid method:" + method);
        }

        final String uri = requestLine[1];
        String path;
        System.out.println(uri);
        if (!uri.startsWith("/")) {
            throw new BadRequestException("Invalid Path");
        }
        List<NameValuePair> queryParams = new ArrayList<>();
        if (uri.contains("?")) {
            path = uri.split("\\?")[0];
            String queryLine =  uri.split("\\?")[1];
            queryParams = URLEncodedUtils.parse(queryLine, Charset.defaultCharset());
        } else {
            path = uri;
        }

        // ищем заголовки
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            throw new BadRequestException("Invalid header");
        }

        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));

        // для GET тела нет
        if (!method.equals(GET)) {
            in.skip(headersDelimiter.length);
            // вычитываем Content-Length, чтобы прочитать body
            final var contentLength = extractHeader(headers, "Content-Length");
            if (contentLength.isPresent()) {
                final var length = Integer.parseInt(contentLength.get());
                final var bodyBytes = in.readNBytes(length);

                final var body = new String(bodyBytes);
                System.out.println(body);
            }
        }
        return new Request(method,path,headers,queryParams, in);
    }
    public Optional<String> getQueryParam(String nameParam) {
        return queryParams.stream()
                .filter(x -> x.getName()
                        .equals(nameParam))
                .findFirst()
                .map(NameValuePair::getValue);
    }

    public List<NameValuePair> getQueryParams() {
        return queryParams;
    }
    @Override
    public String toString() {
        return "server.Request{" +
                "method='" + method + '\'' +
                ", path='" + path + "', " +
                ", headers='" + headers + '\'' +
                ", body='" + body + '\'' +
                '}';
    }

    public String getBody() {
        return body;
    }

    public String getPath() {
        return path;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public String getMethod() {
        return method;
    }

    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
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

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }


}
