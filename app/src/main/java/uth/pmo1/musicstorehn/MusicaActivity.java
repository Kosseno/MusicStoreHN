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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

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

        listviewMusic.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmarEliminacion(arrayListCanciones.get(position));
            return true;
        });
    }

    private void confirmarEliminacion(Cancion cancion) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar canción")
                .setMessage("¿Deseas eliminar '" + cancion.getNombreCancion() + "' de tu biblioteca?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarCancion(cancion))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarCancion(Cancion cancion) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Cancion");
        dbRef.orderByChild("urlCancion").equalTo(cancion.getUrlCancion())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            ds.getRef().removeValue().addOnSuccessListener(unused -> {
                                Toast.makeText(MusicaActivity.this, "Canción eliminada", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
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
                    arrayAdapter = new ArrayAdapter<Cancion>(MusicaActivity.this, android.R.layout.simple_list_item_1, arrayListCanciones) {
                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
                            TextView textView = view.findViewById(android.R.id.text1);
                            textView.setSingleLine(true);
                            textView.setMaxLines(1);
                            textView.setText(getItem(position).getNombreCancion());
                            textView.setTextColor(getResources().getColor(R.color.white));
                            return view;
                        }
                    };
                    listviewMusic.setAdapter(arrayAdapter);
                } else {
                    arrayAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MusicaActivity.this, "Error al cargar canciones: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
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
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    nombreCancion = cursor.getString(nameIndex);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (nombreCancion == null) nombreCancion = "Cancion_" + System.currentTimeMillis();
            subirCancionFirebase();
        }
    }

    private void subirCancionFirebase() {
        if (uri == null || currentUserId == null) return;

        StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child("Cancion")
                .child(currentUserId)
                .child(System.currentTimeMillis() + "_" + nombreCancion);

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Subiendo canción");
        progressDialog.setCancelable(false);
        progressDialog.show();

        storageReference.putFile(uri)
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressDialog.setMessage("Progreso: " + (int) progress + "%");
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return storageReference.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        enviarDetallesDB(downloadUri.toString());
                    } else {
                        Toast.makeText(MusicaActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void enviarDetallesDB(String url) {
        if (currentUserId == null) return;
        Cancion cancionObj = new Cancion(nombreCancion, url, currentUserId);
        FirebaseDatabase.getInstance().getReference("Cancion").push().setValue(cancionObj)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MusicaActivity.this, "¡Canción subida con éxito!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MusicaActivity.this, "Error al guardar en BD", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void validarPermisos() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? 
                Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;

        Dexter.withActivity(this)
                .withPermission(permission)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        elegirCancion();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MusicaActivity.this, "Permiso denegado", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_musica, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.subir_archivo) {
            validarPermisos();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
