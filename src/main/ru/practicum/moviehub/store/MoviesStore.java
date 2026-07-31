package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;
import java.util.stream.Collectors;

public class MoviesStore {
    private final Map<Long, Movie> movies = new HashMap<>();
    private long nextId = 1;

    public Movie getFilm(Long id) {
        return movies.get(id);
    }

    public List<Movie> getAllFilms() {
        return new ArrayList<>(movies.values());
    }

    public List<Movie> getfilmOfTheYear(int year) {
        return movies.values().stream()
                .filter(m -> m.getYear() == year)
                .collect(Collectors.toList());
    }

    public Movie save(Movie movie) {
        long id = nextId++;
        movie.setId(id);
        movies.put(id, movie);
        return movie;
    }

    public boolean deleteById(Long id) {
        return movies.remove(id) != null;
    }

    public void clear() {
        movies.clear();
        nextId = 1;
    }
}