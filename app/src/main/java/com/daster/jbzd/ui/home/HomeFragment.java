package com.daster.jbzd.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.daster.jbzd.MemeRowAdapter;
import com.daster.jbzd.R;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class HomeFragment extends Fragment {
    ListView memeList;
    SwipeRefreshLayout swipeRefreshLayout;
    MemeRowAdapter memeRowAdapter;
    int page = 1;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        memeList = root.findViewById(R.id.ListViewMemes);
        memeRowAdapter = new MemeRowAdapter(getActivity());
        memeList.setAdapter(memeRowAdapter);
        memeList.setOnScrollListener(new AbsListView.OnScrollListener() {
            private int preLast;
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                final int lastItem = firstVisibleItem + visibleItemCount;

                if(lastItem == totalItemCount)
                {
                    if(preLast!=lastItem)
                    {
                        //to avoid multiple calls for last item
                        Log.d("Last", "Last");
                        preLast = lastItem;
                        StartDataLoader(++page);
                    }
                }
            }
        });
        memeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(memeRowAdapter.getLink(position)));
                startActivity(i);
            }
        });
        memeList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if(isStoragePermissionGranted()){
                    ImageView image = view.findViewById(R.id.imageView);
                    BitmapDrawable draw = (BitmapDrawable)image.getDrawable();
                    Bitmap bitmap = draw.getBitmap();

                    FileOutputStream outStream = null;
                    File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    File dir = new File(sdCard.getAbsolutePath() + "/jbzd");
                    dir.mkdirs();
                    String fileName = String.format("%d.jpg", System.currentTimeMillis());
                    File outFile = new File(dir, fileName);
                    try {
                        outStream = new FileOutputStream(outFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                        outStream.flush();
                        outStream.close();
                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        intent.setData(Uri.fromFile(outFile));
                        getActivity().sendBroadcast(intent);
                    } catch (Exception e) {
                        Toast.makeText(getActivity(),"Błąd podczas zapisywania pliku!", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                    Toast.makeText(getActivity(),"Obrazek zapisany w pamięci urządzenia!", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(getActivity(),"Brak uprawnień do zapisu plików!", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        swipeRefreshLayout = root.findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                StartDataLoader();
            }
        });
        swipeRefreshLayout.setRefreshing(true);
        StartDataLoader();
        return root;
    }
    public Boolean isStoragePermissionGranted(){
        if(getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            return true;
        }else{
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            if(getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                return true;
            }else{
                return false;
            }
        }
    }
    public void StartDataLoader(){
        StartDataLoader(1);
    }
    public void StartDataLoader(int page){
        String siteType = getArguments().getString("siteType");

        //memeRowAdapter.clear();
        switch (siteType){
            case "home":
                new DataLoader().execute("str/"+page);
                break;
            case "waiting":
                new DataLoader().execute("oczekujace/"+page);
                break;
        }
    }

    private class DataLoader extends AsyncTask<String, Integer, JSONArray> {

        String jbzd = "https://jbzd.com.pl/";

        @Override
        protected void onPostExecute(JSONArray result) {
            swipeRefreshLayout.setRefreshing(false);
            if(result != null){
                try {
                    Log.e("JSON", result.toString());
                    for(int i=0; i<result.length(); i++){
                        String title = result.getJSONObject(i).getString("title");
                        String link = result.getJSONObject(i).getString("link");
                        String content = result.getJSONObject(i).getString("content");
                        switch (result.getJSONObject(i).getString("type")){
                            case "img":
                                new DownloadImageTask(title, link).execute(content);
                                break;
                            case "video":
                                memeRowAdapter.addVideo(title+"[video]", link, content);
                                break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else{
                Toast.makeText(getActivity(),"Błąd pobierania danych!", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected JSONArray doInBackground(String... strings) {
            HttpURLConnection connection = null;
            final JSONArray memes = new JSONArray();
            try {
                Document doc = Jsoup.connect(jbzd+strings[0]).get();
                Elements articles = doc.select("article");
                for(Element article : articles){
                    JSONObject meme = new JSONObject();
                    meme.put("title", article.select(".article-title").select("a").text());
                    meme.put("link", article.select(".article-title").select("a").attr("href"));
                    String img = article.select(".article-image").select("img").attr("src");
                    String video = article.select(".article-image").select("video").select("source").attr("src");
                    String type = img != "" ? "img" : video != "" ? "video" : "null";
                    meme.put("type", type);
                    switch (type){
                        case "img":
                            meme.put("content", img);
                            break;
                        case "video":
                            meme.put("content", video);
                            break;
                        case "null":
                            meme.put("content", "null");
                            break;
                    }
                    memes.put(meme);
                }
                return memes;
            }catch (final Exception e){
                Log.e("MEME", e.getMessage());
                return null;
            }
        }
    }
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        String title;
        String link;
        public DownloadImageTask(String title, String link) {
            this.title = title;
            this.link = link;
        }
        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            memeRowAdapter.addImage(title, link, result);
        }
    }
}