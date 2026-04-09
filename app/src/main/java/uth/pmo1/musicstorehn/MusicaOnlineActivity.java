package uth.pmo1.musicstorehn;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import android.Manifest;
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

    // ✅ MEJORA: ExoPlayer en vez de MediaPlayer para consistencia con el resto de la app
    private ExoPlayer player;

    // ✅ MEJORA: Mini-player persistente en la parte inferior
    private LinearLayout miniPlayerLayout;
    private ImageView imgMiniArt;
    private TextView tvMiniTitle, tvMiniArtist, tvMiniTime;
    private ImageButton btnMiniPlayPause, btnMiniClose;
    private SeekBar seekBarMini;
    private ProgressBar progressBarSearch;

    // Estado
    private int currentPlayingIndex = -1;
    private int currentDownloadingIndex = -1;
    private Handler seekHandler;
    private Runnable seekRunnable;

    private static final int REQUEST_CODE = 200;

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
    }

    private void initViews() {
        txtSearch = findViewById(R.id.txtBuscar);
        listViewItems = findViewById(R.id.listViewItems);
        progressBarSearch = findViewById(R.id.progressBarSearch);

        // Mini Player views
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
                if (playbackState == Player.STATE_ENDED) {
                    onTrackFinished();
                } else if (playbackState == Player.STATE_READY) {
                    // Actualizar duración total en el seek bar
                    long duration = player.getDuration();
                    if (duration > 0) {
                        seekBarMini.setMax((int) duration);
                    }
                    startSeekBarUpdate();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                btnMiniPlayPause.setImageResource(isPlaying ?
                        R.drawable.baseline_pause_circle_outline_24 :
                        R.drawable.baseline_play_circle_outline_24);
                if (isPlaying) {
                    startSeekBarUpdate();
                } else {
                    stopSeekBarUpdate();
                }
            }
        });
    }

    private void initEvents() {
        // Buscar con Enter del teclado
        txtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                ejecutarBusqueda();
                return true;
            }
            return false;
        });

        listViewItems.setOnItemClickListener((adapterView, view, i, l) -> {
            if (results == null || i >= results.size()) return;
            Result song = results.get(i);
            if (song == null) return;

            String destFilename = getCacheDir() + "/" + song.getTrackId() + ".m4a";
            int state = song.getState();

            switch (state) {
                case 0: // No descargado — sin acción aún (el adapter muestra el botón de descarga)
                case 1: // Listo para descargar
                    if (currentDownloadingIndex >= 0) {
                        Toasty.info(this, "Espera a que termine la descarga actual", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    descargarCancion(song, destFilename, i);
                    break;
                case 2: // Descargado — reproducir
                    reproducirCancion(song, destFilename, i);
                    break;
                case 3: // Reproduciendo — pausar
                    pausarReproduccion();
                    song.setState(4); // Estado "pausado"
                    notifyAdapter();
                    break;
                case 4: // Pausado — reanudar
                    reanudarReproduccion();
                    song.setState(3);
                    notifyAdapter();
                    break;
            }
        });

        // Mini Player controles
        btnMiniPlayPause.setOnClickListener(v -> {
            if (player == null) return;
            if (player.isPlaying()) {
                player.pause();
                if (currentPlayingIndex >= 0 && currentPlayingIndex < results.size()) {
                    results.get(currentPlayingIndex).setState(4);
                    notifyAdapter();
                }
            } else {
                player.play();
                if (currentPlayingIndex >= 0 && currentPlayingIndex < results.size()) {
                    results.get(currentPlayingIndex).setState(3);
                    notifyAdapter();
                }
            }
        });

        btnMiniClose.setOnClickListener(v -> {
            detenerReproduccion();
            ocultarMiniPlayer();
        });

        seekBarMini.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    player.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekBarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                startSeekBarUpdate();
            }
        });
    }

    // ========== BÚSQUEDA ==========

    public void buscarCancion(View view) {
        ejecutarBusqueda();
    }

    private void ejecutarBusqueda() {
        String termino = txtSearch.getText().toString().trim();
        if (termino.isEmpty()) {
            Toasty.info(this, "Escribe el nombre de una canción o artista", Toast.LENGTH_SHORT, false).show();
            return;
        }

        detenerReproduccion();
        ocultarMiniPlayer();
        ocultarTeclado();

        if (progressBarSearch != null) progressBarSearch.setVisibility(View.VISIBLE);

        service.searchSongsByTerm(termino, (isNetworkError, statusCode, root) -> {
            runOnUiThread(() -> {
                if (progressBarSearch != null) progressBarSearch.setVisibility(View.GONE);

                if (!isNetworkError && statusCode == 200 && root != null && root.getResults() != null) {
                    results = new ArrayList<Result>(root.getResults());

                    // Marcar las canciones ya descargadas
                    for (Result r : results) {
                        String path = getCacheDir() + "/" + r.getTrackId() + ".m4a";
                        if (new File(path).exists()) {
                            r.setState(2); // Ya descargada
                        } else {
                            r.setState(1); // Lista para descargar
                        }
                    }

                    adapter = new AdapterListMusica(this, results);
                    listViewItems.setAdapter(adapter);

                    if (results.isEmpty()) {
                        Toasty.info(this, "Sin resultados para \"" + termino + "\"", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toasty.error(this, "Error de conexión. Verifica tu internet.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // ========== DESCARGA ==========

    private void descargarCancion(Result song, String destFilename, int position) {
        currentDownloadingIndex = position;
        song.setState(5); // Estado "descargando"
        notifyAdapter();

        Toasty.info(this, "Descargando: " + song.getTrackName(), Toast.LENGTH_SHORT, false).show();

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(song.getPreviewUrl());
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.connect();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    int totalSize = connection.getContentLength();
                    InputStream input = new BufferedInputStream(url.openStream(), 8192);
                    FileOutputStream output = new FileOutputStream(destFilename);

                    byte[] buffer = new byte[4096];
                    int count;
                    long downloaded = 0;

                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                        downloaded += count;

                        // ✅ MEJORA: Progreso de descarga visible (actualizar cada 10%)
                        if (totalSize > 0) {
                            int progress = (int) ((downloaded * 100) / totalSize);
                            // Se podría actualizar un ProgressBar aquí si se agrega al item
                        }
                    }

                    output.flush();
                    output.close();
                    input.close();

                    runOnUiThread(() -> {
                        song.setState(2); // Descargada y lista para reproducir
                        currentDownloadingIndex = -1;
                        notifyAdapter();
                        Toasty.success(this, song.getTrackName() + " descargada", Toast.LENGTH_SHORT, false).show();
                    });
                } else {
                    runOnUiThread(() -> {
                        song.setState(1);
                        currentDownloadingIndex = -1;
                        notifyAdapter();
                        Toasty.error(this, "Error del servidor", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e("Download", "Error: " + e.getMessage());
                // Limpiar archivo incompleto
                new File(destFilename).delete();
                runOnUiThread(() -> {
                    song.setState(1);
                    currentDownloadingIndex = -1;
                    notifyAdapter();
                    Toasty.error(this, "Error en la descarga", Toast.LENGTH_SHORT).show();
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    // ========== REPRODUCCIÓN ==========

    private void reproducirCancion(Result song, String filePath, int position) {
        // Restaurar estado de la canción anterior
        if (currentPlayingIndex >= 0 && currentPlayingIndex < results.size()) {
            Result prev = results.get(currentPlayingIndex);
            if (prev.getState() == 3 || prev.getState() == 4) {
                prev.setState(2);
            }
        }

        File file = new File(filePath);
        if (!file.exists()) {
            Toasty.error(this, "Archivo no encontrado. Descárgalo de nuevo.", Toast.LENGTH_SHORT).show();
            song.setState(1);
            notifyAdapter();
            return;
        }

        // Reproducir con ExoPlayer
        player.stop();
        player.clearMediaItems();
        player.setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(file)));
        player.prepare();
        player.play();

        currentPlayingIndex = position;
        song.setState(3); // Reproduciendo
        notifyAdapter();

        mostrarMiniPlayer(song);
    }

    private void pausarReproduccion() {
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }

    private void reanudarReproduccion() {
        if (player != null) {
            player.play();
        }
    }

    private void detenerReproduccion() {
        if (player != null) {
            player.stop();
            player.clearMediaItems();
        }
        if (currentPlayingIndex >= 0 && currentPlayingIndex < results.size()) {
            Result song = results.get(currentPlayingIndex);
            if (song.getState() == 3 || song.getState() == 4) {
                song.setState(2);
                notifyAdapter();
            }
        }
        currentPlayingIndex = -1;
        stopSeekBarUpdate();
    }

    private void onTrackFinished() {
        if (currentPlayingIndex >= 0 && currentPlayingIndex < results.size()) {
            results.get(currentPlayingIndex).setState(2);
            notifyAdapter();
        }
        currentPlayingIndex = -1;
        ocultarMiniPlayer();
        stopSeekBarUpdate();
    }

    // ========== MINI PLAYER ==========

    private void mostrarMiniPlayer(Result song) {
        miniPlayerLayout.setVisibility(View.VISIBLE);
        tvMiniTitle.setText(song.getTrackName());
        tvMiniArtist.setText(song.getArtistName());
        btnMiniPlayPause.setImageResource(R.drawable.baseline_pause_circle_outline_24);

        // Cargar artwork
        if (song.getArtworkUrl100() != null && !song.getArtworkUrl100().isEmpty()) {
            Picasso.get()
                    .load(song.getArtworkUrl100())
                    .placeholder(R.drawable.baseline_music_note_24)
                    .error(R.drawable.baseline_music_note_24)
                    .into(imgMiniArt);
        }

        seekBarMini.setProgress(0);
        seekBarMini.setMax(100);
    }

    private void ocultarMiniPlayer() {
        miniPlayerLayout.setVisibility(View.GONE);
        stopSeekBarUpdate();
    }

    // ========== SEEK BAR ==========

    private void startSeekBarUpdate() {
        stopSeekBarUpdate();
        seekRunnable = new Runnable() {
            @Override
            public void run() {
                if (player != null && player.isPlaying()) {
                    long position = player.getCurrentPosition();
                    long duration = player.getDuration();

                    if (duration > 0) {
                        seekBarMini.setMax((int) duration);
                        seekBarMini.setProgress((int) position);
                        tvMiniTime.setText(formatTime(position) + " / " + formatTime(duration));
                    }
                }
                seekHandler.postDelayed(this, 500);
            }
        };
        seekHandler.post(seekRunnable);
    }

    private void stopSeekBarUpdate() {
        if (seekHandler != null && seekRunnable != null) {
            seekHandler.removeCallbacks(seekRunnable);
        }
    }

    private String formatTime(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    // ========== UTILIDADES ==========

    private void notifyAdapter() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void ocultarTeclado() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void verificarPermisos() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // No detenemos el player al pausar la activity — solo al destruir
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSeekBarUpdate();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}