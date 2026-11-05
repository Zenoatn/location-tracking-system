package br.edu.maua.server.services;

public class OperationException extends Exception {

    private final String code;

    public OperationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
