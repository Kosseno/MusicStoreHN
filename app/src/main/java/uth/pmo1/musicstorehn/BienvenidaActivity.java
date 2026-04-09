package uth.pmo1.musicstorehn;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import java.time.temporal.Temporal;

public class BienvenidaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_bienvenida);

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

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(BienvenidaActivity.this, IniciarSesionActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
        }, 4000);
    }
}