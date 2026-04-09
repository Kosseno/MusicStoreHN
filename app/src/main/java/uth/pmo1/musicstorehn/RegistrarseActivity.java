package uth.pmo1.musicstorehn;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import es.dmoral.toasty.Toasty;

public class RegistrarseActivity extends AppCompatActivity {

    EditText txtUsuarioR, txtEmailR, txtPassR;
    Button btnRegistrardatos;
    TextView cuentaregistrada;
    FirebaseAuth firebaseAuth;
    FirebaseAuth.AuthStateListener authStateListener;
    DatabaseReference databaseReference;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrarse);

        txtUsuarioR = findViewById(R.id.txtUsuarioR);
        txtPassR = findViewById(R.id.txtPassR);
        txtEmailR = findViewById(R.id.txtEmailR);

        cuentaregistrada = findViewById(R.id.cuentaRegistrada);
        btnRegistrardatos = findViewById(R.id.btnRegistrardatos);
        progressDialog = new ProgressDialog(this);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        cuentaregistrada.setOnClickListener(view -> {
            Intent intent = new Intent(RegistrarseActivity.this, IniciarSesionActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        btnRegistrardatos.setOnClickListener(view -> crearCuenta());

        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            // Se mantiene el listener por si se necesita lógica adicional en el futuro
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

    private void crearCuenta() {
        String user = txtUsuarioR.getText().toString().trim();
        String email = txtEmailR.getText().toString().trim();
        String password = txtPassR.getText().toString().trim();

        if (user.isEmpty() && email.isEmpty() && password.isEmpty()) {
            Toasty.info(RegistrarseActivity.this, "Debe ingresar los datos!", Toast.LENGTH_SHORT, false).show();
            txtUsuarioR.requestFocus();
            return;
        }

        if (user.isEmpty()) {
            txtUsuarioR.setError("Ingrese un usuario");
            txtUsuarioR.requestFocus();
            return;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            txtEmailR.setError("Correo no válido!");
            txtEmailR.requestFocus();
            return;
        }
        if (password.isEmpty() || password.length() < 8) {
            txtPassR.setError("Se necesitan 8 o más caracteres!");
            txtPassR.requestFocus();
            return;
        }

        progressDialog.setTitle("Creando cuenta");
        progressDialog.setMessage("Espere un momento por favor...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser != null) {
                    firebaseUser.sendEmailVerification();
                    String id = firebaseUser.getUid();

                    // ✅ MEJORA: Ya NO se guarda la contraseña en la base de datos.
                    // Firebase Auth la gestiona internamente de forma segura.
                    Map<String, Object> map = new HashMap<>();
                    map.put("uid", id);
                    map.put("usuario", user);
                    map.put("correo", email);
                    map.put("fotoUrl", "");
                    map.put("carrera", "");
                    map.put("descripcion", "");
                    map.put("creadoEn", System.currentTimeMillis());

                    databaseReference.child("Usuarios").child(id).setValue(map).addOnCompleteListener(dbTask -> {
                        progressDialog.dismiss();
                        if (dbTask.isSuccessful()) {
                            Toasty.success(RegistrarseActivity.this,
                                    "Usuario " + user + " creado exitosamente!", Toast.LENGTH_LONG, false).show();
                            // Cerrar sesión para que el usuario inicie sesión manualmente
                            firebaseAuth.signOut();
                            Intent intent = new Intent(RegistrarseActivity.this, IniciarSesionActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                            finish();
                        } else {
                            Toasty.error(RegistrarseActivity.this, "No se pudieron crear datos!", Toast.LENGTH_SHORT, false).show();
                        }
                    });
                }
            } else {
                progressDialog.dismiss();
                Toasty.error(RegistrarseActivity.this, "Correo ya registrado!", Toast.LENGTH_SHORT, false).show();
                txtEmailR.setError("Correo no válido!");
                txtEmailR.requestFocus();
            }
        });
    }
}