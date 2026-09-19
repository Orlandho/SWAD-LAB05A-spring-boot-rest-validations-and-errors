package com.ejemplo.laboratorio05.exception;

public class ProductoDuplicadoException extends RuntimeException {
    public ProductoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
