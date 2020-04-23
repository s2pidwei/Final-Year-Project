package com.example.finalyearproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class ScannedResultActivity extends AppCompatActivity {

    TextView tvResult;
    ListView lvResults;
    ArrayList<DetectedProduct> detectedProducts;
    ProductListAdapter adapter;
    ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanned_result);
        tvResult = findViewById(R.id.tvResult);
        lvResults = findViewById(R.id.lvResults);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        //get result from scanning
        Intent intent = getIntent();
        String method = intent.getStringExtra("method");

        String[] productNames;
        final float[] confidence;

        detectedProducts = new ArrayList<>();

        switch (method) {
            case "detect":
                getResults("Detected Results:");
                ArrayList<String> checkDuplicate = new ArrayList<>();
                //Apply loading dialog
                loadingDialog = ProgressDialog.show(ScannedResultActivity.this, "",
                        "Retrieving Data. Please wait...", true);

                Bundle detectedProduct = getIntent().getBundleExtra("detectedProduct");
                productNames = detectedProduct.getStringArray("name");
                confidence = detectedProduct.getFloatArray("confidence");

                if (productNames != null && confidence != null && productNames.length > 0 && confidence.length > 0) {
                    for (int i = 0; i < productNames.length; i++) {
                        final String item_name = productNames[i];
                        final float confidenceLvl = confidence[i];
                        if(!checkDuplicate.contains(item_name)) {
                            getProductFromFirebase(item_name,confidenceLvl);
                            checkDuplicate.add(item_name);
                        }
                    }

                    adapter = new ProductListAdapter(detectedProducts, getApplicationContext());
                    lvResults.setAdapter(adapter);
                    lvResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            String productId = detectedProducts.get(position).getProductId();
                            Intent intent = new Intent(getApplicationContext(), ProductDetails.class);
                            intent.putExtra("productId",productId);
                            startActivity(intent);
                        }
                    });
                }
                break;
            case "qr": {
                String result = intent.getStringExtra("result");
                //see result
                getResults(result);
                break;
            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }

    private void getProductFromFirebase(final String item_name, final float confidenceLvl) {
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
                                Product product = new Product();
                                String productName = String.valueOf(document.get("name"));
                                product.setName(productName);
                                detectedProducts.add(new DetectedProduct(product, confidenceLvl, item_name));
                                adapter.notifyDataSetChanged(); //Reload list view
                                loadingDialog.dismiss(); //Close loading dialog
                            }
                        } else {
                            Log.d("TAG_ERROR", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private void getResults(String result){
        tvResult.setText(result);
    }

}
