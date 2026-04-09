package uth.pmo1.musicstorehn;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Random;

public class FCM extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String titulo = "MusicStoreHN";
        String detalle = "";

        if (remoteMessage.getNotification() != null) {
            titulo = remoteMessage.getNotification().getTitle();
            detalle = remoteMessage.getNotification().getBody();
        } else if (remoteMessage.getData().size() > 0) {
            titulo = remoteMessage.getData().get("titulo");
            detalle = remoteMessage.getData().get("detalle");
        }

        if (detalle != null && !detalle.isEmpty()) {
            mostrarNotificacion(titulo, detalle);
        }
    }

    private void mostrarNotificacion(String titulo, String detalle) {
        String channelId = "music_store_notifications";
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel nc = new NotificationChannel(channelId, "Novedades Musicales", NotificationManager.IMPORTANCE_HIGH);
            nc.setDescription("Avisos de nuevas canciones y grupos");
            if (nm != null) {
                nm.createNotificationChannel(nc);
            }
        }

        Intent intent = new Intent(this, MenuPrincipal.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.baseline_notifications_active_24)
                .setContentTitle(titulo)
                .setContentText(detalle)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        Random random = new Random();
        int idNotify = random.nextInt(8000);

        if (nm != null) {
            nm.notify(idNotify, builder.build());
        }
    }
}
