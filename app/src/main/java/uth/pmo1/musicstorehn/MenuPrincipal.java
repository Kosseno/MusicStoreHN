package uth.pmo1.musicstorehn;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MenuPrincipal extends AppCompatActivity {

    // ✅ MEJORA: Se eliminó SharedPreferences — la sesión se gestiona con Firebase Auth
    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;
    DatabaseReference databaseReference;
    LinearLayout perfil, musica, musicaonline, grupos, salir;
    TextView tusuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_principal);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        // ✅ MEJORA: Si no hay sesión activa, redirigir al login
        if (firebaseUser == null) {
            startActivity(new Intent(this, IniciarSesionActivity.class));
            finish();
            return;
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("Usuarios");

        tusuario = findViewById(R.id.textVUsuario);
        perfil = findViewById(R.id.btnPerfilWrapper);
        musica = findViewById(R.id.btnMusicaWrapper);
        musicaonline = findViewById(R.id.btnMusicaonlineWrapper);
        grupos = findViewById(R.id.btnGruposWrapper);
        salir = findViewById(R.id.btnSalirWrapper);

        perfil.setOnClickListener(view -> startActivity(new Intent(MenuPrincipal.this, UsuarioActivity.class)));
        musica.setOnClickListener(view -> startActivity(new Intent(MenuPrincipal.this, MusicaActivity.class)));
        musicaonline.setOnClickListener(view -> startActivity(new Intent(MenuPrincipal.this, MusicaOnlineActivity.class)));
        grupos.setOnClickListener(view -> startActivity(new Intent(MenuPrincipal.this, GruposActivity.class)));

        databaseReference.child(firebaseUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String usuario = "" + snapshot.child("usuario").getValue();
                    tusuario.setText(usuario);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        salir.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MenuPrincipal.this);
            builder.setCancelable(false);
            builder.setMessage("¿Desea cerrar la sesión de MusicStoreHN?")
                    .setPositiveButton("Si", (dialog, which) -> {
                        // ✅ MEJORA: Solo se cierra sesión de Firebase, no hay credenciales locales que limpiar
                        firebaseAuth.signOut();
                        Intent intent = new Intent(MenuPrincipal.this, IniciarSesionActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        finish();
                    }).setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
            builder.show();
        });
    }
}