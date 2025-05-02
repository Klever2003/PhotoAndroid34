package com.example.photo.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Album implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private List<Photo> photos;

    public Album(String name) {
        this.name = name;
        this.photos = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Photo> getPhotos() {
        return new ArrayList<>(photos);
    }

    public int getPhotoCount() {
        return photos.size();
    }

    public boolean addPhoto(Photo photo) {
        if (!photos.contains(photo)) {
            return photos.add(photo);
        }
        return false;
    }

    public boolean removePhoto(Photo photo) {
        return photos.remove(photo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Album album = (Album) o;
        return name.equals(album.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}