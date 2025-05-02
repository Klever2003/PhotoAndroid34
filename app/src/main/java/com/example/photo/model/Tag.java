package com.example.photo.model;

import java.io.Serializable;
import java.util.Objects;

public class Tag implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type { PERSON, LOCATION }

    private Type type;
    private String value;

    public Tag(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return type == tag.type &&
                value.toLowerCase().equals(tag.value.toLowerCase()); // Case insensitive
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value.toLowerCase());
    }

    @Override
    public String toString() {
        return type.name() + ": " + value;
    }
}