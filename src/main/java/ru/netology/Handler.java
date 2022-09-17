package ru.netology;

import java.io.BufferedOutputStream;

@FunctionalInterface
interface Handler {
    void handle(Request request, BufferedOutputStream out);
}
