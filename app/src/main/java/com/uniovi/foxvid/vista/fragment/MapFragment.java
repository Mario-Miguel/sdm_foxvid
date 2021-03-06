package com.uniovi.foxvid.vista.fragment;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.uniovi.foxvid.utils.PostsDatabaseHandler;
import com.uniovi.foxvid.modelo.Post;
import com.uniovi.foxvid.utils.LocationHandler;
import com.uniovi.foxvid.R;
import com.uniovi.foxvid.modelo.Coordinate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    SupportMapFragment mapFragment;
    private GoogleMap mMap;
    LatLng centro;
    List<LatLng> latLngs = new ArrayList<>();

    LocationHandler locationHandler = LocationHandler.getLocationHandler();
    PostsDatabaseHandler postsHandler = PostsDatabaseHandler.getPostsDatabaseHandler();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View root = inflater.inflate(R.layout.fragment_statistics, container, false);

        initMap();

        return root;
    }


    /**
     * M??todo  que inicia el mapa y lo asigna al componente del fragment destinado para el
     */
    private void initMap() {
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }


    /**
     * Se centra la posici??n en Madrid para centrar Espa??a en la pantalla.
     * Tambien se carga la capa para hacer el mapa de calor.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Coordenadas de Madrid
        centro = new LatLng(40.4165, -3.70256);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(centro, 5.2F));

        OnSuccessListener<Location> listener = new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    setCentro(locationHandler.getUserCoordinate());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(centro, 6.2F));
                }
            }
        };

        locationHandler.updateLocate(getActivity(), listener);

        createHeatmapLayer();

    }

    private void setCentro(Coordinate userCoordinate) {
        this.centro = new LatLng(userCoordinate.getLat(), userCoordinate.getLon());
    }

    /**
     * M??todo que crea la capa de calor y la asigna al mapa.
     * Se obtienen los posts realizados en las ??ltimas 24 horas para visualizar en qu?? zonas se
     * est??n realizando m??s publicaciones.
     */
    private void createHeatmapLayer() {

        OnCompleteListener listener = new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                for(Post post: postsHandler.getPosts()){
                    latLngs.add(
                            new LatLng(
                                    post.getLocalization().getLat(),
                                    post.getLocalization().getLon()
                            )
                    );
                }
                if(latLngs.size() > 0) {
                    //Se crea el proveedor de la capa con las coordenadas obtenidas
                    HeatmapTileProvider provider = new HeatmapTileProvider.Builder()
                            .data(latLngs)
                            .build();

                    //Se a??ade la capa al mapa
                    mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
                }else{
                    Toast.makeText(getContext(), R.string.error_mapa, Toast.LENGTH_LONG).show();
                }
            }
        };

        postsHandler.getLast24HoursPosts(listener);

    }


}