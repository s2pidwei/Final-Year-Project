package com.example.finalyearproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class FavouriteActivity extends AppCompatActivity {

    TextView tvFavDesc;
    ListView lvFavourite;
    ArrayList<Product> favouriteProducts;
    FavouriteListAdapter adapter;
    ProgressDialog loadingDialog;
    boolean turnOffDialog = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourite_products);
        tvFavDesc = findViewById(R.id.tvFavDesc);
        lvFavourite = findViewById(R.id.lvFavourite);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        favouriteProducts = new ArrayList<>();
        loadingDialog = ProgressDialog.show(FavouriteActivity.this, "",
                "Retrieving Data. Please wait...", true);
        adapter = new FavouriteListAdapter(favouriteProducts, getApplicationContext());
        lvFavourite.setAdapter(adapter);
        adapter.notifyDataSetChanged(); //Reload list view

        lvFavourite.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String productId = favouriteProducts.get(i).getId();
                Intent intent = new Intent(getApplicationContext(), ProductDetails.class);
                intent.putExtra("productId",productId);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onResume(){
        super.onResume();
        favouriteProducts.clear();
        adapter.notifyDataSetChanged();
        SharedPreferences sharedPreferences = getSharedPreferences("saved_product", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPreferences.getAll();
        for (Map.Entry<String, ?> productId : allEntries.entrySet()) {
            getProductFromFirebase(productId.getValue().toString());
        }
        if(allEntries.isEmpty()){
            tvFavDesc.setText("No favourite product. \n" +
                    "Add the products you want to favorites to get notified when products are discounted.");
        }
        if(turnOffDialog){
            loadingDialog.dismiss();
        }
    }

    private void getProductFromFirebase(String item_name) {
        turnOffDialog = false;
        //Configure Firebase cloud firestore
        FirebaseFirestore fDatabase = FirebaseFirestore.getInstance();
        //Get product from firestore
        fDatabase.collection("product").document(item_name)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null) {
                                // Get items from document snapshot
                                String productName = String.valueOf(document.get("name"));
                                String priceString = Objects.requireNonNull(document.get("price")).toString();
                                double productPrice = Double.parseDouble(priceString);
                                String priceStringBefore = Objects.requireNonNull(document.get("price_before")).toString();
                                // Set item into product class
                                double productPriceBefore = Double.parseDouble(priceStringBefore);
                                String type = String.valueOf(document.get("type"));
                                Product product = new Product(productName, productPrice, productPriceBefore , type);
                                product.setId(document.getId());
                                favouriteProducts.add(product);
                                adapter.notifyDataSetChanged(); //Reload list view
                                loadingDialog.dismiss();
                            }
                        } else {
                            Log.d("TAG_ERROR", "Error getting documents: ", task.getException());
                        }
                    }
                });

    }

}
