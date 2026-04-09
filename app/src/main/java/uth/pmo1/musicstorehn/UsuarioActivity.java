package uth.pmo1.musicstorehn;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
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

import es.dmoral.toasty.Toasty;

public class UsuarioActivity extends AppCompatActivity {
    TextView tusuario, tcorreo, tcarrera, tdescripcion;
    ImageView fotografia;
    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;
    DatabaseReference databaseReference;

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
                        
                        // Validar Carrera
                        if (carrera == null || carrera.trim().isEmpty() || carrera.equals("null")) {
                            tcarrera.setText("Añade tu carrera");
                            tcarrera.setAlpha(0.6f);
                        } else {
                            tcarrera.setText(carrera);
                            tcarrera.setAlpha(1.0f);
                        }

                        // Validar Descripción
                        if (descripcion == null || descripcion.trim().isEmpty() || descripcion.equals("null")) {
                            tdescripcion.setText("Cuéntanos algo sobre ti...");
                            tdescripcion.setAlpha(0.6f);
                        } else {
                            tdescripcion.setText(descripcion);
                            tdescripcion.setAlpha(1.0f);
                        }

                        // Validar Foto
                        if (foto != null && !foto.trim().isEmpty() && !foto.equals("null")) {
                            Picasso.get()
                                    .load(foto)
                                    .placeholder(R.drawable.usuario)
                                    .error(R.drawable.usuario)
                                    .into(fotografia);
                        } else {
                            fotografia.setImageResource(R.drawable.usuario);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
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
            Intent intent = new Intent(this, EditarUsuarioActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
            return true;
        }
        if (item.getItemId() == R.id.cambiar_pass) {
            cambiarContrasena();
            return true;
        }
        if (item.getItemId() == R.id.eliminar) {
            eliminarCuenta();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void cambiarContrasena() {
        if (firebaseUser == null || firebaseUser.getEmail() == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Cambiar Contraseña")
                .setMessage("Se enviará un enlace a tu correo (" + firebaseUser.getEmail() + ") para restablecer tu contraseña.")
                .setPositiveButton("Enviar", (dialog, which) -> {
                    firebaseAuth.setLanguageCode("es");
                    firebaseAuth.sendPasswordResetEmail(firebaseUser.getEmail())
                            .addOnSuccessListener(unused ->
                                    Toasty.success(this, "Correo enviado! Revisa tu bandeja.", Toast.LENGTH_LONG, false).show())
                            .addOnFailureListener(e ->
                                    Toasty.error(this, "Error al enviar correo", Toast.LENGTH_SHORT, false).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarCuenta() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Eliminar cuenta");
        builder.setMessage("¿Estás seguro? Esta acción eliminará tu cuenta y todos tus datos permanentemente.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    databaseReference.child(firebaseUser.getUid()).removeValue().addOnSuccessListener(unused -> {
                        firebaseUser.delete().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toasty.success(UsuarioActivity.this, "Cuenta eliminada correctamente!", Toast.LENGTH_SHORT, false).show();
                            } else {
                                Toasty.info(UsuarioActivity.this, "Datos eliminados. Puede que necesites re-autenticarte para eliminar la cuenta de Auth.", Toast.LENGTH_LONG, false).show();
                            }
                            Intent intent = new Intent(UsuarioActivity.this, IniciarSesionActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                            finish();
                        });
                    });
                }).setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}
