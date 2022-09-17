package ru.netology;

public class BadRequestException extends Exception{
    BadRequestException (String message) {
        super(message);
    }
    BadRequestException() {}
}
