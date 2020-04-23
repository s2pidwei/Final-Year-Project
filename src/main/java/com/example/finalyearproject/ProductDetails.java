package com.example.finalyearproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Locale;
import java.util.Objects;

public class ProductDetails extends AppCompatActivity {

    TextView tvName, tvType, tvPrice, tvPriceAfter;
    Button btnFavourite;
    boolean isFavourited;
    ImageView imgViewProduct;
    Product product;
    ProgressDialog loadingDialog;
    String documentId;
    SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);
        //Declare & assign textView variable
        tvName = findViewById(R.id.tvName);
        tvType = findViewById(R.id.tvType);
        tvPrice = findViewById(R.id.tvPrice);
        tvPriceAfter = findViewById(R.id.tvPriceAfter);
        imgViewProduct = findViewById(R.id.imgViewProduct);
        btnFavourite = findViewById(R.id.btnFavourite);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        final String productId = intent.getStringExtra("productId");
        final String productCode = intent.getStringExtra("productCode");
        //Product class
        product = new Product();

        //Load page progress bar
        loadingDialog = ProgressDialog.show(ProductDetails.this, "",
                "Retrieving Data. Please wait...", true);
        FirebaseFirestore fDatabase = FirebaseFirestore.getInstance();

        if(productId != null) {
            fDatabase.collection("product")
                    .document(productId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document != null) {
                                    setAllData(document);
                                }
                            } else {
                                Log.d("TAG_ERROR", "Error getting documents: ", task.getException());
                            }
                        }
                    });
        }else if(productCode != null) {
            fDatabase.collection("product")
                    .whereEqualTo("key", productCode)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                    setAllData(document);
                                    Log.d("TAG_TEST", document.getId() + " => " + document.getData());
                                }
                                if(task.getResult().isEmpty()){
                                    loadingDialog.dismiss();
                                    AlertDialog.Builder builder1 = new AlertDialog.Builder(ProductDetails.this);
                                    builder1.setMessage("Product not found.\nProduct Code: " + productCode);
                                    builder1.setCancelable(false);
                                    builder1.setPositiveButton(
                                            "Back",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.cancel();
                                                    finish();
                                                }
                                            });

                                    AlertDialog alert11 = builder1.create();
                                    alert11.show();
                                }
                            } else {
                                Log.d("TAG_ERROR", "Error getting documents: ", task.getException());
                            }
                        }
                    });
        }

        //Add/Remove product favourite button on click listener
        btnFavourite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!isFavourited) {
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString(documentId, documentId);
                    editor.apply();
                    FirebaseMessaging.getInstance().subscribeToTopic(documentId);
                    isFavourited = true;
                    btnFavourite.setText("Remove from favourite");
                    Toast.makeText(getApplicationContext(),"Product added to favourite.", Toast.LENGTH_SHORT).show();
                }else {
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.remove(documentId);
                    editor.apply();
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(documentId);
                    isFavourited = false;
                    btnFavourite.setText("Add to favourite");
                    Toast.makeText(getApplicationContext(),"Product removed from favourite.", Toast.LENGTH_SHORT).show();
                }

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

    private void setAllData(DocumentSnapshot document){
        documentId = document.getId();
        //Get details from Firebase Cloud Firestore
        String productName = String.valueOf(document.get("name")); // Product Name
        String priceString = Objects.requireNonNull(document.get("price")).toString();
        double productPrice = Double.parseDouble(priceString); // Product Price
        String priceBeforeString = Objects.requireNonNull(document.get("price_before")).toString();
        double productPriceBefore = Double.parseDouble(priceBeforeString); // Product Price Before Discount
        String type = String.valueOf(document.get("type")); // Product Type


        product = new Product(productName, productPrice, type);
        tvName.setText(product.getName());
        tvType.setText(product.getType());

        if(productPrice < productPriceBefore){
            tvPrice.setText(String.format(Locale.getDefault(),"RM%.2f", productPriceBefore));
            tvPrice.setPaintFlags(tvPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvPriceAfter.setText(String.format(Locale.getDefault(),"RM%.2f", product.getPrice()));
        }else{
            tvPrice.setText(String.format(Locale.getDefault(),"RM%.2f", product.getPrice()));
        }
        // Reference to an image file in Firebase Storage
        String imageUrl = "gs://mobile-shopping-application.appspot.com/product/"
                + document.getId() + ".jpg";
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReferenceFromUrl(imageUrl);

        // Load the image using Glide
        Glide.with(getApplicationContext())
                .load(storageReference)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(imgViewProduct);

        //Check if the product is saved by user (favourite)
        sharedpreferences = getSharedPreferences("saved_product", Context.MODE_PRIVATE);
        isFavourited = sharedpreferences.contains(documentId);
        if(isFavourited){
            btnFavourite.setText("Remove from favourite");
        }else{
            btnFavourite.setText("Add to favourite");
        }
        loadingDialog.dismiss();
    }
}
