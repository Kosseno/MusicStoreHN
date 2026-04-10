package uth.pmo1.musicstorehn;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GruposAdapter extends RecyclerView.Adapter<GruposAdapter.ViewHolder> {

    public interface OnGrupoClickListener {
        void onGrupoClick(Grupo grupo);
    }

    private final Context context;
    private final List<Grupo> listaGrupos;
    private final OnGrupoClickListener listener;

    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("dd MMM yyyy", new Locale("es"));

    public GruposAdapter(Context context, ArrayList<Grupo> listaGrupos,
                         OnGrupoClickListener listener) {
        this.context = context;
        this.listaGrupos = listaGrupos != null ? listaGrupos : new ArrayList<>();
        this.listener = listener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_grupo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Grupo grupo = listaGrupos.get(position);

        // Nombre con candado si es privado
        String nombre = grupo.getNombre();
        if (grupo.isEsPrivado()) {
            nombre = "🔒  " + nombre;
        }
        holder.tvNombre.setText(nombre);

        String desc = grupo.getDescripcion();
        holder.tvDescripcion.setText(desc != null && !desc.isEmpty()
                ? desc : "Sin descripción");

        StringBuilder meta = new StringBuilder("Creado por: ")
                .append(grupo.getCreadorNombre() != null ? grupo.getCreadorNombre() : "—");
        if (grupo.getCreadoEn() > 0) {
            meta.append(" • ").append(SDF.format(new Date(grupo.getCreadoEn())));
        }
        if (grupo.getMiembrosCount() > 0) {
            meta.append(" • ").append(grupo.getMiembrosCount()).append(" miembros");
        }
        holder.tvCreador.setText(meta.toString());

        holder.btnEntrar.setOnClickListener(v -> {
            if (listener != null) listener.onGrupoClick(grupo);
        });
    }

    @Override
    public int getItemCount() {
        return listaGrupos.size();
    }

    @Override
    public long getItemId(int position) {
        Grupo g = listaGrupos.get(position);
        return g.getId() != null ? g.getId().hashCode() : position;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvNombre, tvDescripcion, tvCreador;
        final Button btnEntrar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreGrupo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionGrupo);
            tvCreador = itemView.findViewById(R.id.tvCreador);
            btnEntrar = itemView.findViewById(R.id.btnEntrarGrupo);
        }
    }
}
