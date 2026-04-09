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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import es.dmoral.toasty.Toasty;

public class IniciarSesionActivity extends AppCompatActivity {
    // ✅ MEJORA: Se eliminó SharedPreferences para credenciales.
    // Firebase Auth gestiona la persistencia de sesión automáticamente.
    EditText tEmail, tPass;
    TextView olvidastePass, registrate;
    Button btnIniciar;
    FirebaseAuth firebaseAuth;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iniciar_sesion);

        firebaseAuth = FirebaseAuth.getInstance();

        // ✅ MEJORA: Si ya hay sesión activa de Firebase, ir directo al menú
        if (firebaseAuth.getCurrentUser() != null) {
            irAlMenu();
            return;
        }

        tEmail = findViewById(R.id.txtEmail);
        tPass = findViewById(R.id.txtPass);
        olvidastePass = findViewById(R.id.tvolvidastepass);
        registrate = findViewById(R.id.tvregistrarse);
        btnIniciar = findViewById(R.id.btnIniciar);
        progressDialog = new ProgressDialog(IniciarSesionActivity.this);

        olvidastePass.setOnClickListener(view -> {
            Intent intent = new Intent(IniciarSesionActivity.this, RecuperarPassActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        registrate.setOnClickListener(view -> {
            Intent intent = new Intent(IniciarSesionActivity.this, RegistrarseActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        btnIniciar.setOnClickListener(view -> {
            String email = tEmail.getText().toString().trim();
            String password = tPass.getText().toString().trim();

            if (email.isEmpty() && password.isEmpty()) {
                Toasty.info(IniciarSesionActivity.this, "Debe ingresar los datos!", Toast.LENGTH_SHORT, false).show();
                tEmail.requestFocus();
                return;
            }
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tEmail.setError("Correo no válido!");
                tEmail.requestFocus();
                return;
            }
            if (password.isEmpty() || password.length() < 8) {
                tPass.setError("Se necesitan 8 o más caracteres");
                tPass.requestFocus();
                return;
            }

            progressDialog.setTitle("Ingresando");
            progressDialog.setMessage("Espere un momento por favor...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            // ✅ MEJORA: Solo se usa Firebase Auth, no se guardan credenciales localmente
            firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    irAlMenu();
                } else {
                    Toasty.error(IniciarSesionActivity.this, "Credenciales incorrectas!", Toast.LENGTH_SHORT, false).show();
                    tEmail.setText("");
                    tPass.setText("");
                    tEmail.requestFocus();
                }
            });
        });
    }

    private void irAlMenu() {
        Intent intent = new Intent(IniciarSesionActivity.this, MenuPrincipal.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}