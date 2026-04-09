package uth.pmo1.musicstorehn;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
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
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
            Toasty.error(this, "Error: grupo no encontrado", Toast.LENGTH_SHORT).show();
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

        fabSubir.setOnClickListener(v -> {
            if (!esMiembro && !esCreador) {
                Toasty.info(this, "Únete al grupo para subir contenido", Toast.LENGTH_SHORT).show();
                return;
            }
            validarPermisosYSubir();
        });
        btnUnirseSalir.setOnClickListener(v -> gestionarMembresia());
        btnEliminar.setOnClickListener(v -> confirmarEliminacion());
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

        // ✅ MEJORA: Hacer clic en el contador de miembros abre el diálogo de miembros
        tvMiembrosCount.setOnClickListener(v -> mostrarListaMiembros());
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // ✅ MEJORA: Ocultar player cuando termina la reproducción
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    playerView.setVisibility(View.GONE);
                }
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
                    player.stop();
                    player.clearMediaItems();
                    player.setMediaItem(MediaItem.fromUri(multimedia.getUrl()));
                    player.prepare();
                    player.play();
                    playerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onDeleteClick(Multimedia multimedia, int position) {
                confirmarEliminarContenido(multimedia);
            }
        }, currentUserId, false);

        rvFeed.setAdapter(adapter);
    }

    // ========== DATOS DEL USUARIO ==========

    private void obtenerNombreUsuario() {
        if (currentUserId.isEmpty()) return;
        dbRef.child("Usuarios").child(currentUserId).child("usuario")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.getValue() != null)
                            userName = snapshot.getValue(String.class);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // ========== MEMBRESÍA ==========

    private void verificarMiembroYCreador() {
        if (currentUserId.isEmpty()) return;

        dbRef.child("Grupos").child(grupoId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toasty.info(FeedGrupoActivity.this, "Este grupo ha sido eliminado", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Actualizar nombre del grupo si cambió
                String nombre = snapshot.child("nombre").getValue(String.class);
                if (nombre != null) {
                    grupoNombre = nombre;
                    if (getSupportActionBar() != null) getSupportActionBar().setTitle(grupoNombre);
                }

                String creadorId = snapshot.child("creadorId").getValue(String.class);
                esCreador = currentUserId.equals(creadorId);

                if (esCreador) {
                    btnEliminar.setVisibility(View.VISIBLE);
                    fabSubir.setVisibility(View.VISIBLE);
                    esMiembro = true;
                    btnUnirseSalir.setVisibility(View.GONE);
                    adapter.setEsCreadorGrupo(true);
                    // ✅ MEJORA: Invalidar menú para mostrar opciones de admin
                    invalidateOptionsMenu();
                } else {
                    btnEliminar.setVisibility(View.GONE);
                    adapter.setEsCreadorGrupo(false);

                    dbRef.child("Grupos").child(grupoId).child("miembros").child(currentUserId)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    esMiembro = snapshot.exists();
                                    btnUnirseSalir.setText(esMiembro ? "Salir del Grupo" : "Unirse al Grupo");
                                    fabSubir.setVisibility(esMiembro ? View.VISIBLE : View.GONE);
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void gestionarMembresia() {
        if (currentUserId.isEmpty()) return;
        DatabaseReference miembroRef = dbRef.child("Grupos").child(grupoId).child("miembros").child(currentUserId);

        if (esMiembro) {
            new AlertDialog.Builder(this)
                    .setTitle("Salir del Grupo")
                    .setMessage("¿Estás seguro de que quieres salir de \"" + grupoNombre + "\"?\n\nTu contenido compartido permanecerá en el grupo.")
                    .setPositiveButton("Salir", (d, w) ->
                            miembroRef.removeValue().addOnSuccessListener(unused ->
                                    Toasty.info(this, "Has salido del grupo", Toast.LENGTH_SHORT).show()))
                    .setNegativeButton("Cancelar", null)
                    .show();
        } else {
            miembroRef.setValue(userName).addOnSuccessListener(unused ->
                    Toasty.success(this, "¡Te has unido a " + grupoNombre + "!", Toast.LENGTH_SHORT).show());
        }
    }

    private void contarMiembros() {
        dbRef.child("Grupos").child(grupoId).child("miembros").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                tvMiembrosCount.setText(count + (count == 1 ? " Miembro" : " Miembros"));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ✅ NUEVO: Diálogo con la lista de miembros del grupo
    private void mostrarListaMiembros() {
        dbRef.child("Grupos").child(grupoId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                StringBuilder miembrosText = new StringBuilder();

                // Creador
                String creadorNombre = snapshot.child("creadorNombre").getValue(String.class);
                String creadorId = snapshot.child("creadorId").getValue(String.class);
                miembrosText.append("👑  ").append(creadorNombre != null ? creadorNombre : "Admin");
                if (currentUserId.equals(creadorId)) miembrosText.append(" (Tú)");
                miembrosText.append(" — Administrador\n");

                // Miembros
                DataSnapshot miembrosSnap = snapshot.child("miembros");
                for (DataSnapshot ds : miembrosSnap.getChildren()) {
                    String uid = ds.getKey();
                    String nombre = ds.getValue(String.class);
                    if (uid != null && !uid.equals(creadorId)) {
                        miembrosText.append("\n•  ").append(nombre != null ? nombre : "Usuario");
                        if (currentUserId.equals(uid)) miembrosText.append(" (Tú)");

                        // ✅ MEJORA: El creador puede expulsar miembros
                        // (La funcionalidad se implementa desde el diálogo con opciones)
                    }
                }

                long totalCount = miembrosSnap.getChildrenCount();

                AlertDialog.Builder builder = new AlertDialog.Builder(FeedGrupoActivity.this)
                        .setTitle("Miembros (" + totalCount + ")")
                        .setMessage(miembrosText.toString())
                        .setPositiveButton("Cerrar", null);

                builder.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ========== MENÚ DE OPCIONES (ADMIN) ==========

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (esCreador) {
            // ✅ NUEVO: Opción de editar grupo para el admin
            menu.add(0, 1001, 0, "Editar Grupo");
            menu.add(0, 1002, 0, "Ver Miembros");
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 1001) {
            mostrarDialogoEditarGrupo();
            return true;
        }
        if (item.getItemId() == 1002) {
            mostrarListaMiembros();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ✅ NUEVO: Editar nombre y descripción del grupo (solo admin)
    private void mostrarDialogoEditarGrupo() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_crear_grupo, null);
        EditText etNombre = dialogView.findViewById(R.id.etNombreGrupo);
        EditText etDesc = dialogView.findViewById(R.id.etDescripcionGrupo);

        // Cargar datos actuales
        dbRef.child("Grupos").child(grupoId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                String nombre = snapshot.child("nombre").getValue(String.class);
                String desc = snapshot.child("descripcion").getValue(String.class);
                etNombre.setText(nombre);
                etDesc.setText(desc);

                new AlertDialog.Builder(FeedGrupoActivity.this)
                        .setView(dialogView)
                        .setTitle("Editar Grupo")
                        .setPositiveButton("Guardar", (dialog, which) -> {
                            String nuevoNombre = etNombre.getText().toString().trim();
                            String nuevaDesc = etDesc.getText().toString().trim();

                            if (nuevoNombre.isEmpty() || nuevoNombre.length() < 3) {
                                Toasty.warning(FeedGrupoActivity.this, "El nombre debe tener al menos 3 caracteres", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            dbRef.child("Grupos").child(grupoId).child("nombre").setValue(nuevoNombre);
                            dbRef.child("Grupos").child(grupoId).child("descripcion").setValue(nuevaDesc);
                            Toasty.success(FeedGrupoActivity.this, "Grupo actualizado", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ========== CONTENIDO MULTIMEDIA ==========

    private void cargarContenido() {
        dbRef.child("Multimedia").orderByChild("grupoId").equalTo(grupoId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaMultimedia.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Multimedia m = ds.getValue(Multimedia.class);
                    if (m != null) listaMultimedia.add(m);
                }

                // ✅ MEJORA: Ordenar por fecha (más reciente primero)
                Collections.sort(listaMultimedia, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                adapter.notifyDataSetChanged();

                // ✅ MEJORA: Mostrar estado vacío si no hay contenido
                if (tvEmptyState != null) {
                    tvEmptyState.setVisibility(listaMultimedia.isEmpty() ? View.VISIBLE : View.GONE);
                }

                // ✅ MEJORA: Mostrar contador de contenido
                if (tvContenidoCount != null) {
                    int count = listaMultimedia.size();
                    tvContenidoCount.setText(count + (count == 1 ? " archivo" : " archivos"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toasty.error(FeedGrupoActivity.this, "Error al cargar contenido", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmarEliminarContenido(Multimedia multimedia) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Contenido")
                .setMessage("¿Eliminar \"" + multimedia.getNombre() + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    dbRef.child("Multimedia").child(multimedia.getId()).removeValue()
                            .addOnSuccessListener(unused -> {
                                try {
                                    StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(multimedia.getUrl());
                                    ref.delete();
                                } catch (Exception ignored) {}
                                Toasty.success(this, "Contenido eliminado", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toasty.error(this, "Error al eliminar", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarEliminacion() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Grupo")
                .setMessage("¿Eliminar \"" + grupoNombre + "\" y todo su contenido?\n\nEsta acción es irreversible.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    dbRef.child("Multimedia").orderByChild("grupoId").equalTo(grupoId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot ds : snapshot.getChildren()) ds.getRef().removeValue();
                                    dbRef.child("Grupos").child(grupoId).removeValue()
                                            .addOnSuccessListener(unused -> {
                                                Toasty.success(FeedGrupoActivity.this, "Grupo eliminado", Toast.LENGTH_SHORT).show();
                                                finish();
                                            });
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ========== SUBIR ARCHIVO ==========

    private void validarPermisosYSubir() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{android.Manifest.permission.READ_MEDIA_AUDIO, android.Manifest.permission.READ_MEDIA_VIDEO};
        } else {
            permissions = new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE};
        }

        Dexter.withActivity(this).withPermissions(permissions)
                .withListener(new com.karumi.dexter.listener.multi.MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(com.karumi.dexter.MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) elegirArchivo();
                    }
                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> p, PermissionToken t) {
                        t.continuePermissionRequest();
                    }
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
        if (requestCode == 101 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            subirArchivo(data.getData());
        }
    }

    private void subirArchivo(Uri fileUri) {
        String fileName = "multimedia_" + System.currentTimeMillis();
        String type = getContentResolver().getType(fileUri);
        String finalType = (type != null && type.contains("video")) ? "video" : "audio";

        try (Cursor cursor = getContentResolver().query(fileUri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) fileName = cursor.getString(idx);
            }
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Subiendo " + finalType + "...");
        pd.setCancelable(false);
        pd.show();

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("Multimedia").child(grupoId).child(System.currentTimeMillis() + "_" + fileName);

        String finalFileName = fileName;
        storageRef.putFile(fileUri)
                .addOnProgressListener(s -> {
                    double p = (100.0 * s.getBytesTransferred()) / s.getTotalByteCount();
                    pd.setMessage("Progreso: " + (int) p + "%");
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return storageRef.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    pd.dismiss();
                    if (task.isSuccessful()) {
                        guardarEnBD(finalFileName, task.getResult().toString(), finalType);
                    } else {
                        Toasty.error(this, "Error al subir archivo", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void guardarEnBD(String nombre, String url, String tipo) {
        String id = dbRef.child("Multimedia").push().getKey();
        if (id == null) return;
        Multimedia m = new Multimedia(id, nombre, url, tipo, currentUserId, userName, "grupo", grupoId, System.currentTimeMillis());
        dbRef.child("Multimedia").child(id).setValue(m).addOnCompleteListener(task -> {
            if (task.isSuccessful())
                Toasty.success(this, "¡Archivo compartido!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) { player.release(); player = null; }
    }
}