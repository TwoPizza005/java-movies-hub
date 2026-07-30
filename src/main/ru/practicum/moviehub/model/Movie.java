package ru.practicum.moviehub.model;

public class Movie {
    private String nameMovie;
    private int year;
    private long id;

    // Конструктор без ID (для создания нового фильма)
    public Movie(int year, String nameMovie) {
        this.year = year;
        this.nameMovie = nameMovie;
    }

    public Movie() {
    }

    // Геттеры и сеттеры
    public String getNameMovie() {
        return nameMovie;
    }

    public void setNameMovie(String nameMovie) {
        this.nameMovie = nameMovie;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Movie{" +
                "nameMovie='" + nameMovie + '\'' +
                ", year=" + year +
                ", id=" + id +
                '}';
    }
}