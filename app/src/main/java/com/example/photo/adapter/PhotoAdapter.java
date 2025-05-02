package com.example.photo.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photo.R;
import com.example.photo.model.Photo;

import java.io.InputStream;
import java.io.IOException;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private List<Photo> photos;
    private Context context;
    private OnPhotoClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnPhotoClickListener {
        void onPhotoClick(Photo photo, int position);
    }

    public PhotoAdapter(Context context, List<Photo> photos, OnPhotoClickListener listener) {
        this.context = context;
        this.photos = photos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Photo photo = photos.get(position);

        // Safely load image using InputStream instead of direct URI
        try {
            final InputStream imageStream =
                    context.getContentResolver().openInputStream(photo.getUri());
            final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
            holder.photoImageView.setImageBitmap(selectedImage);
            if (imageStream != null) {
                imageStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Show a placeholder if image loading fails
            holder.photoImageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.fileNameTextView.setText(photo.getFilename());

        // Highlight selected item
        if (selectedPosition == position) {
            holder.itemView.setBackgroundResource(R.color.selected_item);
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                // Update selection
                int previousSelected = selectedPosition;
                selectedPosition = position;

                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPosition);

                listener.onPhotoClick(photo, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    public void updatePhotos(List<Photo> photos) {
        this.photos = photos;
        selectedPosition = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public void clearSelection() {
        int oldPosition = selectedPosition;
        selectedPosition = RecyclerView.NO_POSITION;
        if (oldPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(oldPosition);
        }
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView photoImageView;
        TextView fileNameTextView;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            photoImageView = itemView.findViewById(R.id.photoImageView);
            fileNameTextView = itemView.findViewById(R.id.fileNameTextView);
        }
    }
}