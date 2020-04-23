package com.example.finalyearproject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Locale;

public class ProductListAdapter extends ArrayAdapter<DetectedProduct>{
    private ArrayList<DetectedProduct> dataSet;
    private Context mContext;

    private static class ViewHolder {
        TextView txtName;
        TextView txtConfidence;
        ImageView imgViewPreview;
    }

    //public ProductListAdapter(ArrayList<Product> data, Context context) {
    public ProductListAdapter(ArrayList<DetectedProduct> data, Context context) {
        super(context, R.layout.result_listview, data);
        this.dataSet = data;
        this.mContext=context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            DetectedProduct detectedProducts = dataSet.get(position);
            // Check if an existing view is being reused, otherwise inflate the view
            ViewHolder viewHolder; // view lookup cache stored in tag

            final View result;

            if (convertView == null) {

                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.result_listview, parent, false);
                viewHolder.txtName = convertView.findViewById(R.id.tvResultNames);
                viewHolder.txtConfidence = convertView.findViewById(R.id.tvResultConfidence);
                viewHolder.imgViewPreview = convertView.findViewById(R.id.imgViewPreview);

                result = convertView;

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
                result = convertView;
            }
            float confidencePercent = (detectedProducts.getConfidenceLvl() * 100);
            viewHolder.txtName.setText(detectedProducts.getProduct().getName());
            viewHolder.txtConfidence.setText(String.format(Locale.getDefault(), "%.2f %%", confidencePercent));


            // Reference to an image file in Firebase Storage
            String imageUrl = "gs://mobile-shopping-application.appspot.com/product/"
                    + detectedProducts.getProductId() + ".jpg";
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageReference = storage.getReferenceFromUrl(imageUrl);
            // Load the image using Glide
            Glide.with(getContext())
                    .load(storageReference)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(viewHolder.imgViewPreview);
        // Return the completed view to render on screen
        return convertView;
    }
}
