package uth.pmo1.musicstorehn;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
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

public class MenuPrincipal extends AppCompatActivity {

    private static final String TAG = "FCM_DEBUG";
    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;
    DatabaseReference databaseReference;
    LinearLayout perfil, musica, musicaonline, grupos, salir;
    TextView tusuario;
    FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_principal);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        // Listener para detectar si la sesión se cierra o el usuario se elimina
        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                irALogin();
            }
        };

        if (firebaseUser == null) {
            irALogin();
            return;
        }

        // 1. Crear el canal de notificación inmediatamente
        crearCanalNotificacion();

        // 2. Suscribirse al tema y obtener el Token para depuración
        FirebaseMessaging.getInstance().subscribeToTopic("enviaratodos")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Suscripción exitosa al tema 'enviaratodos'");
                    }
                });

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Error al obtener el token de FCM", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    Log.d(TAG, "MI TOKEN FCM ES: " + token);
                });

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
                } else {
                    // Si el usuario ya no existe en la base de datos (fue eliminado), cerramos sesión
                    if (firebaseAuth.getCurrentUser() != null) {
                        firebaseAuth.signOut();
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        salir.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MenuPrincipal.this);
            builder.setCancelable(false);
            builder.setMessage("¿Desea cerrar la sesión?")
                    .setPositiveButton("Si", (dialog, which) -> {
                        firebaseAuth.signOut();
                    }).setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
            builder.show();
        });
    }

    private void irALogin() {
        Intent intent = new Intent(MenuPrincipal.this, IniciarSesionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
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

    private void crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "music_store_notifications";
            CharSequence name = "Novedades Musicales";
            String description = "Avisos de nuevas canciones y grupos";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}