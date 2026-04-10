package uth.pmo1.musicstorehn;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Adapter para el ListView de canciones (lista_view_canciones.xml).
 *
 * Este adapter SÍ carga la miniatura (fotoUrl) con Picasso antes de reproducir,
 * que era justo lo que no se veía con el adapter anterior.
 *
 * Usa el patrón ViewHolder para scrolleo fluido y un listener para los
 * botones de acción (descargar/reproducir y eliminar), de modo que la
 * Activity no tenga que preocuparse por findViewById dentro del loop.
 */
public class CancionAdapter extends BaseAdapter {

    public interface OnCancionActionListener {
        void onPlayOrDownload(Cancion cancion, int position);
        void onDelete(Cancion cancion, int position);
    }

    private final Context context;
    private final List<Cancion> canciones;
    private final String currentUserId;
    private final OnCancionActionListener listener;

    public CancionAdapter(Context context,
                          ArrayList<Cancion> canciones,
                          String currentUserId,
                          OnCancionActionListener listener) {
        this.context = context;
        this.canciones = canciones != null ? canciones : new ArrayList<>();
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @Override public int getCount() { return canciones.size(); }
    @Override public Object getItem(int position) { return canciones.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.lista_view_canciones, parent, false);
            vh = new ViewHolder(convertView);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        Cancion c = canciones.get(position);

        vh.txtNombreCancion.setText(c.getNombreCancion());

        // Nombre del usuario que subió (si lo guardas; si no, queda "Artista")
        if (c.getUserName() != null && !c.getUserName().isEmpty()) {
            vh.txtNombreArtista.setText(c.getUserName());
        } else {
            vh.txtNombreArtista.setText("Artista desconocido");
        }

        // ---------- CARGA DE LA MINIATURA (EL FIX PRINCIPAL) ----------
        // Cancelamos cualquier request previo (importante en ListView reciclado)
        Picasso.get().cancelRequest(vh.imgPhoto);

        if (c.getFotoUrl() != null && !c.getFotoUrl().trim().isEmpty()) {
            Picasso.get()
                    .load(c.getFotoUrl())
                    .placeholder(R.drawable.baseline_music_note_24)
                    .error(R.drawable.baseline_music_note_24)
                    .fit()
                    .centerCrop()
                    .into(vh.imgPhoto);
        } else {
            vh.imgPhoto.setImageResource(R.drawable.baseline_music_note_24);
        }
        // ---------------------------------------------------------------

        // Botón eliminar solo visible si la canción es del usuario actual
        boolean esMia = currentUserId != null && currentUserId.equals(c.getUserId());
        vh.imgDelete.setVisibility(esMia ? View.VISIBLE : View.GONE);

        vh.progressDownload.setVisibility(View.GONE);

        vh.imgAction.setOnClickListener(v -> {
            if (listener != null) listener.onPlayOrDownload(c, position);
        });

        vh.imgDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(c, position);
        });

        return convertView;
    }

    public void setProgressVisible(View row, boolean visible) {
        if (row == null) return;
        ViewHolder vh = (ViewHolder) row.getTag();
        if (vh != null) {
            vh.progressDownload.setVisibility(visible ? View.VISIBLE : View.GONE);
            vh.imgAction.setVisibility(visible ? View.GONE : View.VISIBLE);
        }
    }

    static class ViewHolder {
        final CircleImageView imgPhoto;
        final TextView txtNombreCancion;
        final TextView txtNombreArtista;
        final ProgressBar progressDownload;
        final ImageView imgDelete;
        final ImageView imgAction;

        ViewHolder(View v) {
            imgPhoto = v.findViewById(R.id.imgPhoto);
            txtNombreCancion = v.findViewById(R.id.txtNombreCancion);
            txtNombreArtista = v.findViewById(R.id.txtNombreArtista);
            progressDownload = v.findViewById(R.id.progressDownload);
            imgDelete = v.findViewById(R.id.imgDelete);
            imgAction = v.findViewById(R.id.imgAction);
        }
    }
}
