package com.daster.jbzd;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class MemeRowAdapter extends ArrayAdapter<String> {

    Context context;
    ArrayList<String> titles = new ArrayList<>();
    ArrayList<Bitmap> images = new ArrayList<>();
    public MemeRowAdapter(@NonNull Context context) {
        super(context, R.layout.meme_row);
        this.context = context;
    }

    @Deprecated
    public void add(String object) {
        throw new UnsupportedOperationException();
    }
    public void add(String title, Bitmap image){
        super.add(title);
        titles.add(title);
        images.add(image);
    }

    @Override
    public void clear() {
        super.clear();
        this.titles.clear();
        this.images.clear();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.meme_row, parent, false);

        TextView mTitle = row.findViewById(R.id.textViewMemeTitle);
        ImageView mImage = row.findViewById(R.id.imageView);
        VideoView mVideo = row.findViewById(R.id.videoView);

        mTitle.setText(titles.get(position));
        mImage.setImageBitmap(images.get(position));

        return row;
    }
}
