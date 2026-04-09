package uth.pmo1.musicstorehn;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class GruposAdapter extends RecyclerView.Adapter<GruposAdapter.ViewHolder> {

    private Context context;
    private ArrayList<Grupo> listaGrupos;
    private OnGrupoClickListener listener;

    public interface OnGrupoClickListener {
        void onGrupoClick(Grupo grupo);
    }

    public GruposAdapter(Context context, ArrayList<Grupo> listaGrupos, OnGrupoClickListener listener) {
        this.context = context;
        this.listaGrupos = listaGrupos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_grupo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Grupo grupo = listaGrupos.get(position);
        holder.tvNombre.setText(grupo.getNombre());
        holder.tvDescripcion.setText(grupo.getDescripcion());
        holder.tvCreador.setText("Creado por: " + grupo.getCreadorNombre());

        holder.btnEntrar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGrupoClick(grupo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaGrupos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvDescripcion, tvCreador;
        Button btnEntrar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreGrupo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionGrupo);
            tvCreador = itemView.findViewById(R.id.tvCreador);
            btnEntrar = itemView.findViewById(R.id.btnEntrarGrupo);
        }
    }
}
