package com.example.idear;
import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class ListViewActivity extends Activity {
    private static final String TAG = "ListViewActivity";

    private ListAdapter listAdapter;
    private ListView listView;

    private static int index;

    @Override
    public void onCreate(Bundle saveState){
        super.onCreate(saveState);
        //setContentView(R.layout.listview_layout);
        index = 0;
        //listView= (ListView) findViewById(R.id.listView);
        //listViewActivity = new ListViewActivity(getApplicationContext(),R.layout.listview_row_layout);
        //listView.setAdapter(listViewActivity);

    }
}
