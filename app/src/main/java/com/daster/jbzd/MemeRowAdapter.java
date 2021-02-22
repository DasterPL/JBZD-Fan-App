package com.daster.jbzd;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
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
    ArrayList<String> links = new ArrayList<>();
    ArrayList<Bitmap> images = new ArrayList<>();
    ArrayList<String> videos = new ArrayList<>();
    public MemeRowAdapter(@NonNull Context context) {
        super(context, R.layout.meme_row);
        this.context = context;
    }

    @Deprecated
    public void add(String object) {
        throw new UnsupportedOperationException();
    }
    public void addImage(String title, String link, Bitmap image){
        super.add(title);
        titles.add(title);
        links.add(link);
        images.add(image);
        videos.add(null);
    }
    public void addVideo(String title, String link, String url){
        super.add(title);
        titles.add(title);
        links.add(link);
        videos.add(url);
        images.add(null);
    }

    @Override
    public void clear() {
        super.clear();
        this.titles.clear();
        this.images.clear();
        this.links.clear();
        this.videos.clear();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.meme_row, parent, false);

        TextView mTitle = row.findViewById(R.id.textViewMemeTitle);
        ImageView mImage = row.findViewById(R.id.imageView);
        final VideoView mVideo = row.findViewById(R.id.videoView);

        mTitle.setText(titles.get(position));
        if(images.get(position) != null) {
            mImage.setImageBitmap(images.get(position));
            mVideo.setVisibility(View.GONE);
        }else if(videos.get(position) != null){
            //MediaController controller = new MediaController(mVideo.getContext());
            //controller.setMediaPlayer(mVideo);
            //mVideo.setMediaController(controller);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mVideo.setVideoURI(Uri.parse(videos.get(position)));
                }
            }).start();

            mVideo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mVideo.isPlaying()){
                        mVideo.pause();
                    }else {
                        mVideo.start();
                    }
                }
            });
            mVideo.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mVideo.seekTo(1);
                }
            });

            mVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mVideo.seekTo(1);
                }
            });

            mImage.setVisibility(View.GONE);
        }
        return row;
    }
    public String getLink(int position){
        return links.get(position);
    }
}
