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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import es.dmoral.toasty.Toasty;

public class IniciarSesionActivity extends AppCompatActivity {
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

        // Si ya hay sesión activa de Firebase, ir directo al menú
        if (firebaseAuth.getCurrentUser() != null) {
            actualizarTokenFCM();
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

            firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    verificarExistenciaUsuario();
                } else {
                    progressDialog.dismiss();
                    Toasty.error(IniciarSesionActivity.this, "Credenciales incorrectas!", Toast.LENGTH_SHORT, false).show();
                    tEmail.setText("");
                    tPass.setText("");
                    tEmail.requestFocus();
                }
            });
        });
    }

    private void verificarExistenciaUsuario() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            DatabaseReference dr = FirebaseDatabase.getInstance().getReference("Usuarios").child(user.getUid());
            dr.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    progressDialog.dismiss();
                    if (snapshot.exists()) {
                        actualizarTokenFCM();
                        irAlMenu();
                    } else {
                        // El usuario existe en Auth pero no en la BD (posible cuenta a medio borrar)
                        firebaseAuth.signOut();
                        Toasty.error(IniciarSesionActivity.this, "Esta cuenta ya no existe.", Toast.LENGTH_SHORT, false).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    progressDialog.dismiss();
                    firebaseAuth.signOut();
                    Toasty.error(IniciarSesionActivity.this, "Error de red: " + error.getMessage(), Toast.LENGTH_SHORT, false).show();
                }
            });
        } else {
            progressDialog.dismiss();
        }
    }

    private void actualizarTokenFCM() {
        FirebaseMessaging.getInstance().getToken()
            .addOnSuccessListener(token -> FCM.guardarTokenEnFirestore(token));
    }

    private void irAlMenu() {
        Intent intent = new Intent(IniciarSesionActivity.this, MenuPrincipal.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}
