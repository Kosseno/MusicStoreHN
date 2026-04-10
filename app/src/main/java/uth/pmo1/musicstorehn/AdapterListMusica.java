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

    public interface OnDeleteClickListener {
        void onDelete(Result song);
    }

    private Context context;
    private List<Result> results;
    private OnDeleteClickListener deleteListener;

    public AdapterListMusica(Context newContext, List<Result> newResults, OnDeleteClickListener listener) {
        context = newContext;
        results = newResults;
        deleteListener = listener;
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

            if (result.getArtworkUrl100() != null && !result.getArtworkUrl100().isEmpty()) {
                Picasso.get().load(result.getArtworkUrl100()).placeholder(R.drawable.baseline_music_note_24).error(R.drawable.baseline_music_note_24).into(viewHolder.imgPhoto);
            } else {
                viewHolder.imgPhoto.setImageResource(R.drawable.baseline_music_note_24);
            }

            int state = result.getState();

            if (viewHolder.progressDownload != null) {
                viewHolder.progressDownload.setVisibility(state == 5 ? View.VISIBLE : View.GONE);
            }
            viewHolder.imgAction.setVisibility(state == 5 ? View.GONE : View.VISIBLE);
            
            // Mostrar botón de eliminar solo si está descargado
            viewHolder.imgDelete.setVisibility(state == 2 || state == 3 || state == 4 ? View.VISIBLE : View.GONE);
            viewHolder.imgDelete.setOnClickListener(v -> {
                if (deleteListener != null) deleteListener.onDelete(result);
            });

            switch (state) {
                case 1:
                    viewHolder.imgAction.setImageResource(R.drawable.baseline_arrow_circle_down_24);
                    viewHolder.nombreCancion.setTextColor(context.getResources().getColor(R.color.text_primary));
                    convertView.setAlpha(1.0f);
                    break;
                case 2:
                    viewHolder.imgAction.setImageResource(R.drawable.baseline_play_circle_outline_24);
                    viewHolder.nombreCancion.setTextColor(context.getResources().getColor(R.color.text_primary));
                    convertView.setAlpha(1.0f);
                    break;
                case 3:
                    viewHolder.imgAction.setImageResource(R.drawable.baseline_pause_circle_outline_24);
                    viewHolder.nombreCancion.setTextColor(context.getResources().getColor(R.color.primary));
                    convertView.setAlpha(1.0f);
                    break;
                case 4:
                    viewHolder.imgAction.setImageResource(R.drawable.baseline_play_circle_outline_24);
                    viewHolder.nombreCancion.setTextColor(context.getResources().getColor(R.color.primary));
                    convertView.setAlpha(0.85f);
                    break;
                case 5:
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
        ImageView imgDelete;
        ProgressBar progressDownload;

        public ViewHolder(View view) {
            imgPhoto = view.findViewById(R.id.imgPhoto);
            nombreCancion = view.findViewById(R.id.txtNombreCancion);
            nombreArtista = view.findViewById(R.id.txtNombreArtista);
            imgAction = view.findViewById(R.id.imgAction);
            imgDelete = view.findViewById(R.id.imgDelete);
            progressDownload = view.findViewById(R.id.progressDownload);
        }
    }
}