package com.example.finalyearproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Objects;

public class SearchActivity extends AppCompatActivity {

    ArrayList<Product> searchProducts;
    FavouriteListAdapter adapter;
    SearchView searchBar;
    ListView lvSearch;
    ProgressBar progressBar;
    TextView tvNoSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        searchBar = findViewById(R.id.searchBar);
        lvSearch = findViewById(R.id.lvSearch);
        progressBar = findViewById(R.id.progressBar);
        searchProducts = new ArrayList<>();
        tvNoSearch = findViewById(R.id.tvNoSearch);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }


        adapter = new FavouriteListAdapter(searchProducts, getApplicationContext());
        lvSearch.setAdapter(adapter);
        adapter.notifyDataSetChanged(); //Reload list view

        tvNoSearch.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        lvSearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String productId = searchProducts.get(i).getId();
                Intent intent = new Intent(getApplicationContext(), ProductDetails.class);
                intent.putExtra("productId",productId);
                startActivity(intent);
            }
        });
        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                searchProducts.clear();
                adapter.notifyDataSetChanged();
                tvNoSearch.setVisibility(View.GONE); //Hide "no result"text
                progressBar.setVisibility(View.VISIBLE); //Show loading circle
                getProductsFromFirebase(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        searchBar.setIconified(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }

    private void getProductsFromFirebase(String item_name) {
        //turnOffDialog = false;
        item_name = toTitleCase(item_name);
        //Configure Firebase cloud firestore
        FirebaseFirestore fDatabase = FirebaseFirestore.getInstance();
        //Get products from firestore
        final String finalItem_name = item_name;
        fDatabase.collection("product")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            boolean isEmpty = true;
                            for (DocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                if(Objects.requireNonNull(document.get("name")).toString().contains(finalItem_name)) {
                                    isEmpty = false;
                                    setAllData(document);
                                }
                            }
                            if(isEmpty){
                                tvNoSearch.setVisibility(View.VISIBLE);
                            }

                            progressBar.setVisibility(View.GONE);
                        } else {
                            Log.d("TAG_ERROR", "Error getting documents: ", task.getException());
                        }

                    }
                });

    }

    private void setAllData(DocumentSnapshot document){
        // Get items from document snapshot
        String productName = String.valueOf(document.get("name"));
        String priceString = Objects.requireNonNull(document.get("price")).toString();
        double productPrice = Double.parseDouble(priceString);
        String priceStringBefore = Objects.requireNonNull(document.get("price_before")).toString();
        // Set items into product class
        double productPriceBefore = Double.parseDouble(priceStringBefore);
        String type = String.valueOf(document.get("type"));
        Product product = new Product(productName, productPrice, productPriceBefore , type);
        product.setId(document.getId());
        searchProducts.add(product);
        adapter.notifyDataSetChanged(); //Reload list view
    }

    public static String toTitleCase(String givenString) {
        String[] arr = givenString.split(" ");
        StringBuilder sb = new StringBuilder();

        for (String s : arr) {
            sb.append(Character.toUpperCase(s.charAt(0)))
                    .append(s.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
