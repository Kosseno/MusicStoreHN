package uth.pmo1.musicstorehn;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import es.dmoral.toasty.Toasty;

public class EditarUsuarioActivity extends AppCompatActivity {

    // ✅ MEJORA: Se eliminaron los campos de contraseña (password, verPass)
    // La contraseña se cambia únicamente a través de Firebase Auth por correo electrónico
    EditText usuario, correo, carrera, descripcion;
    Button editar, cancelar;
    ImageView tomarFoto;
    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;
    DatabaseReference databaseReference;
    Uri uri;
    String imgUrl;
    ProgressDialog progressDialog;
    ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_usuario);

        usuario = findViewById(R.id.txtUsuario);
        correo = findViewById(R.id.txtCorreo);
        carrera = findViewById(R.id.txtCarrera);
        descripcion = findViewById(R.id.txtDescripcion);

        editar = findViewById(R.id.btneditar);
        cancelar = findViewById(R.id.btnCancelar);
        tomarFoto = findViewById(R.id.imgtomarFoto);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("Usuarios");

        progressDialog = new ProgressDialog(EditarUsuarioActivity.this);

        // ✅ MEJORA: Usar ActivityResultLauncher en vez del deprecado startActivityForResult
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        uri = result.getData().getData();
                        tomarFoto.setImageURI(uri);
                    }
                });

        if (firebaseUser != null) {
            cargarDatosUsuario();
        }

        editar.setOnClickListener(view -> {
            // ✅ MEJORA: Validación de campos obligatorios
            String nombreUsuario = usuario.getText().toString().trim();
            if (nombreUsuario.isEmpty()) {
                usuario.setError("El nombre de usuario es obligatorio");
                usuario.requestFocus();
                return;
            }

            if (uri != null) {
                guardarDatosConImagen();
            } else {
                editarDatos(imgUrl);
            }
        });

        cancelar.setOnClickListener(view -> volverAlPerfil());

        tomarFoto.setOnClickListener(view -> {
            Intent photo = new Intent(Intent.ACTION_PICK);
            photo.setType("image/*");
            activityResultLauncher.launch(photo);
        });
    }

    private void cargarDatosUsuario() {
        databaseReference.child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String correoDatos = "" + snapshot.child("correo").getValue();
                    String usuarioDatos = "" + snapshot.child("usuario").getValue();
                    String carreraDatos = snapshot.hasChild("carrera") ? "" + snapshot.child("carrera").getValue() : "";
                    String descripcionDatos = snapshot.hasChild("descripcion") ? "" + snapshot.child("descripcion").getValue() : "";
                    imgUrl = "" + snapshot.child("fotoUrl").getValue();

                    usuario.setText(usuarioDatos);
                    correo.setText(correoDatos);
                    // ✅ MEJORA: El correo es de solo lectura (se cambia desde Firebase Auth)
                    correo.setEnabled(false);
                    carrera.setText(carreraDatos);
                    descripcion.setText(descripcionDatos);

                    if (imgUrl != null && !imgUrl.isEmpty() && !imgUrl.equals("null")) {
                        Picasso.get().load(imgUrl).placeholder(R.drawable.usuario).into(tomarFoto);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toasty.error(EditarUsuarioActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarDatosConImagen() {
        progressDialog.setTitle("Actualizando perfil");
        progressDialog.setMessage("Subiendo imagen, espere un momento por favor...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        StorageReference storage = FirebaseStorage.getInstance().getReference()
                .child("Usuarios")
                .child(firebaseUser.getUid() + "_" + System.currentTimeMillis());

        storage.putFile(uri).addOnSuccessListener(taskSnapshot -> {
            storage.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                editarDatos(downloadUri.toString());
                progressDialog.dismiss();
            });
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toasty.error(EditarUsuarioActivity.this, "Error al subir imagen", Toast.LENGTH_SHORT).show();
        });
    }

    private void editarDatos(String nuevaImgUrl) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Usuarios").child(firebaseUser.getUid());

        // ✅ MEJORA: Ya NO se guarda la contraseña en la base de datos
        Map<String, Object> map = new HashMap<>();
        map.put("usuario", usuario.getText().toString().trim());
        map.put("carrera", carrera.getText().toString().trim());
        map.put("descripcion", descripcion.getText().toString().trim());
        if (nuevaImgUrl != null && !nuevaImgUrl.equals("null")) {
            map.put("fotoUrl", nuevaImgUrl);
        }

        reference.updateChildren(map).addOnSuccessListener(unused -> {
            Toasty.success(EditarUsuarioActivity.this, "Perfil actualizado!", Toast.LENGTH_SHORT, false).show();
            volverAlPerfil();
        }).addOnFailureListener(e -> {
            Toasty.error(EditarUsuarioActivity.this, "Error al actualizar!", Toast.LENGTH_SHORT, false).show();
        });
    }

    private void volverAlPerfil() {
        Intent intent = new Intent(EditarUsuarioActivity.this, UsuarioActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    @Override
    public void onBackPressed() {
        volverAlPerfil();
    }
}