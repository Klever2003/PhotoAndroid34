package com.example.photo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.photo.adapter.PhotoAdapter;
import com.example.photo.model.Album;
import com.example.photo.model.Photo;
import com.example.photo.model.PhotoAlbumManager;

import java.util.List;

public class AlbumActivity extends AppCompatActivity implements PhotoAdapter.OnPhotoClickListener {

    private TextView albumTitleTextView;
    private RecyclerView photosRecyclerView;
    private Button addPhotoButton;
    private Button removePhotoButton;
    private Button movePhotoButton;
    private Button slideshowButton;

    private PhotoAlbumManager photoAlbumManager;
    private Album album;
    private PhotoAdapter photoAdapter;
    private Photo selectedPhoto;

    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        String albumName = getIntent().getStringExtra("ALBUM_NAME");
        if (albumName == null) {
            Toast.makeText(this, "Error loading album", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        photoAlbumManager = PhotoAlbumManager.getInstance();
        album = photoAlbumManager.getAlbum(albumName);

        if (album == null) {
            Toast.makeText(this, "Album not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        albumTitleTextView = findViewById(R.id.albumTitleTextView);
        photosRecyclerView = findViewById(R.id.photosRecyclerView);
        addPhotoButton = findViewById(R.id.addPhotoButton);
        removePhotoButton = findViewById(R.id.removePhotoButton);
        movePhotoButton = findViewById(R.id.movePhotoButton);
        slideshowButton = findViewById(R.id.slideshowButton);

        albumTitleTextView.setText(album.getName());

        photosRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        photoAdapter = new PhotoAdapter(this, album.getPhotos(), this);
        photosRecyclerView.setAdapter(photoAdapter);

        addPhotoButton.setOnClickListener(v -> pickImage());
        removePhotoButton.setOnClickListener(v -> removePhoto());
        movePhotoButton.setOnClickListener(v -> movePhoto());
        slideshowButton.setOnClickListener(v -> startSlideshow());

        // Register activity result launcher for picking images
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            addPhotoToAlbum(imageUri);
                        }
                    }
                }
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        photoAlbumManager.saveAlbums(this);
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }
    private void addPhotoToAlbum(Uri imageUri) {
        try {
            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
            getContentResolver().takePersistableUriPermission(imageUri, takeFlags);
        } catch (SecurityException e) {
        }

        // Get the filename
        String filename = getFileNameFromUri(imageUri);
        if (filename == null) {
            filename = "Photo_" + System.currentTimeMillis();
        }

        // Create a new photo
        Photo photo = new Photo(imageUri, filename);

        // Add to album
        if (album.addPhoto(photo)) {
            photoAdapter.updatePhotos(album.getPhotos());
            photoAlbumManager.saveAlbums(this);
            Toast.makeText(this, "Photo added", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Photo already exists in album", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void removePhoto() {
        if (selectedPhoto == null) {
            Toast.makeText(this, "No photo selected", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove Photo");
        builder.setMessage("Are you sure you want to remove this photo from the album?");

        builder.setPositiveButton("Yes", (dialog, which) -> {
            if (album.removePhoto(selectedPhoto)) {
                selectedPhoto = null;
                photoAdapter.clearSelection();
                photoAdapter.updatePhotos(album.getPhotos());
                photoAlbumManager.saveAlbums(AlbumActivity.this);

                removePhotoButton.setEnabled(false);
                movePhotoButton.setEnabled(false);

                Toast.makeText(AlbumActivity.this, "Photo removed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AlbumActivity.this, "Failed to remove photo", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("No", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void movePhoto() {
        if (selectedPhoto == null) {
            Toast.makeText(this, "No photo selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get list of other albums
        List<Album> albums = photoAlbumManager.getAlbums();
        if (albums.size() <= 1) {
            Toast.makeText(this, "No other albums available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a list of album names (excluding current)
        String[] albumNames = new String[albums.size() - 1];
        int index = 0;
        for (Album a : albums) {
            if (!a.getName().equals(album.getName())) {
                albumNames[index++] = a.getName();
            }
        }

        // Show album selection dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Move to Album");
        builder.setItems(albumNames, (dialog, which) -> {
            String destAlbumName = albumNames[which];
            Album destAlbum = photoAlbumManager.getAlbum(destAlbumName);

            if (destAlbum.addPhoto(selectedPhoto) && album.removePhoto(selectedPhoto)) {
                selectedPhoto = null;
                photoAdapter.clearSelection();
                photoAdapter.updatePhotos(album.getPhotos());
                photoAlbumManager.saveAlbums(AlbumActivity.this);

                removePhotoButton.setEnabled(false);
                movePhotoButton.setEnabled(false);

                Toast.makeText(AlbumActivity.this, "Photo moved to " + destAlbumName, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AlbumActivity.this, "Failed to move photo", Toast.LENGTH_SHORT).show();
            }
        });

        builder.show();
    }

    private void startSlideshow() {
        if (album.getPhotoCount() == 0) {
            Toast.makeText(this, "No photos in album", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PhotoViewActivity.class);
        intent.putExtra("ALBUM_NAME", album.getName());
        intent.putExtra("POSITION", photoAdapter.getSelectedPosition() >= 0 ?
                photoAdapter.getSelectedPosition() : 0);
        startActivity(intent);
    }

    @Override
    public void onPhotoClick(Photo photo, int position) {
        selectedPhoto = photo;
        removePhotoButton.setEnabled(true);
        movePhotoButton.setEnabled(true);
    }
}