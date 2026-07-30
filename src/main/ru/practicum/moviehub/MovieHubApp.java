package ru.practicum.moviehub;

import ru.practicum.moviehub.http.MoviesServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;

public class MovieHubApp {
    public static void main(String[] args) throws IOException {
        MoviesStore store = new MoviesStore();
        final MoviesServer server = new MoviesServer(store, 8080);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
        System.out.println("Сервер запущен на http://localhost:8080/movies");
    }
}