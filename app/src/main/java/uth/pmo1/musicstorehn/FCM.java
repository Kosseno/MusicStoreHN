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
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Random;

public class FCM extends FirebaseMessagingService {

    private static final String TAG = "FCM_Service";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Log.d(TAG, "Mensaje recibido de: " + remoteMessage.getFrom());

        String titulo = "MusicStoreHN";
        String detalle = "";

        // Si el mensaje viene de la consola de Firebase como "Notificación"
        if (remoteMessage.getNotification() != null) {
            titulo = remoteMessage.getNotification().getTitle();
            detalle = remoteMessage.getNotification().getBody();
            Log.d(TAG, "Cuerpo de notificación: " + detalle);
        }
        
        // Si el mensaje viene con "Datos" (Data payload)
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Carga de datos: " + remoteMessage.getData());
            if (remoteMessage.getData().containsKey("titulo")) {
                titulo = remoteMessage.getData().get("titulo");
            }
            if (remoteMessage.getData().containsKey("detalle")) {
                detalle = remoteMessage.getData().get("detalle");
            } else if (remoteMessage.getData().containsKey("body")) {
                detalle = remoteMessage.getData().get("body");
            }
        }

        if (detalle != null && !detalle.isEmpty()) {
            mostrarNotificacion(titulo, detalle);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nuevo Token: " + token);
    }

    private void mostrarNotificacion(String titulo, String detalle) {
        String channelId = "music_store_notifications";
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel nc = new NotificationChannel(channelId, "Novedades Musicales", NotificationManager.IMPORTANCE_HIGH);
            nc.setDescription("Avisos de nuevas canciones y grupos");
            nc.enableLights(true);
            nc.enableVibration(true);
            if (nm != null) {
                nm.createNotificationChannel(nc);
            }
        }

        Intent intent = new Intent(this, MenuPrincipal.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent;
        int flags = PendingIntent.FLAG_ONE_SHOT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.baseline_notifications_active_24)
                .setContentTitle(titulo)
                .setContentText(detalle)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);

        int idNotify = new Random().nextInt(8000);

        if (nm != null) {
            nm.notify(idNotify, builder.build());
        }
    }
}