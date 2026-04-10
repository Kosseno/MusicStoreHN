package uth.pmo1.musicstorehn;

/**
 * ============================================================
 * SNIPPET DE REFERENCIA — NO es un archivo completo
 * ============================================================
 *
 * Este archivo muestra CÓMO debe verse el listener del feed del grupo
 * en tu FeedGrupoActivity (o como se llame) DESPUÉS de la limpieza.
 *
 * La regla de oro es:
 *
 *    ❌ NUNCA llames a mostrarNotificacion() dentro de un listener
 *       de Firestore/Realtime Database en el cliente.
 *
 *    ✅ Las notificaciones las manda la Cloud Function
 *       notificarNuevoMultimedia automáticamente al detectar un
 *       documento nuevo en grupos/{grupoId}/multimedia.
 *
 * Si encuentras código como este en tu activity actual:
 *
 *     ref.addChildEventListener(new ChildEventListener() {
 *         public void onChildAdded(DataSnapshot snap, String prev) {
 *             Multimedia m = snap.getValue(Multimedia.class);
 *             listaMultimedia.add(m);
 *             adapter.notifyDataSetChanged();
 *             mostrarNotificacion("Nueva canción", m.getNombre()); // ❌ BORRAR ESTO
 *         }
 *         ...
 *     });
 *
 * Borra la llamada a mostrarNotificacion(). Es exactamente la línea
 * que te estaba spameando cada vez que abrías el grupo, porque
 * onChildAdded se dispara una vez por cada hijo existente al
 * adjuntarse el listener.
 *
 * Abajo queda un ejemplo limpio usando Firestore con addSnapshotListener.
 */

import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;

public class FeedGrupoSnippet {

    private static final String TAG = "FeedGrupo";

    private String grupoId;
    private ArrayList<Multimedia> lista = new ArrayList<>();
    private MultimediaAdapter adapter;
    private ListenerRegistration listenerReg;

    void attachListener() {
        listenerReg = FirebaseFirestore.getInstance()
                .collection("grupos").document(grupoId)
                .collection("multimedia")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listener", e);
                        return;
                    }
                    if (snapshots == null) return;

                    // Solo actualizamos la lista. NADA de notificaciones aquí.
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        Multimedia m = dc.getDocument().toObject(Multimedia.class);
                        m.setId(dc.getDocument().getId());

                        switch (dc.getType()) {
                            case ADDED:
                                lista.add(0, m);
                                adapter.notifyItemInserted(0);
                                break;
                            case MODIFIED:
                                for (int i = 0; i < lista.size(); i++) {
                                    if (m.getId().equals(lista.get(i).getId())) {
                                        lista.set(i, m);
                                        adapter.notifyItemChanged(i);
                                        break;
                                    }
                                }
                                break;
                            case REMOVED:
                                for (int i = 0; i < lista.size(); i++) {
                                    if (m.getId().equals(lista.get(i).getId())) {
                                        lista.remove(i);
                                        adapter.notifyItemRemoved(i);
                                        break;
                                    }
                                }
                                break;
                        }
                    }
                });
    }

    void detachListener() {
        // Importante: siempre remover en onStop/onDestroy para evitar leaks
        if (listenerReg != null) {
            listenerReg.remove();
            listenerReg = null;
        }
    }
}
