package com.example.photo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.photo.model.Album;
import com.example.photo.model.Photo;
import com.example.photo.model.PhotoAlbumManager;
import com.example.photo.model.Tag;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

public class PhotoViewActivity extends AppCompatActivity {

    private ImageView fullPhotoImageView;
    private TextView filenameTextView;
    private TextView tagsTextView;
    private Button prevButton;
    private Button tagButton;
    private Button nextButton;

    private PhotoAlbumManager photoAlbumManager;
    private Album album;
    private List<Photo> photos;
    private int currentPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_view);

        // Getting data from intent
        boolean fromSearch = getIntent().getBooleanExtra("FROM_SEARCH", false);
        currentPosition = getIntent().getIntExtra("POSITION", 0);

        if (fromSearch) {
            // We're showing search results
            photoAlbumManager = PhotoAlbumManager.getInstance();
            photos = photoAlbumManager.getLastSearchResults();

            if (photos == null || photos.isEmpty()) {
                Toast.makeText(this, "No photos to display", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            // Normal album viewing
            String albumName = getIntent().getStringExtra("ALBUM_NAME");
            if (albumName == null) {
                Toast.makeText(this, "Error loading album", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Initializing the manager and getting album
            photoAlbumManager = PhotoAlbumManager.getInstance();
            album = photoAlbumManager.getAlbum(albumName);

            if (album == null) {
                Toast.makeText(this, "Album not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            photos = album.getPhotos();

            if (photos.isEmpty()) {
                Toast.makeText(this, "No photos in album", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        fullPhotoImageView = findViewById(R.id.fullPhotoImageView);
        filenameTextView = findViewById(R.id.filenameTextView);
        tagsTextView = findViewById(R.id.tagsTextView);
        prevButton = findViewById(R.id.prevButton);
        tagButton = findViewById(R.id.tagButton);
        nextButton = findViewById(R.id.nextButton);

        prevButton.setOnClickListener(v -> showPreviousPhoto());
        nextButton.setOnClickListener(v -> showNextPhoto());
        tagButton.setOnClickListener(v -> showTagOptions());

        // Initial photo
        displayCurrentPhoto();
    }

    @Override
    protected void onPause() {
        super.onPause();
        photoAlbumManager.saveAlbums(this);
    }

    private void displayCurrentPhoto() {
        if (currentPosition < 0 || currentPosition >= photos.size()) {
            return;
        }

        Photo photo = photos.get(currentPosition);

        // Displaying the photo using InputStream instead of setImageURI
        try {
            final InputStream imageStream =
                    getContentResolver().openInputStream(photo.getUri());
            final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
            fullPhotoImageView.setImageBitmap(selectedImage);
            if (imageStream != null) {
                imageStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            fullPhotoImageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Display filename and tags
        filenameTextView.setText(photo.getFilename());

        updateTagsDisplay(photo);

        prevButton.setEnabled(currentPosition > 0);
        nextButton.setEnabled(currentPosition < photos.size() - 1);
    }

    private void updateTagsDisplay(Photo photo) {
        Set<Tag> tags = photo.getTags();
        if (tags.isEmpty()) {
            tagsTextView.setText("No tags");
        } else {
            StringBuilder tagsText = new StringBuilder();
            for (Tag tag : tags) {
                tagsText.append(tag.toString()).append("\n");
            }
            // Remove last newline
            if (tagsText.length() > 0) {
                tagsText.setLength(tagsText.length() - 1);
            }
            tagsTextView.setText(tagsText.toString());
        }
    }

    private void showPreviousPhoto() {
        if (currentPosition > 0) {
            currentPosition--;
            displayCurrentPhoto();
        }
    }

    private void showNextPhoto() {
        if (currentPosition < photos.size() - 1) {
            currentPosition++;
            displayCurrentPhoto();
        }
    }


    private void showTagOptions() {
        String[] options = {"Add Tag", "Remove Tag"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tag Options");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                showAddTagDialog();
            } else {
                showRemoveTagDialog();
            }
        });

        builder.show();
    }

    private void showAddTagDialog() {
        String[] tagTypes = {"PERSON", "LOCATION"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Tag Type");
        builder.setItems(tagTypes, (dialog, which) -> {
            Tag.Type selectedType = which == 0 ? Tag.Type.PERSON : Tag.Type.LOCATION;
            showTagValueDialog(selectedType);
        });

        builder.show();
    }

    private void showTagValueDialog(Tag.Type type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter " + type.name() + " Value");

        // The input
        final android.widget.EditText input = new android.widget.EditText(this);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String value = input.getText().toString().trim();
            if (!value.isEmpty()) {
                Photo currentPhoto = photos.get(currentPosition);
                Tag tag = new Tag(type, value);
                if (currentPhoto.addTag(tag)) {
                    updateTagsDisplay(currentPhoto);
                    photoAlbumManager.saveAlbums(PhotoViewActivity.this);
                    Toast.makeText(PhotoViewActivity.this, "Tag added", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PhotoViewActivity.this, "Tag already exists", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showRemoveTagDialog() {
        Photo currentPhoto = photos.get(currentPosition);
        Set<Tag> tags = currentPhoto.getTags();

        if (tags.isEmpty()) {
            Toast.makeText(this, "No tags to remove", Toast.LENGTH_SHORT).show();
            return;
        }

        // Trying to convert tags to array for dialog
        String[] tagStrings = new String[tags.size()];
        Tag[] tagArray = tags.toArray(new Tag[0]);

        for (int i = 0; i < tagArray.length; i++) {
            tagStrings[i] = tagArray[i].toString();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Tag to Remove");
        builder.setItems(tagStrings, (dialog, which) -> {
            Tag tagToRemove = tagArray[which];
            if (currentPhoto.removeTag(tagToRemove)) {
                updateTagsDisplay(currentPhoto);
                photoAlbumManager.saveAlbums(PhotoViewActivity.this);
                Toast.makeText(PhotoViewActivity.this, "Tag removed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(PhotoViewActivity.this, "Failed to remove tag", Toast.LENGTH_SHORT).show();
            }
        });

        builder.show();
    }
}