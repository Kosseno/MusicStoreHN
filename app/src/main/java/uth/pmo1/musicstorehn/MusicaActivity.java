package uth.pmo1.musicstorehn;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MusicaActivity extends AppCompatActivity {

    private Uri uri;
    private String nombreCancion;
    private ListView listviewMusic;
    private final ArrayList<Cancion> arrayListCanciones = new ArrayList<>();
    private ArrayAdapter<Cancion> arrayAdapter;
    
    private ExoPlayer player;
    private PlayerView playerView;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_musica);

        listviewMusic = findViewById(R.id.listviewMusic);
        playerView = findViewById(R.id.player_view);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        initializePlayer();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        FirebaseMessaging.getInstance().subscribeToTopic("enviaratodos");

        recuperarCanciones();

        listviewMusic.setOnItemClickListener((adapterView, view, position, id) -> {
            if (player != null) {
                player.seekTo(position, 0);
                player.play();
                playerView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void confirmarEliminacion(Cancion cancion) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar canción")
                .setMessage("¿Deseas eliminar '" + cancion.getNombreCancion() + "' permanentemente?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarCancion(cancion))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarCancion(Cancion cancion) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Eliminando...");
        pd.show();

        if (cancion.getUrlCancion() != null && cancion.getUrlCancion().contains("firebase")) {
            try { FirebaseStorage.getInstance().getReferenceFromUrl(cancion.getUrlCancion()).delete(); } catch (Exception e) {}
        }
        if (cancion.getFotoUrl() != null && cancion.getFotoUrl().contains("firebase")) {
            try { FirebaseStorage.getInstance().getReferenceFromUrl(cancion.getFotoUrl()).delete(); } catch (Exception e) {}
        }

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Cancion");
        dbRef.orderByChild("urlCancion").equalTo(cancion.getUrlCancion())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) ds.getRef().removeValue();
                        pd.dismiss();
                        Toast.makeText(MusicaActivity.this, "Canción eliminada", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { pd.dismiss(); }
                });
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) player.release();
    }

    private void recuperarCanciones() {
        if (currentUserId == null) return;

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Cancion");
        databaseReference.orderByChild("userId").equalTo(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                arrayListCanciones.clear();
                List<MediaItem> mediaItems = new ArrayList<>();

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Cancion cancionObj = ds.getValue(Cancion.class);
                    if (cancionObj != null && cancionObj.getUrlCancion() != null) {
                        arrayListCanciones.add(cancionObj);
                        mediaItems.add(MediaItem.fromUri(cancionObj.getUrlCancion()));
                    }
                }

                if (player != null) {
                    player.setMediaItems(mediaItems);
                    player.prepare();
                }

                if (arrayAdapter == null) {
                    arrayAdapter = new ArrayAdapter<Cancion>(MusicaActivity.this, R.layout.lista_view_canciones, arrayListCanciones) {
                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                            if (convertView == null) convertView = LayoutInflater.from(getContext()).inflate(R.layout.lista_view_canciones, parent, false);
                            
                            Cancion cancion = getItem(position);
                            TextView tvNombre = convertView.findViewById(R.id.txtNombreCancion);
                            TextView tvArtista = convertView.findViewById(R.id.txtNombreArtista);
                            ImageView imgCaratula = convertView.findViewById(R.id.imgPhoto);
                            ImageView btnDelete = convertView.findViewById(R.id.imgDelete);
                            ImageView btnAction = convertView.findViewById(R.id.imgAction);
                            
                            tvNombre.setText(cancion.getNombreCancion());
                            tvArtista.setText("Subido por ti");
                            btnDelete.setVisibility(View.VISIBLE);
                            btnAction.setVisibility(View.GONE);

                            if (cancion.getFotoUrl() != null && !cancion.getFotoUrl().isEmpty()) {
                                Picasso.get().load(cancion.getFotoUrl()).placeholder(R.drawable.baseline_music_note_24).error(R.drawable.baseline_music_note_24).into(imgCaratula);
                            } else {
                                imgCaratula.setImageResource(R.drawable.baseline_music_note_24);
                            }

                            btnDelete.setOnClickListener(v -> confirmarEliminacion(cancion));
                            return convertView;
                        }
                    };
                    listviewMusic.setAdapter(arrayAdapter);
                } else arrayAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void elegirCancion() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uri = data.getData();
            obtenerNombreArchivo(uri);
            subirCancionFirebase();
        }
    }

    private void obtenerNombreArchivo(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                nombreCancion = cursor.getString(nameIndex);
            }
        } catch (Exception e) {}
        if (nombreCancion == null) nombreCancion = "Cancion_" + System.currentTimeMillis();
    }

    private void subirCancionFirebase() {
        if (uri == null || currentUserId == null) return;
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Procesando canción...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 1. Extraer miniatura/carátula del audio
        byte[] caratulaBytes = extraerCaratula(uri);

        StorageReference baseRef = FirebaseStorage.getInstance().getReference().child("Cancion").child(currentUserId);
        String fileName = System.currentTimeMillis() + "_" + nombreCancion;
        StorageReference songRef = baseRef.child(fileName);

        songRef.putFile(uri).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                songRef.getDownloadUrl().addOnSuccessListener(songUrl -> {
                    if (caratulaBytes != null) {
                        StorageReference artRef = baseRef.child(fileName + "_art.jpg");
                        artRef.putBytes(caratulaBytes).addOnSuccessListener(t -> {
                            artRef.getDownloadUrl().addOnSuccessListener(artUrl -> {
                                enviarDetallesDB(songUrl.toString(), artUrl.toString(), progressDialog);
                            });
                        }).addOnFailureListener(e -> enviarDetallesDB(songUrl.toString(), null, progressDialog));
                    } else {
                        enviarDetallesDB(songUrl.toString(), null, progressDialog);
                    }
                });
            } else {
                progressDialog.dismiss();
                Toast.makeText(this, "Error al subir", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private byte[] extraerCaratula(Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, uri);
            byte[] art = retriever.getEmbeddedPicture();
            retriever.release();
            return art;
        } catch (Exception e) { return null; }
    }

    private void enviarDetallesDB(String songUrl, String artUrl, ProgressDialog pd) {
        Cancion cancionObj = new Cancion(nombreCancion, songUrl, currentUserId, artUrl);
        FirebaseDatabase.getInstance().getReference("Cancion").push().setValue(cancionObj)
                .addOnCompleteListener(task -> {
                    pd.dismiss();
                    if (task.isSuccessful()) Toast.makeText(this, "¡Subida con éxito!", Toast.LENGTH_SHORT).show();
                });
    }

    private void validarPermisos() {
        String p = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;
        Dexter.withActivity(this).withPermission(p).withListener(new PermissionListener() {
            @Override public void onPermissionGranted(PermissionGrantedResponse r) { elegirCancion(); }
            @Override public void onPermissionDenied(PermissionDeniedResponse r) {}
            @Override public void onPermissionRationaleShouldBeShown(PermissionRequest p, PermissionToken t) { t.continuePermissionRequest(); }
        }).check();
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) { getMenuInflater().inflate(R.menu.menu_musica, menu); return true; }
    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.subir_archivo) { validarPermisos(); return true; }
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}