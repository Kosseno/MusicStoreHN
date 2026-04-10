package uth.pmo1.musicstorehn;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import es.dmoral.toasty.Toasty;

public class FeedGrupoActivity extends AppCompatActivity {

    private String grupoId, grupoNombre;
    private RecyclerView rvFeed;
    private MultimediaAdapter adapter;
    private ArrayList<Multimedia> listaMultimedia;
    private FloatingActionButton fabSubir;
    private ExoPlayer player;
    private PlayerView playerView;
    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;
    private String userName = "Usuario";
    private String currentUserId = "";
    private boolean esCreador = false;
    private boolean esMiembro = false;

    private MaterialButton btnUnirseSalir, btnEliminar;
    private TextView tvMiembrosCount, tvContenidoCount, tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_grupo);

        grupoId = getIntent().getStringExtra("grupoId");
        grupoNombre = getIntent().getStringExtra("grupoNombre");

        if (grupoId == null || grupoId.isEmpty()) {
            Toasty.error(this, "Error: grupo no encontrado").show();
            finish();
            return;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(grupoNombre);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) currentUserId = user.getUid();
        dbRef = FirebaseDatabase.getInstance().getReference();

        initViews();
        initializePlayer();
        setupRecyclerView();

        obtenerNombreUsuario();
        verificarMiembroYCreador();
        cargarContenido();
        contarMiembros();
        
        // Escuchar novedades del grupo para notificar automáticamente
        escucharNovedadesGrupo();

        fabSubir.setOnClickListener(v -> {
            if (!esMiembro && !esCreador) {
                Toasty.info(this, "Únete al grupo para subir contenido").show();
                return;
            }
            validarPermisosYSubir();
        });
        btnUnirseSalir.setOnClickListener(v -> gestionarMembresia());
        btnEliminar.setOnClickListener(v -> confirmarEliminacion());
    }

    private void escucharNovedadesGrupo() {
        dbRef.child("Grupos").child(grupoId).child("ultimaNovedad").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String mensaje = snapshot.child("mensaje").getValue(String.class);
                    String autorId = snapshot.child("autorId").getValue(String.class);
                    
                    // Solo notificar si el mensaje es nuevo y no lo subí yo mismo
                    if (autorId != null && !autorId.equals(currentUserId)) {
                        mostrarNotificacionLocal("Novedad en " + grupoNombre, mensaje);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void mostrarNotificacionLocal(String titulo, String cuerpo) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "grupo_notifications";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Avisos de Grupo", NotificationManager.IMPORTANCE_HIGH);
            if (nm != null) nm.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.baseline_notifications_active_24)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (nm != null) nm.notify(new Random().nextInt(1000), builder.build());
    }

    private void initViews() {
        rvFeed = findViewById(R.id.rvFeed);
        fabSubir = findViewById(R.id.fabSubirContenido);
        playerView = findViewById(R.id.player_view);
        btnUnirseSalir = findViewById(R.id.btnUnirseSalir);
        btnEliminar = findViewById(R.id.btnEliminarGrupo);
        tvMiembrosCount = findViewById(R.id.tvMiembrosCount);
        tvContenidoCount = findViewById(R.id.tvContenidoCount);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvMiembrosCount.setOnClickListener(v -> mostrarListaMiembros());
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) playerView.setVisibility(View.GONE);
            }
        });
    }

    private void setupRecyclerView() {
        rvFeed.setLayoutManager(new LinearLayoutManager(this));
        listaMultimedia = new ArrayList<>();
        adapter = new MultimediaAdapter(this, listaMultimedia, new MultimediaAdapter.OnMultimediaClickListener() {
            @Override
            public void onPlayClick(Multimedia multimedia, int position) {
                if (player != null && multimedia.getUrl() != null) {
                    player.stop(); player.clearMediaItems();
                    player.setMediaItem(MediaItem.fromUri(multimedia.getUrl()));
                    player.prepare(); player.play();
                    playerView.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onDeleteClick(Multimedia m, int p) { confirmarEliminarContenido(m); }
        }, currentUserId, false);
        rvFeed.setAdapter(adapter);
    }

    private void obtenerNombreUsuario() {
        if (currentUserId.isEmpty()) return;
        dbRef.child("Usuarios").child(currentUserId).child("usuario")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) userName = String.valueOf(snapshot.getValue());
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void verificarMiembroYCreador() {
        if (currentUserId.isEmpty()) return;
        dbRef.child("Grupos").child(grupoId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                String creadorId = snapshot.child("creadorId").getValue(String.class);
                esCreador = currentUserId.equals(creadorId);
                if (esCreador) {
                    btnEliminar.setVisibility(View.VISIBLE); fabSubir.setVisibility(View.VISIBLE);
                    esMiembro = true; btnUnirseSalir.setVisibility(View.GONE);
                    adapter.setEsCreadorGrupo(true);
                } else {
                    btnEliminar.setVisibility(View.GONE);
                    dbRef.child("Grupos").child(grupoId).child("miembros").child(currentUserId)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    esMiembro = snapshot.exists();
                                    btnUnirseSalir.setText(esMiembro ? "Salir del Grupo" : "Unirse al Grupo");
                                    fabSubir.setVisibility(esMiembro ? View.VISIBLE : View.GONE);
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void gestionarMembresia() {
        if (currentUserId.isEmpty()) return;
        DatabaseReference miembroRef = dbRef.child("Grupos").child(grupoId).child("miembros").child(currentUserId);
        if (esMiembro) {
            new AlertDialog.Builder(this).setTitle("Salir").setMessage("¿Salir de " + grupoNombre + "?")
                    .setPositiveButton("Salir", (d, w) -> miembroRef.removeValue().addOnSuccessListener(unused -> {
                        Toasty.info(this, "Has salido").show();
                    })).setNegativeButton("Cancelar", null).show();
        } else {
            miembroRef.setValue(userName).addOnSuccessListener(unused -> {
                Toasty.success(this, "¡Te has unido!").show();
            });
        }
    }

    private void cargarContenido() {
        dbRef.child("Multimedia").orderByChild("grupoId").equalTo(grupoId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaMultimedia.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Multimedia m = ds.getValue(Multimedia.class);
                    if (m != null) listaMultimedia.add(m);
                }
                Collections.sort(listaMultimedia, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                adapter.notifyDataSetChanged();
                if (tvEmptyState != null) tvEmptyState.setVisibility(listaMultimedia.isEmpty() ? View.VISIBLE : View.GONE);
                if (tvContenidoCount != null) tvContenidoCount.setText(listaMultimedia.size() + " archivos");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void validarPermisosYSubir() {
        String[] p = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? 
            new String[]{Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO} : 
            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        Dexter.withActivity(this).withPermissions(p).withListener(new MultiplePermissionsListener() {
            @Override public void onPermissionsChecked(com.karumi.dexter.MultiplePermissionsReport r) { if (r.areAllPermissionsGranted()) elegirArchivo(); }
            @Override public void onPermissionRationaleShouldBeShown(List<PermissionRequest> p, PermissionToken t) { t.continuePermissionRequest(); }
        }).check();
    }

    private void elegirArchivo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"audio/*", "video/*"});
        startActivityForResult(intent, 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) subirArchivo(data.getData());
    }

    private void subirArchivo(Uri fileUri) {
        String fileName = obtenerNombreArchivo(fileUri);
        String type = getContentResolver().getType(fileUri);
        String finalType = (type != null && type.contains("video")) ? "video" : "audio";
        
        ProgressDialog pd = new ProgressDialog(this); 
        pd.setTitle("Subiendo..."); 
        pd.setCancelable(false); 
        pd.show();
        
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("Multimedia").child(grupoId).child(System.currentTimeMillis() + "_" + fileName);
        storageRef.putFile(fileUri).addOnCompleteListener(task -> {
            pd.dismiss();
            if (task.isSuccessful()) {
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    guardarEnBD(fileName, uri.toString(), finalType);
                    notificarAutomaticamente(fileName); // ✅ Notificación automática
                });
            } else Toasty.error(this, "Error al subir").show();
        });
    }

    private void notificarAutomaticamente(String nombreArchivo) {
        // Escribir novedad en el grupo para que los demás la detecten
        Map<String, Object> novedad = new HashMap<>();
        novedad.put("mensaje", userName + " subió: " + nombreArchivo);
        novedad.put("autorId", currentUserId);
        novedad.put("timestamp", System.currentTimeMillis());
        
        dbRef.child("Grupos").child(grupoId).child("ultimaNovedad").setValue(novedad);
    }

    private String obtenerNombreArchivo(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) result = cursor.getString(nameIndex);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result != null ? result : "archivo_" + System.currentTimeMillis();
    }

    private void guardarEnBD(String nombre, String url, String tipo) {
        String id = dbRef.child("Multimedia").push().getKey();
        if (id == null) return;
        Multimedia m = new Multimedia(id, nombre, url, tipo, currentUserId, userName, "grupo", grupoId, System.currentTimeMillis());
        dbRef.child("Multimedia").child(id).setValue(m).addOnCompleteListener(task -> {
            if (task.isSuccessful()) Toasty.success(this, "¡Compartido!").show();
        });
    }

    private void contarMiembros() {
        dbRef.child("Grupos").child(grupoId).child("miembros").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tvMiembrosCount.setText(snapshot.getChildrenCount() + " Miembros");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void mostrarListaMiembros() {
        dbRef.child("Grupos").child(grupoId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                StringBuilder sb = new StringBuilder("Admin: " + snapshot.child("creadorNombre").getValue() + "\n");
                for (DataSnapshot ds : snapshot.child("miembros").getChildren()) {
                    if (!ds.getKey().equals(snapshot.child("creadorId").getValue())) sb.append("\n• ").append(ds.getValue());
                }
                new AlertDialog.Builder(FeedGrupoActivity.this).setTitle("Miembros").setMessage(sb.toString()).setPositiveButton("OK", null).show();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void confirmarEliminarContenido(Multimedia multimedia) {
        new AlertDialog.Builder(this).setTitle("Eliminar").setMessage("¿Borrar archivo?").setPositiveButton("Sí", (d, w) -> {
            dbRef.child("Multimedia").child(multimedia.getId()).removeValue();
        }).setNegativeButton("No", null).show();
    }

    private void confirmarEliminacion() {
        new AlertDialog.Builder(this).setTitle("Eliminar Grupo").setMessage("¿Eliminar para siempre?").setPositiveButton("Sí", (d, w) -> {
            dbRef.child("Grupos").child(grupoId).removeValue().addOnSuccessListener(u -> finish());
        }).setNegativeButton("No", null).show();
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        if (esCreador) { menu.add(0, 1, 0, "Editar Grupo"); menu.add(0, 2, 0, "Ver Miembros"); }
        return true;
    }
    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 1) mostrarDialogoEditarGrupo();
        if (item.getItemId() == 2) mostrarListaMiembros();
        if (item.getItemId() == android.R.id.home) finish();
        return true;
    }

    private void mostrarDialogoEditarGrupo() { /* ... */ }
    @Override protected void onDestroy() { super.onDestroy(); if (player != null) player.release(); }
}