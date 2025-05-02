package com.example.photo.model;

import android.net.Uri;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Photo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uriString;  // Store as String for serialization
    private String filename;
    private Set<Tag> tags;

    // Transient so it's not serialized
    private transient Uri uri;

    public Photo(Uri uri, String filename) {
        this.uri = uri;
        this.uriString = uri.toString();
        this.filename = filename;
        this.tags = new HashSet<>();
    }

    public Uri getUri() {
        if (uri == null && uriString != null) {
            uri = Uri.parse(uriString);
        }
        return uri;
    }

    public String getFilename() {
        return filename;
    }

    public Set<Tag> getTags() {
        return new HashSet<>(tags);
    }

    public boolean addTag(Tag tag) {
        return tags.add(tag);
    }

    public boolean removeTag(Tag tag) {
        return tags.remove(tag);
    }

    public boolean hasTag(Tag.Type type, String value) {
        return tags.contains(new Tag(type, value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Photo photo = (Photo) o;
        return uriString.equals(photo.uriString);
    }

    @Override
    public int hashCode() {
        return uriString.hashCode();
    }
}