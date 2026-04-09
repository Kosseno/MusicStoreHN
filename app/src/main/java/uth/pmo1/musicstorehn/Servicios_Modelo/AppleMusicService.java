package uth.pmo1.musicstorehn.Servicios_Modelo;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class AppleMusicService {
    private Context context;

    public AppleMusicService(Context context){
        this.context = context;
    }

    public void searchSongsByTerm(String searchTerm, OnDataResponse delegate){
        String url = "https://itunes.apple.com/search?media=music&entity=song&term=" + searchTerm.replace(" ", "+");

        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    Gson gson = new Gson();
                    Root root = gson.fromJson(response, Root.class);
                    if(delegate !=  null){
                        delegate.onChange(false, 200, root);
                    }
                },
                error -> {
                    Log.d("AppleMusicService", "Error: " + error.getMessage());
                    if(delegate !=  null){
                        delegate.onChange(true, -1, null);
                    }
                });

        queue.add(stringRequest);
    }

    public interface OnDataResponse{
        void onChange(boolean isNetworkError, int statusCode, Root root);
    }
}
