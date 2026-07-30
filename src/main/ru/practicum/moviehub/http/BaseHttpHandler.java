package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

public class BaseHttpHandler implements HttpHandler {
    private final MoviesStore store;
    private final Gson gson = new Gson();

    public BaseHttpHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String method = httpExchange.getRequestMethod();
        String path = httpExchange.getRequestURI().getPath();
        String query = httpExchange.getRequestURI().getQuery();

        try {
            switch (method) {
                case "GET" -> handlerGet(httpExchange, path, query);
                case "POST" -> handlerPost(httpExchange);
                case "DELETE" -> handlerDelete(httpExchange, path);
                default -> sendErrorResponse(httpExchange, 405, "Метод не поддерживается");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(httpExchange, 500, "Внутренняя ошибка сервера");
        }
    }

    private void handlerGet(HttpExchange httpExchange, String path, String query) throws IOException {
        try {
            Long id = extractId(path);
            if (id != null) {
                Movie movie = store.getFilm(id);
                if (movie != null) {
                    sendResponse(httpExchange, 200, gson.toJson(movie));
                } else {
                    sendErrorResponse(httpExchange, 404, "Фильм не найден");
                }
                return;
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(httpExchange, 400, "Некорректный ID");
            return;
        }

        if (query != null && query.startsWith("year=")) {
            String yearStr = query.substring(5);
            try {
                int year = Integer.parseInt(yearStr);
                List<Movie> filmOfTheYear = store.getfilmOfTheYear(year);
                sendResponse(httpExchange, 200, gson.toJson(filmOfTheYear));
            } catch (NumberFormatException e) {
                // ⚠️ ВАЖНО: именно это сообщение ожидается в тесте
                sendErrorResponse(httpExchange, 400, "Некорректный параметр запроса — 'year'");
            }
            return;
        }

        List<Movie> allFilms = store.getAllFilms();
        sendResponse(httpExchange, 200, gson.toJson(allFilms));
    }

    private void handlerPost(HttpExchange httpExchange) throws IOException {
        if (!httpExchange.getRequestURI().getPath().equals("/movies")) {
            sendErrorResponse(httpExchange, 405, "Метод не поддерживается для данного пути");
            return;
        }
        String contentType = httpExchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.startsWith("application/json")) {
            sendErrorResponse(httpExchange, 415, "Неподдерживаемый тип содержимого");
            return;
        }
        String body = new String(httpExchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (body.isEmpty()) {
            sendErrorResponse(httpExchange, 400, "Пустое тело запроса");
            return;
        }

        try {
            Movie newFilm = gson.fromJson(body, Movie.class);

            if (newFilm.getNameMovie() == null || newFilm.getNameMovie().trim().isEmpty()) {
                sendValidationError(httpExchange, "название не должно быть пустым");
                return;
            }
            if (newFilm.getNameMovie().length() > 100) {
                sendValidationError(httpExchange, "название не должно превышать 100 символов");
                return;
            }

            int currentYear = LocalDate.now().getYear();
            if (newFilm.getYear() < 1888 || newFilm.getYear() > currentYear + 1) {
                sendValidationError(httpExchange, "год должен быть между 1888 и " + (currentYear + 1));
                return;
            }

            Movie saved = store.save(newFilm);
            sendResponse(httpExchange, 201, gson.toJson(saved));
        } catch (JsonSyntaxException e) {
            sendErrorResponse(httpExchange, 400, "Некорректный JSON");
        }
    }

    private void handlerDelete(HttpExchange httpExchange, String path) throws IOException {
        try {
            Long id = extractId(path);
            if (id == null) {
                sendErrorResponse(httpExchange, 405, "Метод DELETE не поддерживается для /movies");
                return;
            }
            boolean deleted = store.deleteById(id);
            if (deleted) {
                sendNoContent(httpExchange);
            } else {
                sendErrorResponse(httpExchange, 404, "Фильм не найден");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(httpExchange, 400, "Некорректный ID");
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(204, -1);
        exchange.getResponseBody().close();
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        ErrorResponse error = new ErrorResponse(message);
        String json = gson.toJson(error);
        sendResponse(exchange, statusCode, json);
    }

    private void sendValidationError(HttpExchange exchange, String detail) throws IOException {
        ErrorResponse error = new ErrorResponse("Ошибка валидации", List.of(detail));
        String json = gson.toJson(error);
        sendResponse(exchange, 422, json);
    }

    private Long extractId(String path) throws NumberFormatException {
        String[] segments = path.split("/");
        if (segments.length >= 3) {
            String idStr = segments[2];
            if (!idStr.isEmpty()) {
                return Long.parseLong(idStr);
            }
        }
        return null;
    }
}