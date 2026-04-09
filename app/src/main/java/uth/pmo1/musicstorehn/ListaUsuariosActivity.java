package uth.pmo1.musicstorehn;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.os.Bundle;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ListaUsuariosActivity extends AppCompatActivity {

    UsuariosAdapter usuariosAdapter;
    RecyclerView recyclerView;
    DatabaseReference databaseReference;
    ArrayList<Usuarios> lista;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_usuarios);

        recyclerView = findViewById(R.id.recyclervListaUsuarios);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        lista = new ArrayList<Usuarios>();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        databaseReference = FirebaseDatabase.getInstance().getReference().child("Usuarios");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lista.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    Usuarios user = ds.getValue(Usuarios.class);
                    lista.add(user);
                }
                usuariosAdapter = new UsuariosAdapter(ListaUsuariosActivity.this,lista);
                recyclerView.setAdapter(usuariosAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}