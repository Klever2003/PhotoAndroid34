package com.example.photo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.example.photo.adapter.AlbumAdapter;
import com.example.photo.model.Album;
import com.example.photo.model.PhotoAlbumManager;

public class MainActivity extends AppCompatActivity implements AlbumAdapter.OnAlbumClickListener {

    private RecyclerView albumsRecyclerView;
    private EditText albumNameEditText;
    private Button createAlbumButton;
    private Button searchButton;

    private AlbumAdapter albumAdapter;
    private PhotoAlbumManager photoAlbumManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, 100);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            }
        }

        photoAlbumManager = PhotoAlbumManager.getInstance();
        photoAlbumManager.loadAlbums(this);

        albumsRecyclerView = findViewById(R.id.albumsRecyclerView);
        albumNameEditText = findViewById(R.id.albumNameEditText);
        createAlbumButton = findViewById(R.id.createAlbumButton);
        searchButton = findViewById(R.id.searchButton);

        // RecyclerView that is needed
        albumsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        albumAdapter = new AlbumAdapter(this, photoAlbumManager.getAlbums(), this);
        albumsRecyclerView.setAdapter(albumAdapter);

        createAlbumButton.setOnClickListener(v -> handleCreateAlbum());
        searchButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Process to save albums when activity is paused
        photoAlbumManager.saveAlbums(this);
    }

    private void handleCreateAlbum() {
        String albumName = albumNameEditText.getText().toString().trim();

        if (albumName.isEmpty()) {
            Toast.makeText(this, "Album name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Album album = photoAlbumManager.createAlbum(albumName);
        if (album != null) {
            albumAdapter.updateAlbums(photoAlbumManager.getAlbums());
            albumNameEditText.setText("");
            Toast.makeText(this, "Album created", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Album already exists", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAlbumClick(Album album) {
        Intent intent = new Intent(this, AlbumActivity.class);
        intent.putExtra("ALBUM_NAME", album.getName());
        startActivity(intent);
    }

    @Override
    public void onAlbumLongClick(Album album, View view) {
        // Show popup menu for rename/delete
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.album_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_rename) {
                showRenameDialog(album);
                return true;
            } else if (id == R.id.menu_delete) {
                showDeleteDialog(album);
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void showRenameDialog(Album album) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Album");

        final EditText input = new EditText(this);
        input.setText(album.getName());
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                if (photoAlbumManager.renameAlbum(album.getName(), newName)) {
                    albumAdapter.updateAlbums(photoAlbumManager.getAlbums());
                    Toast.makeText(MainActivity.this, "Album renamed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to rename album", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showDeleteDialog(Album album) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Album");
        builder.setMessage("Are you sure you want to delete this album?");

        builder.setPositiveButton("Yes", (dialog, which) -> {
            if (photoAlbumManager.deleteAlbum(album.getName())) {
                albumAdapter.updateAlbums(photoAlbumManager.getAlbums());
                Toast.makeText(MainActivity.this, "Album deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Failed to delete album", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("No", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}