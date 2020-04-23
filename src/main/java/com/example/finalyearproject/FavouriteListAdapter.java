package com.example.finalyearproject;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class FavouriteListAdapter extends ArrayAdapter<Product> {
    private ArrayList<Product> dataSet;
    private Context mContext;

    private static class ViewHolder {
        TextView tvFavouriteNames;
        TextView tvPricePreview;
        TextView tvPriceAfterPreview;
        ImageView imgProductPreview;
    }

    public FavouriteListAdapter(ArrayList<Product> data, Context context) {
        super(context, R.layout.favourite_listview, data);
        this.dataSet = data;
        this.mContext=context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position

        Product favouriteProducts = dataSet.get(position);
        // Check if an existing view is being reused, otherwise inflate the view
        FavouriteListAdapter.ViewHolder viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {

            viewHolder = new FavouriteListAdapter.ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.favourite_listview, parent, false);
            viewHolder.tvFavouriteNames = convertView.findViewById(R.id.tvFavouriteNames);
            viewHolder.tvPricePreview = convertView.findViewById(R.id.tvPricePreview);
            viewHolder.tvPriceAfterPreview = convertView.findViewById(R.id.tvPriceAfterPreview);
            viewHolder.imgProductPreview = convertView.findViewById(R.id.imgProductPreview);

            result = convertView;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (FavouriteListAdapter.ViewHolder) convertView.getTag();
            result = convertView;
        }

        viewHolder.tvFavouriteNames.setText(favouriteProducts.getName());
        if(favouriteProducts.getPrice() < favouriteProducts.getPriceBefore()) {
            viewHolder.tvPricePreview.setText(String.format(Locale.getDefault(), "RM %.2f", favouriteProducts.getPriceBefore()));
            viewHolder.tvPricePreview.setPaintFlags(viewHolder.tvPricePreview.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            viewHolder.tvPriceAfterPreview.setText(String.format(Locale.getDefault(), "RM %.2f", favouriteProducts.getPrice()));
        }else{
            viewHolder.tvPricePreview.setText(String.format(Locale.getDefault(), "RM %.2f", favouriteProducts.getPrice()));
        }

        // Reference to an image file in Firebase Storage
        String imageUrl = "gs://mobile-shopping-application.appspot.com/product/"
                + favouriteProducts.getId() + ".jpg";
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReferenceFromUrl(imageUrl);
        // Load the image using Glide
        Glide.with(getContext())
                .load(storageReference)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(viewHolder.imgProductPreview);

        // Return the completed view to render on screen
        return convertView;
    }
}
