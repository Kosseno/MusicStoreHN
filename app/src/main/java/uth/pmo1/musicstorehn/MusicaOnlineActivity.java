package uth.pmo1.musicstorehn;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import uth.pmo1.musicstorehn.Servicios_Modelo.AppleMusicService;
import uth.pmo1.musicstorehn.Servicios_Modelo.Result;
import es.dmoral.toasty.Toasty;

public class MusicaOnlineActivity extends AppCompatActivity {

    private EditText txtSearch;
    private ListView listViewItems;
    private AppleMusicService service;
    private List<Result> results;
    private AdapterListMusica adapter;

    private ExoPlayer player;

    private LinearLayout miniPlayerLayout;
    private ImageView imgMiniArt;
    private TextView tvMiniTitle, tvMiniArtist, tvMiniTime;
    private ImageButton btnMiniPlayPause, btnMiniClose;
    private SeekBar seekBarMini;
    private ProgressBar progressBarSearch;

    private int currentPlayingIndex = -1;
    private int currentDownloadingIndex = -1;
    private Handler seekHandler;
    private Runnable seekRunnable;

    private static final int REQUEST_CODE = 200;
    private static final String PREFS_NAME = "DescargasPrefs";
    private static final String KEY_DESCARGAS = "lista_descargas";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_musica_online);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        initPlayer();
        initEvents();
        verificarPermisos();

        service = new AppleMusicService(this);
        results = new ArrayList<>();
        seekHandler = new Handler(Looper.getMainLooper());

        adapter = new AdapterListMusica(this, results, song -> confirmarEliminacion(song));
        listViewItems.setAdapter(adapter);

        cargarCancionesDescargadas();
    }

    private void cargarCancionesDescargadas() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_DESCARGAS, null);
        results.clear();
        if (json != null) {
            Gson gson = new Gson();
            List<Result> descargas = gson.fromJson(json, new TypeToken<List<Result>>(){}.getType());
            for (Result r : descargas) {
                File file = new File(getCacheDir() + "/" + r.getTrackId() + ".m4a");
                if (file.exists()) {
                    r.setState(2);
                    results.add(r);
                }
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void confirmarEliminacion(Result song) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar descarga")
                .setMessage("¿Estás seguro de que quieres eliminar \"" + song.getTrackName() + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarCancion(song))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarCancion(Result song) {
        File file = new File(getCacheDir() + "/" + song.getTrackId() + ".m4a");
        if (file.exists() && file.delete()) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            Gson gson = new Gson();
            String json = prefs.getString(KEY_DESCARGAS, null);
            if (json != null) {
                List<Result> descargas = gson.fromJson(json, new TypeToken<List<Result>>(){}.getType());
                descargas.removeIf(r -> r.getTrackId() == song.getTrackId());
                prefs.edit().putString(KEY_DESCARGAS, gson.toJson(descargas)).apply();
            }
            
            if (txtSearch.getText().toString().trim().isEmpty()) {
                cargarCancionesDescargadas();
            } else {
                song.setState(1); // Cambiar a "listo para descargar" en la búsqueda actual
                notifyAdapter();
            }
            Toasty.success(this, "Descarga eliminada", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarDescargaEnPrefs(Result song) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(KEY_DESCARGAS, null);
        List<Result> descargas = json == null ? new ArrayList<>() : gson.fromJson(json, new TypeToken<List<Result>>(){}.getType());
        
        if (descargas.stream().noneMatch(r -> r.getTrackId() == song.getTrackId())) {
            descargas.add(song);
            prefs.edit().putString(KEY_DESCARGAS, gson.toJson(descargas)).apply();
        }
    }

    private void initViews() {
        txtSearch = findViewById(R.id.txtBuscar);
        listViewItems = findViewById(R.id.listViewItems);
        progressBarSearch = findViewById(R.id.progressBarSearch);
        miniPlayerLayout = findViewById(R.id.miniPlayerLayout);
        imgMiniArt = findViewById(R.id.imgMiniArt);
        tvMiniTitle = findViewById(R.id.tvMiniTitle);
        tvMiniArtist = findViewById(R.id.tvMiniArtist);
        tvMiniTime = findViewById(R.id.tvMiniTime);
        btnMiniPlayPause = findViewById(R.id.btnMiniPlayPause);
        btnMiniClose = findViewById(R.id.btnMiniClose);
        seekBarMini = findViewById(R.id.seekBarMini);
    }

    private void initPlayer() {
        player = new ExoPlayer.Builder(this).build();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) onTrackFinished();
                else if (playbackState == Player.STATE_READY) {
                    if (player.getDuration() > 0) seekBarMini.setMax((int) player.getDuration());
                    startSeekBarUpdate();
                }
            }
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                btnMiniPlayPause.setImageResource(isPlaying ? R.drawable.baseline_pause_circle_outline_24 : R.drawable.baseline_play_circle_outline_24);
                if (isPlaying) startSeekBarUpdate(); else stopSeekBarUpdate();
            }
        });
    }

    private void initEvents() {
        txtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { ejecutarBusqueda(); return true; }
            return false;
        });

        listViewItems.setOnItemClickListener((adapterView, view, i, l) -> {
            if (results == null || i >= results.size()) return;
            Result song = results.get(i);
            String destFilename = getCacheDir() + "/" + song.getTrackId() + ".m4a";
            switch (song.getState()) {
                case 1: if (currentDownloadingIndex < 0) descargarCancion(song, destFilename, i); break;
                case 2: reproducirCancion(song, destFilename, i); break;
                case 3: pausarReproduccion(); song.setState(4); notifyAdapter(); break;
                case 4: reanudarReproduccion(); song.setState(3); notifyAdapter(); break;
            }
        });

        btnMiniPlayPause.setOnClickListener(v -> {
            if (player == null) return;
            if (player.isPlaying()) {
                player.pause();
                if (currentPlayingIndex >= 0) { results.get(currentPlayingIndex).setState(4); notifyAdapter(); }
            } else {
                player.play();
                if (currentPlayingIndex >= 0) { results.get(currentPlayingIndex).setState(3); notifyAdapter(); }
            }
        });
        btnMiniClose.setOnClickListener(v -> { detenerReproduccion(); ocultarMiniPlayer(); });
        seekBarMini.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { if (fromUser && player != null) player.seekTo(progress); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { stopSeekBarUpdate(); }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { startSeekBarUpdate(); }
        });
    }

    public void buscarCancion(View view) { ejecutarBusqueda(); }

    private void ejecutarBusqueda() {
        String termino = txtSearch.getText().toString().trim();
        if (termino.isEmpty()) { cargarCancionesDescargadas(); ocultarTeclado(); return; }
        detenerReproduccion(); ocultarMiniPlayer(); ocultarTeclado();
        if (progressBarSearch != null) progressBarSearch.setVisibility(View.VISIBLE);

        service.searchSongsByTerm(termino, (isNetworkError, statusCode, root) -> {
            runOnUiThread(() -> {
                if (progressBarSearch != null) progressBarSearch.setVisibility(View.GONE);
                if (!isNetworkError && statusCode == 200 && root != null && root.getResults() != null) {
                    results.clear(); results.addAll(root.getResults());
                    for (Result r : results) {
                        r.setState(new File(getCacheDir() + "/" + r.getTrackId() + ".m4a").exists() ? 2 : 1);
                    }
                    if (adapter != null) adapter.notifyDataSetChanged();
                } else Toasty.error(this, "Error de conexión", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void descargarCancion(Result song, String destFilename, int position) {
        currentDownloadingIndex = position; song.setState(5); notifyAdapter();
        Toasty.info(this, "Descargando...", Toast.LENGTH_SHORT, false).show();
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(song.getPreviewUrl());
                connection = (HttpURLConnection) url.openConnection();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream input = new BufferedInputStream(url.openStream(), 8192);
                    FileOutputStream output = new FileOutputStream(destFilename);
                    byte[] buffer = new byte[4096]; int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                    output.flush(); output.close(); input.close();
                    runOnUiThread(() -> {
                        song.setState(2); currentDownloadingIndex = -1;
                        guardarDescargaEnPrefs(song); notifyAdapter();
                        Toasty.success(this, "Descargada", Toast.LENGTH_SHORT, false).show();
                    });
                }
            } catch (Exception e) {
                new File(destFilename).delete();
                runOnUiThread(() -> { song.setState(1); currentDownloadingIndex = -1; notifyAdapter(); });
            } finally { if (connection != null) connection.disconnect(); }
        }).start();
    }

    private void reproducirCancion(Result song, String filePath, int position) {
        if (currentPlayingIndex >= 0) {
            Result prev = results.get(currentPlayingIndex);
            if (prev.getState() == 3 || prev.getState() == 4) prev.setState(2);
        }
        player.stop(); player.clearMediaItems();
        player.setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(new File(filePath))));
        player.prepare(); player.play();
        currentPlayingIndex = position; song.setState(3); notifyAdapter();
        mostrarMiniPlayer(song);
    }

    private void pausarReproduccion() { if (player != null) player.pause(); }
    private void reanudarReproduccion() { if (player != null) player.play(); }
    private void detenerReproduccion() {
        if (player != null) { player.stop(); player.clearMediaItems(); }
        if (currentPlayingIndex >= 0) {
            Result song = results.get(currentPlayingIndex);
            if (song.getState() == 3 || song.getState() == 4) { song.setState(2); notifyAdapter(); }
        }
        currentPlayingIndex = -1; stopSeekBarUpdate();
    }

    private void onTrackFinished() {
        if (currentPlayingIndex >= 0) { results.get(currentPlayingIndex).setState(2); notifyAdapter(); }
        currentPlayingIndex = -1; ocultarMiniPlayer(); stopSeekBarUpdate();
    }

    private void mostrarMiniPlayer(Result song) {
        miniPlayerLayout.setVisibility(View.VISIBLE);
        tvMiniTitle.setText(song.getTrackName()); tvMiniArtist.setText(song.getArtistName());
        btnMiniPlayPause.setImageResource(R.drawable.baseline_pause_circle_outline_24);
        if (song.getArtworkUrl100() != null && !song.getArtworkUrl100().isEmpty()) {
            Picasso.get().load(song.getArtworkUrl100()).placeholder(R.drawable.baseline_music_note_24).into(imgMiniArt);
        } else imgMiniArt.setImageResource(R.drawable.baseline_music_note_24);
        seekBarMini.setProgress(0);
    }

    private void ocultarMiniPlayer() { miniPlayerLayout.setVisibility(View.GONE); stopSeekBarUpdate(); }

    private void startSeekBarUpdate() {
        stopSeekBarUpdate();
        seekRunnable = () -> {
            if (player != null && player.isPlaying()) {
                seekBarMini.setProgress((int) player.getCurrentPosition());
                tvMiniTime.setText(formatTime(player.getCurrentPosition()) + " / " + formatTime(player.getDuration()));
            }
            seekHandler.postDelayed(seekRunnable, 500);
        };
        seekHandler.post(seekRunnable);
    }

    private void stopSeekBarUpdate() { if (seekHandler != null && seekRunnable != null) seekHandler.removeCallbacks(seekRunnable); }

    private String formatTime(long millis) {
        return String.format(Locale.getDefault(), "%d:%02d", TimeUnit.MILLISECONDS.toMinutes(millis), 
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    private void notifyAdapter() { if (adapter != null) adapter.notifyDataSetChanged(); }
    private void ocultarTeclado() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

    private void verificarPermisos() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
            }
        }
    }

    @Override protected void onDestroy() { super.onDestroy(); stopSeekBarUpdate(); if (player != null) player.release(); }
}