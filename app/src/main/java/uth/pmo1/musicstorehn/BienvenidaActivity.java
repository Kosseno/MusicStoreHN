package uth.pmo1.musicstorehn;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

public class BienvenidaActivity extends AppCompatActivity {

    private static final String TAG = "FCM_DEBUG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_bienvenida);

        Log.i(TAG, ">>> INICIANDO APP EN 2026 <<<");

        // Forzar inicialización
        FirebaseApp.initializeApp(this);

        obtenerTokenFCM();

        Animation animation1 = AnimationUtils.loadAnimation(this, R.anim.desplazamiento_arriba);
        Animation animation2 = AnimationUtils.loadAnimation(this, R.anim.desplazamiento_abajo);

        TextView txtbienvenido = findViewById(R.id.textViewBienvenido);
        TextView txtA = findViewById(R.id.textViewA);
        TextView txtlogo = findViewById(R.id.textViewLogo);
        ImageView imglogo = findViewById(R.id.imageViewLogo);

        txtbienvenido.setAnimation(animation2);
        txtA.setAnimation(animation2);
        txtlogo.setAnimation(animation2);
        imglogo.setAnimation(animation1);

        pedirPermisoNotificaciones();
    }

    private void obtenerTokenFCM() {
        Log.i(TAG, "Intentando obtener Token...");
        
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Log.i(TAG, "¡ÉXITO! TOKEN FCM: " + token);
                        // Copia este token para probar en la consola
                    } else {
                        Log.e(TAG, "FALLO TOTAL. Error: " + task.getException().getMessage());
                        if (task.getException().getCause() != null) {
                            Log.e(TAG, "CAUSA DEL FALLO: " + task.getException().getCause().getMessage());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "EXCEPCIÓN EN RED: " + e.getMessage());
                });

        // Verificación de 15 segundos
        new Handler().postDelayed(() -> {
            Log.w(TAG, "Si no ves el TOKEN arriba, el dispositivo tiene bloqueada la conexión a Google.");
        }, 15000);
    }

    private void pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Dexter.withActivity(this)
                    .withPermission(Manifest.permission.POST_NOTIFICATIONS)
                    .withListener(new PermissionListener() {
                        @Override public void onPermissionGranted(PermissionGrantedResponse r) { irALogin(); }
                        @Override public void onPermissionDenied(PermissionDeniedResponse r) { 
                            Log.w(TAG, "Permiso de notificaciones denegado por el usuario.");
                            irALogin(); 
                        }
                        @Override public void onPermissionRationaleShouldBeShown(PermissionRequest p, PermissionToken t) { t.continuePermissionRequest(); }
                    }).check();
        } else {
            irALogin();
        }
    }

    private void irALogin() {
        new Handler().postDelayed(() -> {
            if (!isFinishing()) {
                Intent intent = new Intent(BienvenidaActivity.this, IniciarSesionActivity.class);
                startActivity(intent);
                finish();
            }
        }, 3000);
    }
}