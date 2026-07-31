package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.api.ListOfMoviesTypeToken;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MoviesApiTest {
    private static final int PORT = 8080;
    private static final String BASE_URL = "http://localhost:" + PORT + "/movies";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    private MoviesStore store;
    private MoviesServer server;

    @BeforeAll
    void startServer() throws IOException {
        store = new MoviesStore();
        server = new MoviesServer(store, PORT);
        server.start();
    }

    @AfterAll
    void stopServer() {
        server.stop();
    }

    @BeforeEach
    void clearStore() {
        store.clear();
    }

    // ========== GET /movies ==========

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("application/json; charset=UTF-8", response.headers().firstValue("Content-Type").orElse(""));
        List<Movie> movies = gson.fromJson(response.body(), new ListOfMoviesTypeToken().getType());
        assertTrue(movies.isEmpty());
    }

    @Test
    void getMovies_whenHasFilms_returnsList() throws Exception {
        store.save(new Movie(2010, "Inception"));
        store.save(new Movie(2014, "Interstellar"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("application/json; charset=UTF-8", response.headers().firstValue("Content-Type").orElse(""));
        List<Movie> movies = gson.fromJson(response.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(2, movies.size());
        assertTrue(movies.stream().anyMatch(m -> "Inception".equals(m.getNameMovie())));
    }

    // ========== POST /movies ==========

    @Test
    void postMovie_whenValid_returns201AndSavedMovie() throws Exception {
        String json = "{\"nameMovie\":\"The Matrix\", \"year\":1999}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        assertEquals("application/json; charset=UTF-8", response.headers().firstValue("Content-Type").orElse(""));
        Movie saved = gson.fromJson(response.body(), Movie.class);
        assertNotNull(saved.getId());
        assertEquals("The Matrix", saved.getNameMovie());
        assertEquals(1999, saved.getYear());
    }

    @Test
    void postMovie_whenEmptyTitle_returns422() throws Exception {
        String json = "{\"nameMovie\":\"\", \"year\":2000}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, response.statusCode());
        assertTrue(response.body().contains("название не должно быть пустым"));
    }

    @Test
    void postMovie_whenTitleTooLong_returns422() throws Exception {
        String longTitle = "a".repeat(101);
        String json = "{\"nameMovie\":\"" + longTitle + "\", \"year\":2000}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, response.statusCode());
        assertTrue(response.body().contains("название не должно превышать 100 символов"));
    }

    @Test
    void postMovie_whenInvalidYear_returns422() throws Exception {
        int invalidYear = 1800;
        String json = "{\"nameMovie\":\"Old Film\", \"year\":" + invalidYear + "}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, response.statusCode());
        int currentYear = LocalDate.now().getYear();
        assertTrue(response.body().contains("год должен быть между 1888 и " + (currentYear + 1)));
    }

    @Test
    void postMovie_whenWrongContentType_returns415() throws Exception {
        String json = "{\"nameMovie\":\"Inception\", \"year\":2010}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(415, response.statusCode());
        assertTrue(response.body().contains("Неподдерживаемый тип содержимого"));
    }

    @Test
    void postMovie_whenInvalidJson_returns400() throws Exception {
        String invalidJson = "{\"nameMovie\":\"Inception\", \"year\":\"two thousand\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Некорректный JSON"));
    }

    // ========== GET /movies/{id} ==========

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        Movie saved = store.save(new Movie(2009, "Avatar"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + saved.getId()))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("application/json; charset=UTF-8", response.headers().firstValue("Content-Type").orElse(""));
        Movie movie = gson.fromJson(response.body(), Movie.class);
        assertEquals(saved.getId(), movie.getId());
        assertEquals("Avatar", movie.getNameMovie());
        assertEquals(2009, movie.getYear());
    }

    @Test
    void getMovieById_whenNotFound_returns404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/999"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("Фильм не найден"));
    }

    @Test
    void getMovieById_whenInvalidId_returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/abc"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Некорректный ID"));
    }

    // ========== DELETE /movies/{id} ==========

    @Test
    void deleteMovieById_whenExists_returns204() throws Exception {
        Movie saved = store.save(new Movie(1994, "Pulp Fiction"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + saved.getId()))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(204, response.statusCode());
        assertNull(store.getFilm(saved.getId()));
    }

    @Test
    void deleteMovieById_whenNotFound_returns404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/999"))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("Фильм не найден"));
    }

    @Test
    void deleteMovieById_whenInvalidId_returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/abc"))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Некорректный ID"));
    }

    // ========== GET /movies?year=YYYY ==========

    @Test
    void getMoviesByYear_whenMatches_returnsFilteredList() throws Exception {
        store.save(new Movie(2000, "Gladiator"));
        store.save(new Movie(2000, "Memento"));
        store.save(new Movie(2001, "A Beautiful Mind"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "?year=2000"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("application/json; charset=UTF-8", response.headers().firstValue("Content-Type").orElse(""));
        List<Movie> movies = gson.fromJson(response.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(2, movies.size());
        assertTrue(movies.stream().allMatch(m -> m.getYear() == 2000));
    }

    @Test
    void getMoviesByYear_whenNoMatches_returnsEmptyList() throws Exception {
        store.save(new Movie(2005, "Batman Begins"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "?year=1999"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("application/json; charset=UTF-8", response.headers().firstValue("Content-Type").orElse(""));
        List<Movie> movies = gson.fromJson(response.body(), new ListOfMoviesTypeToken().getType());
        assertTrue(movies.isEmpty());
    }

    @Test
    void getMoviesByYear_whenInvalidYear_returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "?year=abc"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Некорректный параметр запроса"));
    }

    // ========== 405 ==========

    @Test
    void unsupportedMethod_returns405() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
        assertTrue(response.body().contains("Метод не поддерживается"));
    }
}