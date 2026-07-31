package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseHttpHandler {
    protected final Gson gson = new Gson();
    protected final Logger logger = Logger.getLogger(getClass().getName());

    protected void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(204, -1);
        exchange.getResponseBody().close();
    }

    protected void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        ErrorResponse error = new ErrorResponse(message);
        String json = gson.toJson(error);
        sendResponse(exchange, statusCode, json);
    }

    protected void sendValidationError(HttpExchange exchange, String detail) throws IOException {
        ErrorResponse error = new ErrorResponse("Ошибка валидации", List.of(detail));
        String json = gson.toJson(error);
        sendResponse(exchange, 422, json);
    }

    protected void logError(String msg, Exception e) {
        logger.log(Level.SEVERE, msg, e);
    }
}