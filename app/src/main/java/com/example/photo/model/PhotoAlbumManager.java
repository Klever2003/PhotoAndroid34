package com.example.photo.model;

import android.content.Context;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class PhotoAlbumManager {
    private static final String FILENAME = "photoalbum.dat";

    private ArrayList<Album> albums;
    private static PhotoAlbumManager instance;

    private PhotoAlbumManager() {
        albums = new ArrayList<>();
        lastSearchResults = new ArrayList<>();
    }

    public static PhotoAlbumManager getInstance() {
        if (instance == null) {
            instance = new PhotoAlbumManager();
        }
        return instance;
    }

    public List<Album> getAlbums() {
        return new ArrayList<>(albums);
    }

    public Album getAlbum(String name) {
        for (Album album : albums) {
            if (album.getName().equals(name)) {
                return album;
            }
        }
        return null;
    }

    public Album createAlbum(String name) {
        if (getAlbum(name) != null) {
            return null; // Album with this name already exists
        }

        Album album = new Album(name);
        albums.add(album);
        return album;
    }

    public boolean deleteAlbum(String name) {
        Album album = getAlbum(name);
        if (album != null) {
            return albums.remove(album);
        }
        return false;
    }

    public boolean renameAlbum(String oldName, String newName) {
        Album album = getAlbum(oldName);
        if (album == null || getAlbum(newName) != null) {
            return false;
        }

        album.setName(newName);
        return true;
    }

    public void saveAlbums(Context context) {
        try {
            FileOutputStream fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(albums);
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadAlbums(Context context) {
        try {
            FileInputStream fis = context.openFileInput(FILENAME);
            ObjectInputStream ois = new ObjectInputStream(fis);
            albums = (ArrayList<Album>) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException | ClassNotFoundException e) {
            // First time use or error, start with empty list
            albums = new ArrayList<>();
        }
    }

    private List<Photo> lastSearchResults;

    public List<Photo> getLastSearchResults() {
        return lastSearchResults != null ? new ArrayList<>(lastSearchResults) : new ArrayList<>();
    }
    public void setLastSearchResults(List<Photo> results) {
        this.lastSearchResults = new ArrayList<>(results);
    }
    // Search methods
    public List<Photo> searchPhotosByTag(Tag.Type type, String value, boolean startsWith) {
        List<Photo> results = new ArrayList<>();

        for (Album album : albums) {
            for (Photo photo : album.getPhotos()) {
                for (Tag tag : photo.getTags()) {
                    if (tag.getType() == type) {
                        String tagValue = tag.getValue().toLowerCase();
                        String searchValue = value.toLowerCase();

                        if (startsWith ? tagValue.startsWith(searchValue) : tagValue.equals(searchValue)) {
                            results.add(photo);
                            break;
                        }
                    }
                }
            }
        }

        return results;
    }
}
