package uth.pmo1.musicstorehn;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MultimediaAdapter extends RecyclerView.Adapter<MultimediaAdapter.ViewHolder> {

    private Context context;
    private ArrayList<Multimedia> listaMultimedia;
    private OnMultimediaClickListener listener;
    private String currentUserId;
    private boolean esCreadorGrupo;

    public interface OnMultimediaClickListener {
        void onPlayClick(Multimedia multimedia, int position);
        void onDeleteClick(Multimedia multimedia, int position);
    }

    public MultimediaAdapter(Context context, ArrayList<Multimedia> listaMultimedia,
                             OnMultimediaClickListener listener, String currentUserId, boolean esCreadorGrupo) {
        this.context = context;
        this.listaMultimedia = listaMultimedia;
        this.listener = listener;
        this.currentUserId = currentUserId;
        this.esCreadorGrupo = esCreadorGrupo;
    }

    public void setEsCreadorGrupo(boolean esCreador) {
        this.esCreadorGrupo = esCreador;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_multimedia, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Multimedia item = listaMultimedia.get(position);
        holder.tvNombre.setText(item.getNombre());
        holder.tvSubidoPor.setText("Subido por: " + item.getUserName());

        if (item.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String fecha = sdf.format(new Date(item.getTimestamp()));
            holder.tvSubidoPor.setText("Subido por: " + item.getUserName() + " • " + fecha);
        }

        // ✅ MEJORA: Mostrar miniatura (carátula o video) si existe
        if (item.getFotoUrl() != null && !item.getFotoUrl().isEmpty()) {
            Picasso.get()
                    .load(item.getFotoUrl())
                    .placeholder(R.drawable.baseline_music_note_24)
                    .error(R.drawable.baseline_music_note_24)
                    .into(holder.ivTipo);
        } else {
            if ("video".equals(item.getTipo())) {
                holder.ivTipo.setImageResource(android.R.drawable.ic_menu_slideshow);
            } else {
                holder.ivTipo.setImageResource(R.drawable.baseline_music_note_24);
            }
        }

        holder.btnPlay.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlayClick(item, position);
            }
        });

        boolean puedeEliminar = (currentUserId != null && currentUserId.equals(item.getUserId())) || esCreadorGrupo;
        holder.btnDelete.setVisibility(puedeEliminar ? View.VISIBLE : View.GONE);

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(item, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaMultimedia != null ? listaMultimedia.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvSubidoPor;
        ImageView ivTipo;
        ImageButton btnPlay, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreMultimedia);
            tvSubidoPor = itemView.findViewById(R.id.tvSubidoPor);
            ivTipo = itemView.findViewById(R.id.ivTipoIcon);
            btnPlay = itemView.findViewById(R.id.btnPlayMultimedia);
            btnDelete = itemView.findViewById(R.id.btnDeleteMultimedia);
        }
    }
}