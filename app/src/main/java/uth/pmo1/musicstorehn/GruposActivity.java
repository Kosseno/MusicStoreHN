package uth.pmo1.musicstorehn;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import es.dmoral.toasty.Toasty;

public class GruposActivity extends AppCompatActivity {

    private RecyclerView rvGrupos;
    private GruposAdapter adapter;
    private ArrayList<Grupo> listaGruposTodos;   // Lista completa
    private ArrayList<Grupo> listaGruposFiltrada; // Lista visible (filtrada)
    private FloatingActionButton fabCrear;
    private DatabaseReference dbRef;
    private FirebaseAuth mAuth;
    private String userName = "Usuario";
    private String currentUserId = "";

    private ChipGroup chipGroupFiltros;
    private Chip chipTodos, chipMisGrupos, chipMiembro;
    private EditText etBuscarGrupo;
    private TextView tvEmptyGrupos;
    private String filtroActual = "todos"; // "todos", "creados", "miembro"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grupos);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) currentUserId = user.getUid();
        dbRef = FirebaseDatabase.getInstance().getReference();

        initViews();
        setupRecyclerView();
        setupFiltros();
        setupBusqueda();

        obtenerNombreUsuario();
        cargarGrupos();

        fabCrear.setOnClickListener(v -> mostrarDialogoCrear());
    }

    private void initViews() {
        rvGrupos = findViewById(R.id.rvGrupos);
        fabCrear = findViewById(R.id.fabCrearGrupo);
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros);
        chipTodos = findViewById(R.id.chipTodos);
        chipMisGrupos = findViewById(R.id.chipMisGrupos);
        chipMiembro = findViewById(R.id.chipMiembro);
        etBuscarGrupo = findViewById(R.id.etBuscarGrupo);
        tvEmptyGrupos = findViewById(R.id.tvEmptyGrupos);
    }

    private void setupRecyclerView() {
        rvGrupos.setLayoutManager(new LinearLayoutManager(this));
        listaGruposTodos = new ArrayList<>();
        listaGruposFiltrada = new ArrayList<>();
        adapter = new GruposAdapter(this, listaGruposFiltrada, grupo -> {
            Intent intent = new Intent(GruposActivity.this, FeedGrupoActivity.class);
            intent.putExtra("grupoId", grupo.getId());
            intent.putExtra("grupoNombre", grupo.getNombre());
            startActivity(intent);
        });
        rvGrupos.setAdapter(adapter);
    }

    private void setupFiltros() {
        if (chipTodos != null) {
            chipTodos.setOnClickListener(v -> {
                filtroActual = "todos";
                aplicarFiltros();
            });
        }
        if (chipMisGrupos != null) {
            chipMisGrupos.setOnClickListener(v -> {
                filtroActual = "creados";
                aplicarFiltros();
            });
        }
        if (chipMiembro != null) {
            chipMiembro.setOnClickListener(v -> {
                filtroActual = "miembro";
                aplicarFiltros();
            });
        }
    }

    private void setupBusqueda() {
        if (etBuscarGrupo == null) return;

        etBuscarGrupo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                aplicarFiltros();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void aplicarFiltros() {
        String busqueda = (etBuscarGrupo != null) ? etBuscarGrupo.getText().toString().trim().toLowerCase() : "";

        listaGruposFiltrada.clear();

        for (Grupo grupo : listaGruposTodos) {
            boolean pasaFiltro = false;
            boolean soyMiembro = esMiembroDe(grupo.getId());
            boolean soyCreador = currentUserId.equals(grupo.getCreadorId());

            // ✅ NUEVO: Lógica de Privacidad
            // Si el grupo es privado, SOLO se muestra si soy miembro o creador
            if (grupo.isEsPrivado() && !soyMiembro && !soyCreador) {
                continue; 
            }

            switch (filtroActual) {
                case "todos":
                    pasaFiltro = true;
                    break;
                case "creados":
                    pasaFiltro = soyCreador;
                    break;
                case "miembro":
                    pasaFiltro = soyMiembro;
                    break;
            }

            if (pasaFiltro && !busqueda.isEmpty()) {
                String nombre = grupo.getNombre() != null ? grupo.getNombre().toLowerCase() : "";
                String desc = grupo.getDescripcion() != null ? grupo.getDescripcion().toLowerCase() : "";
                pasaFiltro = nombre.contains(busqueda) || desc.contains(busqueda);
            }

            if (pasaFiltro) {
                listaGruposFiltrada.add(grupo);
            }
        }

        adapter.notifyDataSetChanged();

        if (tvEmptyGrupos != null) {
            if (listaGruposFiltrada.isEmpty()) {
                tvEmptyGrupos.setVisibility(View.VISIBLE);
                if (!busqueda.isEmpty()) {
                    tvEmptyGrupos.setText("No se encontraron grupos para \"" + busqueda + "\"");
                } else if ("creados".equals(filtroActual)) {
                    tvEmptyGrupos.setText("Aún no has creado ningún grupo.\n\nPresiona + para crear uno.");
                } else if ("miembro".equals(filtroActual)) {
                    tvEmptyGrupos.setText("No te has unido a ningún grupo aún.\n\nExplora los grupos disponibles.");
                } else {
                    tvEmptyGrupos.setText("No hay grupos públicos disponibles.\n\nSé el primero en crear uno.");
                }
            } else {
                tvEmptyGrupos.setVisibility(View.GONE);
            }
        }
    }

    private ArrayList<String> gruposDondeSoyMiembro = new ArrayList<>();

    private boolean esMiembroDe(String grupoId) {
        return gruposDondeSoyMiembro.contains(grupoId);
    }

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

    private void cargarGrupos() {
        dbRef.child("Grupos").orderByChild("creadoEn").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaGruposTodos.clear();
                gruposDondeSoyMiembro.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Grupo grupo = ds.getValue(Grupo.class);
                    if (grupo != null) {
                        listaGruposTodos.add(0, grupo); // Más recientes primero

                        if (currentUserId.equals(grupo.getCreadorId())) {
                            gruposDondeSoyMiembro.add(grupo.getId());
                        } else if (ds.child("miembros").hasChild(currentUserId)) {
                            gruposDondeSoyMiembro.add(grupo.getId());
                        }
                    }
                }

                aplicarFiltros();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toasty.error(GruposActivity.this, "Error al cargar grupos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoCrear() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_crear_grupo, null);
        EditText etNombre = view.findViewById(R.id.etNombreGrupo);
        EditText etDesc = view.findViewById(R.id.etDescripcionGrupo);
        SwitchMaterial swPrivado = view.findViewById(R.id.swPrivado);

        builder.setView(view)
                .setTitle("Crear Nuevo Grupo")
                .setPositiveButton("Crear", (dialog, which) -> {
                    String nombre = etNombre.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    boolean esPrivado = swPrivado.isChecked();

                    if (nombre.isEmpty() || nombre.length() < 3) {
                        Toasty.warning(this, "El nombre debe tener al menos 3 caracteres", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    crearGrupo(nombre, desc, esPrivado);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void crearGrupo(String nombre, String descripcion, boolean esPrivado) {
        String id = dbRef.child("Grupos").push().getKey();
        FirebaseUser user = mAuth.getCurrentUser();
        if (id == null || user == null) return;

        String uid = user.getUid();
        Grupo nuevoGrupo = new Grupo(id, nombre, descripcion, uid, userName, System.currentTimeMillis(), esPrivado);

        dbRef.child("Grupos").child(id).setValue(nuevoGrupo).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                dbRef.child("Grupos").child(id).child("miembros").child(uid).setValue(userName);
                Toasty.success(this, "Grupo \"" + nombre + "\" creado", Toast.LENGTH_SHORT).show();
            } else {
                Toasty.error(this, "No se pudo crear el grupo", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
