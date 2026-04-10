package uth.pmo1.musicstorehn;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private String userName = "Usuario", currentUserId = "";
    private boolean esCreador = false, esMiembro = false;

    private MaterialButton btnUnirseSalir, btnEliminar;
    private TextView tvMiembrosCount, tvContenidoCount, tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_grupo);

        grupoId = getIntent().getStringExtra("grupoId");
        grupoNombre = getIntent().getStringExtra("grupoNombre");

        if (grupoId == null) { finish(); return; }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(grupoNombre);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) currentUserId = mAuth.getCurrentUser().getUid();
        dbRef = FirebaseDatabase.getInstance().getReference();

        initViews();
        initializePlayer();
        setupRecyclerView();
        obtenerNombreUsuario();
        verificarMiembroYCreador();
        cargarContenido();
        contarMiembros();

        fabSubir.setOnClickListener(v -> {
            if (!esMiembro) { Toasty.info(this, "Únete para subir contenido").show(); return; }
            validarPermisosYSubir();
        });
        btnUnirseSalir.setOnClickListener(v -> gestionarMembresia());
        btnEliminar.setOnClickListener(v -> confirmarEliminacion());
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
    }

    private void setupRecyclerView() {
        rvFeed.setLayoutManager(new LinearLayoutManager(this));
        listaMultimedia = new ArrayList<>();
        adapter = new MultimediaAdapter(this, listaMultimedia, new MultimediaAdapter.OnMultimediaClickListener() {
            @Override
            public void onPlayClick(Multimedia m, int p) {
                if (player != null && m.getUrl() != null) {
                    player.stop(); player.clearMediaItems();
                    player.setMediaItem(MediaItem.fromUri(m.getUrl()));
                    player.prepare(); player.play();
                    playerView.setVisibility(View.VISIBLE);
                    if (m.getFotoUrl() == null || m.getFotoUrl().isEmpty()) generarMiniaturaAlVuelo(m);
                }
            }
            @Override public void onDeleteClick(Multimedia m, int p) { confirmarEliminarContenido(m); }
        }, currentUserId, false);
        rvFeed.setAdapter(adapter);
    }

    private void verificarMiembroYCreador() {
        dbRef.child("Grupos").child(grupoId).addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                if (!s.exists()) return;
                esCreador = currentUserId.equals(s.child("creadorId").getValue(String.class));
                
                if (esCreador) {
                    btnUnirseSalir.setVisibility(View.GONE);
                    btnEliminar.setVisibility(View.VISIBLE);
                    fabSubir.setVisibility(View.VISIBLE);
                    esMiembro = true;
                    adapter.setEsCreadorGrupo(true);
                    invalidateOptionsMenu();
                } else {
                    btnUnirseSalir.setVisibility(View.VISIBLE);
                    btnEliminar.setVisibility(View.GONE);
                    dbRef.child("Grupos").child(grupoId).child("miembros").child(currentUserId).addValueEventListener(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot s) {
                            esMiembro = s.exists();
                            btnUnirseSalir.setText(esMiembro ? "Salir del Grupo" : "Unirse al Grupo");
                            fabSubir.setVisibility(esMiembro ? View.VISIBLE : View.GONE);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError e) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void generarMiniaturaAlVuelo(Multimedia m) {
        new Thread(() -> {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(m.getUrl(), new HashMap<>());
                Bitmap bitmap;
                if ("video".equals(m.getTipo())) bitmap = retriever.getFrameAtTime(1000000);
                else {
                    byte[] art = retriever.getEmbeddedPicture();
                    bitmap = (art != null) ? android.graphics.BitmapFactory.decodeByteArray(art, 0, art.length) : null;
                }
                retriever.release();
                if (bitmap != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                    StorageReference ref = FirebaseStorage.getInstance().getReference().child("Multimedia").child(m.getGrupoId()).child(m.getId() + "_thumb.jpg");
                    ref.putBytes(baos.toByteArray()).addOnSuccessListener(t -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        FirebaseDatabase.getInstance().getReference("Multimedia").child(m.getId()).child("fotoUrl").setValue(uri.toString());
                    }));
                }
            } catch (Exception e) {}
        }).start();
    }

    private void subirArchivo(Uri fileUri) {
        String fileName = obtenerNombreArchivo(fileUri);
        String type = getContentResolver().getType(fileUri);
        String finalType = (type != null && type.contains("video")) ? "video" : "audio";
        ProgressDialog pd = new ProgressDialog(this); pd.setTitle("Subiendo..."); pd.show();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("Multimedia").child(grupoId).child(System.currentTimeMillis() + "_" + fileName);
        storageRef.putFile(fileUri).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String id = dbRef.child("Multimedia").push().getKey();
                    byte[] thumb = extraerMiniaturaLocal(fileUri, finalType);
                    if (thumb != null) {
                        StorageReference tRef = FirebaseStorage.getInstance().getReference().child("Multimedia").child(grupoId).child(id + "_thumb.jpg");
                        tRef.putBytes(thumb).addOnSuccessListener(st -> tRef.getDownloadUrl().addOnSuccessListener(tUri -> {
                            guardarEnBD(id, fileName, uri.toString(), finalType, tUri.toString()); pd.dismiss();
                        })).addOnFailureListener(e -> { guardarEnBD(id, fileName, uri.toString(), finalType, ""); pd.dismiss(); });
                    } else { guardarEnBD(id, fileName, uri.toString(), finalType, ""); pd.dismiss(); }
                });
            } else pd.dismiss();
        });
    }

    private byte[] extraerMiniaturaLocal(Uri uri, String tipo) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, uri);
            Bitmap b = tipo.equals("video") ? retriever.getFrameAtTime(1000000) : null;
            byte[] art = tipo.equals("audio") ? retriever.getEmbeddedPicture() : null;
            if (b != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                b.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                return baos.toByteArray();
            }
            return art;
        } catch (Exception e) { return null; }
    }

    private void guardarEnBD(String id, String nombre, String url, String tipo, String thumbUrl) {
        Multimedia m = new Multimedia(id, nombre, url, tipo, currentUserId, userName, "grupo", grupoId, thumbUrl, System.currentTimeMillis());
        dbRef.child("Multimedia").child(id).setValue(m).addOnSuccessListener(unused -> {
            Toasty.success(this, "¡Compartido!").show();
        });
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

    private void obtenerNombreUsuario() {
        if (currentUserId.isEmpty()) return;
        dbRef.child("Usuarios").child(currentUserId).child("usuario").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) { if (s.exists()) userName = String.valueOf(s.getValue()); }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void gestionarMembresia() {
        DatabaseReference mRef = dbRef.child("Grupos").child(grupoId).child("miembros").child(currentUserId);
        if (esMiembro) mRef.removeValue(); else mRef.setValue(userName);
    }

    private void cargarContenido() {
        dbRef.child("Multimedia").orderByChild("grupoId").equalTo(grupoId).addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                listaMultimedia.clear();
                for (DataSnapshot ds : s.getChildren()) {
                    Multimedia m = ds.getValue(Multimedia.class);
                    if (m != null) listaMultimedia.add(m);
                }
                Collections.sort(listaMultimedia, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                adapter.notifyDataSetChanged();
                tvEmptyState.setVisibility(listaMultimedia.isEmpty() ? View.VISIBLE : View.GONE);
                tvContenidoCount.setText(listaMultimedia.size() + " archivos");
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void validarPermisosYSubir() {
        String[] p = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? new String[]{android.Manifest.permission.READ_MEDIA_AUDIO, android.Manifest.permission.READ_MEDIA_VIDEO} : new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE};
        Dexter.withActivity(this).withPermissions(p).withListener(new MultiplePermissionsListener() {
            @Override public void onPermissionsChecked(com.karumi.dexter.MultiplePermissionsReport r) { if (r.areAllPermissionsGranted()) elegirArchivo(); }
            @Override public void onPermissionRationaleShouldBeShown(List<PermissionRequest> p, PermissionToken t) { t.continuePermissionRequest(); }
        }).check();
    }

    private void elegirArchivo() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT); i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"audio/*", "video/*"});
        startActivityForResult(i, 101);
    }

    @Override protected void onActivityResult(int rc, int res, Intent d) {
        super.onActivityResult(rc, res, d);
        if (rc == 101 && res == RESULT_OK && d != null) subirArchivo(d.getData());
    }

    private String obtenerNombreArchivo(Uri u) {
        try (Cursor c = getContentResolver().query(u, null, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME));
        } catch (Exception e) {}
        return "archivo_" + System.currentTimeMillis();
    }

    private void contarMiembros() {
        dbRef.child("Grupos").child(grupoId).child("miembros").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) { tvMiembrosCount.setText(s.getChildrenCount() + " Miembros"); }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void mostrarListaMiembros() {
        dbRef.child("Grupos").child(grupoId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                if (!s.exists()) return;
                StringBuilder sb = new StringBuilder("Admin: " + s.child("creadorNombre").getValue() + "\n");
                for (DataSnapshot ds : s.child("miembros").getChildren()) sb.append("\n• ").append(ds.getValue());
                new AlertDialog.Builder(FeedGrupoActivity.this).setTitle("Miembros").setMessage(sb.toString()).setPositiveButton("OK", null).show();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void confirmarEliminarContenido(Multimedia m) {
        new AlertDialog.Builder(this).setTitle("Eliminar").setMessage("¿Borrar?").setPositiveButton("Sí", (d, w) -> dbRef.child("Multimedia").child(m.getId()).removeValue()).show();
    }

    private void confirmarEliminacion() {
        new AlertDialog.Builder(this).setTitle("Eliminar Grupo").setMessage("¿Eliminar?").setPositiveButton("Sí", (d, w) -> dbRef.child("Grupos").child(grupoId).removeValue().addOnSuccessListener(u -> finish())).show();
    }

    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear(); if (esCreador) { menu.add(0, 1, 0, "Editar Grupo"); menu.add(0, 2, 0, "Ver Miembros"); }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 1) mostrarDialogoEditarGrupo();
        else if (item.getItemId() == 2) mostrarListaMiembros();
        else if (item.getItemId() == android.R.id.home) finish();
        return true;
    }

    private void mostrarDialogoEditarGrupo() {
        dbRef.child("Grupos").child(grupoId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Grupo grupo = snapshot.getValue(Grupo.class);
                if (grupo == null) return;
                View view = LayoutInflater.from(FeedGrupoActivity.this).inflate(R.layout.dialog_crear_grupo, null);
                EditText etNombre = view.findViewById(R.id.etNombreGrupo);
                EditText etDesc = view.findViewById(R.id.etDescripcionGrupo);
                SwitchMaterial swPrivado = view.findViewById(R.id.swPrivado);
                etNombre.setText(grupo.getNombre()); etDesc.setText(grupo.getDescripcion()); swPrivado.setChecked(grupo.isEsPrivado());
                new AlertDialog.Builder(FeedGrupoActivity.this).setView(view).setTitle("Editar").setPositiveButton("Guardar", (d, w) -> {
                    Map<String, Object> up = new HashMap<>(); up.put("nombre", etNombre.getText().toString()); up.put("descripcion", etDesc.getText().toString()); up.put("esPrivado", swPrivado.isChecked());
                    dbRef.child("Grupos").child(grupoId).updateChildren(up);
                }).show();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    @Override protected void onDestroy() { super.onDestroy(); if (player != null) player.release(); }
}
