package com.example.photo;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photo.adapter.PhotoAdapter;
import com.example.photo.model.Album;
import com.example.photo.model.Photo;
import com.example.photo.model.PhotoAlbumManager;
import com.example.photo.model.Tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchActivity extends AppCompatActivity implements PhotoAdapter.OnPhotoClickListener {

    private RadioGroup searchTypeRadioGroup;
    private RadioButton singleTagRadioButton;
    private RadioButton andRadioButton;
    private RadioButton orRadioButton;

    private Spinner tagTypeSpinner;
    private AutoCompleteTextView tagValueAutoComplete;
    private View secondTagLayout;
    private Spinner secondTagTypeSpinner;
    private AutoCompleteTextView secondTagValueAutoComplete;

    private Button searchButton;
    private RecyclerView searchResultsRecyclerView;

    private PhotoAlbumManager photoAlbumManager;
    private PhotoAdapter photoAdapter;
    private List<Photo> searchResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Initializing views
        searchTypeRadioGroup = findViewById(R.id.searchTypeRadioGroup);
        singleTagRadioButton = findViewById(R.id.singleTagRadioButton);
        andRadioButton = findViewById(R.id.andRadioButton);
        orRadioButton = findViewById(R.id.orRadioButton);

        tagTypeSpinner = findViewById(R.id.tagTypeSpinner);
        tagValueAutoComplete = findViewById(R.id.tagValueAutoComplete);
        secondTagLayout = findViewById(R.id.secondTagLayout);
        secondTagTypeSpinner = findViewById(R.id.secondTagTypeSpinner);
        secondTagValueAutoComplete = findViewById(R.id.secondTagValueAutoComplete);

        searchButton = findViewById(R.id.searchButton);
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);

        // Manager
        photoAlbumManager = PhotoAlbumManager.getInstance();

        // Creating empty results list
        searchResults = new ArrayList<>();
        photoAdapter = new PhotoAdapter(this, searchResults, this);
        searchResultsRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        searchResultsRecyclerView.setAdapter(photoAdapter);

        // Setup spinners and auto-complete
        setupTagTypeSpinners();

        setupAutoComplete();

        searchTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.singleTagRadioButton) {
                secondTagLayout.setVisibility(View.GONE);
            } else {
                secondTagLayout.setVisibility(View.VISIBLE);
            }
        });

        // Setting up a search button
        searchButton.setOnClickListener(v -> performSearch());
    }

    private void setupTagTypeSpinners() {
        // Setup spinner with PERSON and LOCATION options
        String[] tagTypes = {"PERSON", "LOCATION"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, tagTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        tagTypeSpinner.setAdapter(adapter);
        secondTagTypeSpinner.setAdapter(adapter);
    }

    private void setupAutoComplete() {
        // Get all unique tag values for auto-completion
        Set<String> personValues = new HashSet<>();
        Set<String> locationValues = new HashSet<>();

        // Collect all tag values from all photos in all albums
        for (Album album : photoAlbumManager.getAlbums()) {
            for (Photo photo : album.getPhotos()) {
                for (Tag tag : photo.getTags()) {
                    if (tag.getType() == Tag.Type.PERSON) {
                        personValues.add(tag.getValue());
                    } else if (tag.getType() == Tag.Type.LOCATION) {
                        locationValues.add(tag.getValue());
                    }
                }
            }
        }

        // Creating adapters for auto-complete
        ArrayAdapter<String> personAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>(personValues));
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>(locationValues));


        tagValueAutoComplete.setThreshold(1);

        secondTagValueAutoComplete.setThreshold(1);

        updateAutoCompleteAdapters();

        // Updating the adapters when tag type changes
        tagTypeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateAutoCompleteAdapters();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Do nothing
            }
        });

        secondTagTypeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateAutoCompleteAdapters();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void updateAutoCompleteAdapters() {
        // Get all unique tag values for auto-completion
        Set<String> personValues = new HashSet<>();
        Set<String> locationValues = new HashSet<>();

        // Collecting all tag values from all photos in all albums
        for (Album album : photoAlbumManager.getAlbums()) {
            for (Photo photo : album.getPhotos()) {
                for (Tag tag : photo.getTags()) {
                    if (tag.getType() == Tag.Type.PERSON) {
                        personValues.add(tag.getValue());
                    } else if (tag.getType() == Tag.Type.LOCATION) {
                        locationValues.add(tag.getValue());
                    }
                }
            }
        }

        ArrayAdapter<String> personAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>(personValues));
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>(locationValues));

        // Setting up an appropriate adapter based on tag type
        if (tagTypeSpinner.getSelectedItemPosition() == 0) { // PERSON
            tagValueAutoComplete.setAdapter(personAdapter);
        } else { // LOCATION
            tagValueAutoComplete.setAdapter(locationAdapter);
        }

        if (secondTagTypeSpinner.getSelectedItemPosition() == 0) { // PERSON
            secondTagValueAutoComplete.setAdapter(personAdapter);
        } else { // LOCATION
            secondTagValueAutoComplete.setAdapter(locationAdapter);
        }
    }

    private void performSearch() {
        // Get search parameters
        String tagValue = tagValueAutoComplete.getText().toString().trim();
        if (tagValue.isEmpty()) {
            Toast.makeText(this, "Please enter a tag value", Toast.LENGTH_SHORT).show();
            return;
        }

        Tag.Type tagType = tagTypeSpinner.getSelectedItemPosition() == 0 ?
                Tag.Type.PERSON : Tag.Type.LOCATION;

        // Perform search based on radio button selection
        List<Photo> results = new ArrayList<>();

        if (singleTagRadioButton.isChecked()) {
            // Single tag search
            results = searchPhotosWithTag(tagType, tagValue);
        } else {
            // Conjunction or disjunction search
            String secondTagValue = secondTagValueAutoComplete.getText().toString().trim();
            if (secondTagValue.isEmpty()) {
                Toast.makeText(this, "Please enter a second tag value", Toast.LENGTH_SHORT).show();
                return;
            }

            Tag.Type secondTagType = secondTagTypeSpinner.getSelectedItemPosition() == 0 ?
                    Tag.Type.PERSON : Tag.Type.LOCATION;

            if (andRadioButton.isChecked()) {
                // AND search
                results = searchPhotosWithBothTags(tagType, tagValue, secondTagType, secondTagValue);
            } else {
                // OR search
                results = searchPhotosWithEitherTag(tagType, tagValue, secondTagType, secondTagValue);
            }
        }

        // Update results
        searchResults.clear();
        searchResults.addAll(results);
        photoAdapter.updatePhotos(searchResults);

        photoAlbumManager.setLastSearchResults(searchResults);
        // Show message if no results
        if (results.isEmpty()) {
            Toast.makeText(this, "No matching photos found", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, results.size() + " photos found", Toast.LENGTH_SHORT).show();
        }
    }

    private List<Photo> searchPhotosWithTag(Tag.Type tagType, String tagValue) {
        List<Photo> results = new ArrayList<>();

        // Search across all albums
        for (Album album : photoAlbumManager.getAlbums()) {
            for (Photo photo : album.getPhotos()) {
                // Check each tag of the photo
                for (Tag tag : photo.getTags()) {
                    // Check if tag type matches and value starts with search value
                    if (tag.getType() == tagType &&
                            tag.getValue().toLowerCase().startsWith(tagValue.toLowerCase())) {
                        // Add to results if not already added
                        if (!results.contains(photo)) {
                            results.add(photo);
                        }
                        break;
                    }
                }
            }
        }

        return results;
    }

    private List<Photo> searchPhotosWithBothTags(Tag.Type tagType1, String tagValue1,
                                                 Tag.Type tagType2, String tagValue2) {
        List<Photo> results = new ArrayList<>();

        // Search across all albums
        for (Album album : photoAlbumManager.getAlbums()) {
            for (Photo photo : album.getPhotos()) {
                boolean hasTag1 = false;
                boolean hasTag2 = false;

                // Check each tag of the photo
                for (Tag tag : photo.getTags()) {
                    // Check if tag matches first condition
                    if (!hasTag1 && tag.getType() == tagType1 &&
                            tag.getValue().toLowerCase().startsWith(tagValue1.toLowerCase())) {
                        hasTag1 = true;
                    }

                    // Check if tag matches second condition
                    if (!hasTag2 && tag.getType() == tagType2 &&
                            tag.getValue().toLowerCase().startsWith(tagValue2.toLowerCase())) {
                        hasTag2 = true;
                    }

                    // Break early if both conditions are met
                    if (hasTag1 && hasTag2) {
                        break;
                    }
                }

                // Add to results if both conditions are met
                if (hasTag1 && hasTag2 && !results.contains(photo)) {
                    results.add(photo);
                }
            }
        }

        return results;
    }

    private List<Photo> searchPhotosWithEitherTag(Tag.Type tagType1, String tagValue1,
                                                  Tag.Type tagType2, String tagValue2) {
        List<Photo> results = new ArrayList<>();

        // Search across all albums
        for (Album album : photoAlbumManager.getAlbums()) {
            for (Photo photo : album.getPhotos()) {
                boolean matchFound = false;

                // Check each tag of the photo
                for (Tag tag : photo.getTags()) {
                    // Check if tag matches either condition
                    if ((tag.getType() == tagType1 &&
                            tag.getValue().toLowerCase().startsWith(tagValue1.toLowerCase())) ||
                            (tag.getType() == tagType2 &&
                                    tag.getValue().toLowerCase().startsWith(tagValue2.toLowerCase()))) {
                        // Add to results if not already added
                        if (!results.contains(photo)) {
                            results.add(photo);
                            matchFound = true;
                            break;
                        }
                    }
                }

                // Break early if a match is found
                if (matchFound) {
                    break;
                }
            }
        }

        return results;
    }

    @Override
    public void onPhotoClick(Photo photo, int position) {
        Intent intent = new Intent(this, PhotoViewActivity.class);

        intent.putExtra("FROM_SEARCH", true);
        intent.putExtra("POSITION", position);

        startActivity(intent);
    }
}