package uth.pmo1.musicstorehn;

import android.content.Context;
import android.graphics.PorterDuff;
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
import java.util.List;
import java.util.Locale;

public class MultimediaAdapter extends RecyclerView.Adapter<MultimediaAdapter.ViewHolder> {

    public interface OnMultimediaClickListener {
        void onPlayClick(Multimedia multimedia, int position);
        void onDeleteClick(Multimedia multimedia, int position);
    }

    private final Context context;
    private final List<Multimedia> listaMultimedia;
    private final OnMultimediaClickListener listener;
    private final String currentUserId;
    private boolean esCreadorGrupo;

    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("dd MMM yyyy • HH:mm", new Locale("es"));

    public MultimediaAdapter(Context context,
                             ArrayList<Multimedia> listaMultimedia,
                             OnMultimediaClickListener listener,
                             String currentUserId,
                             boolean esCreadorGrupo) {
        this.context = context;
        this.listaMultimedia = listaMultimedia != null ? listaMultimedia : new ArrayList<>();
        this.listener = listener;
        this.currentUserId = currentUserId;
        this.esCreadorGrupo = esCreadorGrupo;
    }

    public void setEsCreadorGrupo(boolean esCreador) {
        if (this.esCreadorGrupo != esCreador) {
            this.esCreadorGrupo = esCreador;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_multimedia, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Multimedia item = listaMultimedia.get(position);

        holder.tvNombre.setText(item.getNombre());

        StringBuilder subtitulo = new StringBuilder("Subido por: ")
                .append(item.getUserName() != null ? item.getUserName() : "—");
        if (item.getTimestamp() > 0) {
            subtitulo.append(" • ").append(SDF.format(new Date(item.getTimestamp())));
        }
        holder.tvSubidoPor.setText(subtitulo.toString());

        // ---------- MINIATURA ----------
        Picasso.get().cancelRequest(holder.ivTipo);

        if (item.getFotoUrl() != null && !item.getFotoUrl().trim().isEmpty()) {
            // Al mostrar carátula real quitamos el tint gris del placeholder
            holder.ivTipo.clearColorFilter();
            holder.ivTipo.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Picasso.get()
                    .load(item.getFotoUrl())
                    .placeholder(R.drawable.baseline_music_note_24)
                    .error(R.drawable.baseline_music_note_24)
                    .fit()
                    .centerCrop()
                    .into(holder.ivTipo);
        } else {
            // Sin carátula: icono genérico según tipo
            holder.ivTipo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            holder.ivTipo.setColorFilter(0xFF9E9E9E, PorterDuff.Mode.SRC_IN);
            if (item.esVideo()) {
                holder.ivTipo.setImageResource(android.R.drawable.ic_menu_slideshow);
            } else {
                holder.ivTipo.setImageResource(R.drawable.baseline_music_note_24);
            }
        }

        holder.btnPlay.setOnClickListener(v -> {
            if (listener != null) listener.onPlayClick(item, holder.getBindingAdapterPosition());
        });

        boolean puedeEliminar =
                (currentUserId != null && currentUserId.equals(item.getUserId()))
                        || esCreadorGrupo;
        holder.btnDelete.setVisibility(puedeEliminar ? View.VISIBLE : View.GONE);

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(item, holder.getBindingAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return listaMultimedia.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvNombre, tvSubidoPor;
        final ImageView ivTipo;
        final ImageButton btnPlay, btnDelete;

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
