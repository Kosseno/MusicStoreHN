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

import java.io.File;
import java.util.List;

import uth.pmo1.musicstorehn.Servicios_Modelo.Result;

public class AdapterListMusica extends BaseAdapter {

    /*
     * ✅ MEJORA: Estados de cada canción:
     * 0 = Sin inicializar
     * 1 = Lista para descargar (mostrar ícono de descarga)
     * 2 = Descargada / lista para reproducir (mostrar ícono de play)
     * 3 = Reproduciendo (mostrar ícono de pause, resaltar fila)
     * 4 = Pausada (mostrar ícono de play, mantener resaltado)
     * 5 = Descargando (mostrar spinner de progreso)
     */

    private Context context;
    private List<Result> results;

    public AdapterListMusica(Context newContext, List<Result> newResults) {
        context = newContext;
        results = newResults;
    }

    @Override
    public int getCount() {
        return results != null ? results.size() : 0;
    }

    @Override
    public Object getItem(int i) {
        return results.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        final int ROW_RESOURCE = R.layout.lista_view_canciones;
        ViewHolder viewHolder;

        if (convertView == null) {
            LayoutInflater layout = LayoutInflater.from(context);
            convertView = layout.inflate(ROW_RESOURCE, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Result result = results.get(pos);
        if (result == null) return convertView;

        try {
            viewHolder.nombreCancion.setText(result.getTrackName() != null ? result.getTrackName() : "Sin título");
            viewHolder.nombreArtista.setText(result.getArtistName() != null ? result.getArtistName() : "Artista desconocido");

            // Cargar imagen con Picasso
            if (result.getArtworkUrl100() != null && !result.getArtworkUrl100().isEmpty()) {
                Picasso.get()
                        .load(result.getArtworkUrl100())
                        .placeholder(R.drawable.baseline_music_note_24)
                        .error(R.drawable.baseline_music_note_24)
                        .into(viewHolder.imgPhoto);
            } else {
                viewHolder.imgPhoto.setImageResource(R.drawable.baseline_music_note_24);
            }

            // ✅ MEJORA: Configurar ícono y visual según el estado
            int state = result.getState();

            // Mostrar/ocultar spinner de descarga
            if (viewHolder.progressDownload != null) {
                viewHolder.progressDownload.setVisibility(state == 5 ? View.VISIBLE : View.GONE);
            }
            viewHolder.imgAction.setVisibility(state == 5 ? View.GONE : View.VISIBLE);

            switch (state) {
                case 1: // Listo para descargar
                    viewHolder.imgAction.setImageResource(R.drawable.baseline_arrow_circle_down_24);
                    viewHolder.nombreCancion.setTextColor(context.getResources().getColor(R.color.text_primary));
                    convertView.setAlpha(1.0f);
                    break;

                case 2: // Descargado — listo para reproducir
                    viewHolder.imgAction.setImageResource(R.drawable.baseline_play_circle_outline_24);
                    viewHolder.nombreCancion.setTextColor(context.getResources().getColor(R.color.text_primary));
                    convertView.setAlpha(1.0f);
                    break;

                case 3: // Reproduciendo
                    viewHolder.imgAction.setImageResource(R.drawable.baseline_pause_circle_outline_24);
                    // ✅ MEJORA: Resaltar la canción en reproducción con el color primary
                    viewHolder.nombreCancion.setTextColor(context.getResources().getColor(R.color.primary));
                    convertView.setAlpha(1.0f);
                    break;

                case 4: // Pausada
                    viewHolder.imgAction.setImageResource(R.drawable.baseline_play_circle_outline_24);
                    // Mantener color primary para indicar que es la canción seleccionada
                    viewHolder.nombreCancion.setTextColor(context.getResources().getColor(R.color.primary));
                    convertView.setAlpha(0.85f);
                    break;

                case 5: // Descargando
                    viewHolder.nombreCancion.setTextColor(context.getResources().getColor(R.color.text_secondary));
                    convertView.setAlpha(0.7f);
                    break;

                default:
                    viewHolder.imgAction.setImageResource(R.drawable.baseline_arrow_circle_down_24);
                    viewHolder.nombreCancion.setTextColor(context.getResources().getColor(R.color.text_primary));
                    convertView.setAlpha(1.0f);
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return convertView;
    }

    public static class ViewHolder {
        ImageView imgPhoto;
        TextView nombreCancion;
        TextView nombreArtista;
        ImageView imgAction;
        ProgressBar progressDownload; // ✅ NUEVO: Spinner de descarga

        public ViewHolder(View view) {
            imgPhoto = view.findViewById(R.id.imgPhoto);
            nombreCancion = view.findViewById(R.id.txtNombreCancion);
            nombreArtista = view.findViewById(R.id.txtNombreArtista);
            imgAction = view.findViewById(R.id.imgAction);
            progressDownload = view.findViewById(R.id.progressDownload);
        }
    }
}