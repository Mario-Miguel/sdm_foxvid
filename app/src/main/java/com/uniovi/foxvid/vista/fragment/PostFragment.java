package com.uniovi.foxvid.vista.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.uniovi.foxvid.ListaPostAdapter;
import com.uniovi.foxvid.R;
import com.uniovi.foxvid.modelo.Coordinate;
import com.uniovi.foxvid.modelo.Post;
import com.uniovi.foxvid.modelo.User;
import com.uniovi.foxvid.vista.MainActivity;
import com.uniovi.foxvid.vista.NewPostActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PostFragment extends Fragment {

    public static final String POSTS = "posts";
    public static final String MAIN = "main";

    private List<Post> listPost;
    private Coordinate coordinate;
    public int distancia;
    private ListaPostAdapter adapter;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();


    RecyclerView listPostView;
    View root;
    FloatingActionButton btnNewPost;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateLocate();
        root = inflater.inflate(R.layout.fragment_post, container, false);

        if (listPost == null) listPost = new ArrayList<Post>();
        coordinate = new Coordinate(0.0, 0.0);
        listPostView = (RecyclerView) root.findViewById(R.id.idRvPost);


        createGesture();

        Bundle args = getArguments();
        if (args != null) {

        }

        SharedPreferences sharedPreferencesMainRecycler =
                PreferenceManager.getDefaultSharedPreferences(getContext() /* Activity context */);
        distancia = sharedPreferencesMainRecycler.getInt("Key_Seek_KM", 0);
        System.out.println("------------------------- " + distancia);

        loadPost();


        //Floating button -> new post
        btnNewPost = root.findViewById(R.id.btnNewPost);

        btnNewPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadNewPost();
            }
        });

        return root;
    }


    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPreferencesMainRecycler =
                PreferenceManager.getDefaultSharedPreferences(getContext() /* Activity context */);
        distancia = sharedPreferencesMainRecycler.getInt("Key_Seek_KM", 0);
        System.out.println("------------------------- " + distancia);
    }

    protected void loadPost() {
        listPostView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        listPostView.setLayoutManager(layoutManager);
        updateValues();
    }

    protected void loadNewPost() {
        Intent newPostIntent = new Intent(getActivity(), NewPostActivity.class);
        startActivity(newPostIntent);
    }

    private void updateValues() {
        List listActualPost = new ArrayList();
        for (Post p : listPost) {
            listActualPost.add(p.getDate());
        }
        //FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("post")

                .orderBy("date", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            //Log.w(TAG, "listen:error", e);
                            return;
                        }
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            switch (dc.getType()) {
                                case ADDED:
                                    if (coordinate.checkDistancia(new Double(dc.getDocument().get("lat").toString()), new Double(dc.getDocument().get("lon").toString()), distancia))
                                        listPost.add(0, new Post(dc.getDocument().get("uid").toString(),
                                                dc.getDocument().get("post").toString(),
                                                //public User(String uid, String name, String email, Uri photo)
                                                new User(dc.getDocument().get("userUid").toString(), null, dc.getDocument().get("userEmail").toString(), dc.getDocument().get("userImage").toString()),
                                                (Timestamp) dc.getDocument().get("date"),
                                                new Coordinate(new Double(dc.getDocument().get("lat").toString()), new Double(dc.getDocument().get("lat").toString())),
                                                Integer.parseInt(dc.getDocument().get("nLikes").toString()),
                                                Integer.parseInt(dc.getDocument().get("nDislikes").toString())
                                        ));

                                    break;
                                case MODIFIED:
                                    //Log.d(TAG, "Modified city: " + dc.getDocument().getData());
                                    break;
                                case REMOVED:
                                    //Log.d(TAG, "Removed city: " + dc.getDocument().getData());
                                    break;
                            }
                        }

                        adapter = new ListaPostAdapter(listPost, null);
                        listPostView.setAdapter(adapter);

                        for (int i = 0; i < listPost.size(); i++) {
                            updateNumberOfLikes(i);
                        }


                    }
                });
    }

    private void updateLocate() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            coordinate.setLat(location.getLatitude());
                            coordinate.setLon(location.getLongitude());
                        }
                    }
                });

    }


    private void createGesture() {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                if (swipeDir == ItemTouchHelper.LEFT)
                    updateLikes(viewHolder.getLayoutPosition(), -1);
                else
                    updateLikes(viewHolder.getLayoutPosition(), 1);
                System.out.println(listPost.get(viewHolder.getLayoutPosition()).getText());
                adapter.notifyItemChanged(viewHolder.getAdapterPosition());
            }

            @Override
            public void onMoved(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int fromPos, RecyclerView.ViewHolder target, int toPos, int x, int y) {
                super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y);
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

                Bitmap icon;
                Paint p = new Paint();
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {

                    View itemView = viewHolder.itemView;
                    float height = (float) itemView.getBottom() - (float) itemView.getTop();
                    float width = height / 3;

                    if (dX > 0) {
                        p.setColor(Color.parseColor("#388E3C"));
                        RectF background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom());
                        c.drawRect(background, p);
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.baseline_thumb_up_black_18dp);
                        RectF icon_dest = new RectF((float) itemView.getLeft() + width, (float) itemView.getTop() + width, (float) itemView.getLeft() + 2 * width, (float) itemView.getBottom() - width);
                        c.drawBitmap(icon, null, icon_dest, p);
                    } else if (dX < 0) {
                        p.setColor(Color.parseColor("#D32F2F"));
                        RectF background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
                        c.drawRect(background, p);
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.baseline_thumb_down_black_18dp);
                        RectF icon_dest = new RectF((float) itemView.getRight() - 2 * width, (float) itemView.getTop() + width, (float) itemView.getRight() - width, (float) itemView.getBottom() - width);
                        c.drawBitmap(icon, null, icon_dest, p);
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX / 4, dY, actionState, isCurrentlyActive);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(listPostView);
    }


    private void updateLikes(final int position, int like) {
        //FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference postRef = db.collection("post").document(listPost.get(position).getUuid())
                .collection("interactions").document(userId);


        Map<String, Object> data = new HashMap<>();
        data.put("like", like);

        postRef.set(data, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("UpdateLikes", "Empieza update");
                        updateNumberOfLikes(position);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("UpdateLikes", "Mal update" + e);
                        Log.w("Error:", "Error al actualizar los likes", e);
                    }
                });
    }


    private void updateNumberOfLikes(final int postPosition) {
        //FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference likeRef = db.collection("post").document(listPost.get(postPosition).getUuid()).collection("interactions");
        Query queryLike = likeRef.whereEqualTo("like", 1);
        Query queryDisike = likeRef.whereEqualTo("like", -1);

        queryLike.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    int numberOfLikes = task.getResult().size();
                    listPost.get(postPosition).setnLikes(numberOfLikes);
                    adapter.notifyItemChanged(postPosition);
                } else {
                    //Log.d(TAG, "Error getting documents: ", task.getException());
                }
            }
        });

        queryDisike.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    int numberOfDislikes = task.getResult().size();
                    listPost.get(postPosition).setnDislikes(numberOfDislikes);
                    adapter.notifyItemChanged(postPosition);
                } else {
                    //Log.d(TAG, "Error getting documents: ", task.getException());
                }
            }
        });
    }


}