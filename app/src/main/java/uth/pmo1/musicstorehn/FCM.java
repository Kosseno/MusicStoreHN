package uth.pmo1.musicstorehn;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FCM extends FirebaseMessagingService {

    private static final String TAG = "FCM_Service";
    private static final String CHANNEL_ID = "music_store_notifications";
    private static final String CHANNEL_NAME = "Novedades Musicales";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String titulo = "MusicStoreHN";
        String detalle = null;

        // 1) Payload de notificación (enviado por la Cloud Function)
        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                titulo = remoteMessage.getNotification().getTitle();
            }
            detalle = remoteMessage.getNotification().getBody();
        }

        // 2) Payload de datos (respaldo o para lógica extra)
        Map<String, String> data = remoteMessage.getData();
        if (!data.isEmpty()) {
            if (data.containsKey("titulo")) titulo = data.get("titulo");
            if (data.containsKey("detalle")) detalle = data.get("detalle");
            else if (data.containsKey("body")) detalle = data.get("body");
        }

        if (detalle != null && !detalle.isEmpty()) {
            mostrarNotificacion(titulo, detalle);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nuevo Token FCM: " + token);
        guardarTokenEnFirestore(token); // Mantenemos el nombre por compatibilidad, pero ahora guarda en RTDB
    }

    /**
     * Guarda el token en Realtime Database para que la Cloud Function lo encuentre.
     * Ruta: /Usuarios/{uid}/fcmToken
     */
    public static void guardarTokenEnFirestore(String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || token == null) return;

        // Ahora guardamos en Realtime Database para coincidir con tu Cloud Function
        FirebaseDatabase.getInstance().getReference("Usuarios")
                .child(user.getUid())
                .child("fcmToken")
                .setValue(token)
                .addOnSuccessListener(v -> Log.d(TAG, "Token guardado en Realtime Database"))
                .addOnFailureListener(e -> Log.e(TAG, "Error guardando token en RTDB", e));
    }

    private void mostrarNotificacion(String titulo, String detalle) {
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
            NotificationChannel nc = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            nc.setDescription("Avisos de nuevas canciones y grupos");
            nc.enableLights(true);
            nc.enableVibration(true);
            nm.createNotificationChannel(nc);
        }

        Intent intent = new Intent(this, MenuPrincipal.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int flags = PendingIntent.FLAG_ONE_SHOT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_notifications_active_24)
                .setContentTitle(titulo)
                .setContentText(detalle)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(detalle))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);

        if (nm != null) {
            nm.notify(new Random().nextInt(8000), builder.build());
        }
    }
}
