package uth.pmo1.musicstorehn;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.File;

import es.dmoral.toasty.Toasty;

public class UsuarioActivity extends AppCompatActivity {
    TextView tusuario, tcorreo, tcarrera, tdescripcion;
    ImageView fotografia;
    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;
    DatabaseReference databaseReference;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usuario);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tusuario = findViewById(R.id.tUsuario);
        tcorreo = findViewById(R.id.tCorreo);
        tcarrera = findViewById(R.id.tCarrera);
        tdescripcion = findViewById(R.id.tDescripcion);
        fotografia = findViewById(R.id.imgFotografia);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("Usuarios");
        progressDialog = new ProgressDialog(this);

        if (firebaseUser != null) {
            databaseReference.child(firebaseUser.getUid()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String correo = String.valueOf(snapshot.child("correo").getValue());
                        String usuario = String.valueOf(snapshot.child("usuario").getValue());
                        String foto = snapshot.hasChild("fotoUrl") ? String.valueOf(snapshot.child("fotoUrl").getValue()) : "";
                        String carrera = snapshot.hasChild("carrera") ? String.valueOf(snapshot.child("carrera").getValue()) : "";
                        String descripcion = snapshot.hasChild("descripcion") ? String.valueOf(snapshot.child("descripcion").getValue()) : "";

                        tusuario.setText(usuario);
                        tcorreo.setText(correo);
                        tcarrera.setText((carrera == null || carrera.equals("null")) ? "Añade tu carrera" : carrera);
                        tdescripcion.setText((descripcion == null || descripcion.equals("null")) ? "Cuéntanos algo sobre ti..." : descripcion);

                        if (foto != null && !foto.trim().isEmpty() && !foto.equals("null")) {
                            Picasso.get().load(foto).placeholder(R.drawable.usuario).error(R.drawable.usuario).into(fotografia);
                        } else {
                            fotografia.setImageResource(R.drawable.usuario);
                        }
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_usuario, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.editar) {
            startActivity(new Intent(this, EditarUsuarioActivity.class));
            finish();
            return true;
        }
        if (item.getItemId() == R.id.eliminar) {
            confirmarEliminacion();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmarEliminacion() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Cuenta")
                .setMessage("¿Estás seguro de que deseas eliminar tu cuenta? Esta acción borrará tu acceso, tu perfil, tus grupos creados y tus descargas locales.")
                .setPositiveButton("ELIMINAR", (dialog, which) -> {
                    if (firebaseUser != null) {
                        eliminarTodo(firebaseUser.getUid());
                    }
                })
                .setNegativeButton("CANCELAR", null)
                .show();
    }

    private void eliminarTodo(String uid) {
        progressDialog.setMessage("Eliminando cuenta, perfil y grupos...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 1. Borrar archivos locales (.m4a descargados)
        try {
            File[] files = getCacheDir().listFiles();
            if (files != null) {
                for (File f : files) if (f.getName().endsWith(".m4a")) f.delete();
            }
            getSharedPreferences("DescargasPrefs", MODE_PRIVATE).edit().clear().apply();
        } catch (Exception e) {
            Log.e("DELETE_LOCAL", "Error: " + e.getMessage());
        }

        // 2. Borrar Grupos donde el usuario es creador
        FirebaseDatabase.getInstance().getReference("Grupos").orderByChild("creadorId").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            ds.getRef().removeValue();
                        }
                        
                        // 3. Borrar el perfil del usuario de Realtime Database
                        databaseReference.child(uid).removeValue().addOnCompleteListener(taskBD -> {
                            
                            // 4. Eliminar de Firebase Authentication
                            if (firebaseUser != null) {
                                firebaseUser.delete().addOnCompleteListener(taskAuth -> {
                                    progressDialog.dismiss();
                                    if (taskAuth.isSuccessful()) {
                                        Toasty.success(UsuarioActivity.this, "Cuenta y grupos eliminados correctamente", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toasty.warning(UsuarioActivity.this, "Sesión expirada. Datos borrados, pero debes re-autenticarte para borrar el acceso final.", Toast.LENGTH_LONG).show();
                                        firebaseAuth.signOut();
                                    }
                                    irALogin();
                                });
                            } else {
                                progressDialog.dismiss();
                                irALogin();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressDialog.dismiss();
                        Toasty.error(UsuarioActivity.this, "Error al eliminar grupos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void irALogin() {
        Intent intent = new Intent(this, IniciarSesionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}