package com.example.idear;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;
import java.util.ArrayList;

//CODE DONE BY ELLIOT S., CLEANED/ADAPTED BY REECE R.
public class ListAdapter extends ArrayAdapter<Image>{
    private static final String TAG = "ArrayAdapter";
    private List<Image> PhotoCellList = new ArrayList<>();

    static class ViewHolder{
        ImageView photoView;
        TextView dateView;
        TextView wordLengthView;
    }

    public ListAdapter (Context context, int textViewID){
        super(context, textViewID);
    }

    @Override
    public void add (Image object){
        PhotoCellList.add(object);
    }

    @Override
    public int getCount(){
        return this.PhotoCellList.size();
    }

    @Override
    public View getView(int position, View ViewConvert, ViewGroup parent){
        View row = ViewConvert;
        ViewHolder viewHold;
        //ImageCell i = new ImageCell();
        if (row == null){
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //row = inflater.inflate(R.layout.listview_row_layout, parent, false);
            viewHold = new ViewHolder();
            //viewHold.photoView = (ImageView) row.findViewById(R.id.Image);
            //viewHold.dateView = (TextView) row.findViewById(R.id.Date);
            //viewHold.wordLengthView = (TextView) row.findViewById(R.id.WordLength);
            row.setTag(viewHold);
        }
        else{
            viewHold = (ViewHolder) row.getTag();
        }
        //getCount count = getItem(position);
        //viewHold.photoView.setImageResource(ImageCell.getImageFilePath());
        //viewHold.dateView.setText(ImageCell.getDate());
        //viewHold.wordLengthView.setText(ImageCell.getWordLength());
        return row;

    }

    public Bitmap decodeToBitmap(byte[] decodedByte){
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }

}


