package com.dazhi.searchCircum;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.baidu.location.BDLocation;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.search.*;
import com.baidu.platform.comapi.basestruct.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * 利用poi搜索功能
 */
public class SearchActivity extends Activity implements View.OnClickListener{
    private ImageButton  back, search;
    private BDLocation bdLocation;

    private MKSearch mSearch = null;   // 搜索模块，也可去掉地图模块独立使用
    private MyAdapter myBaseAdapter = null;
    private ArrayList<MKPoiInfo> poiInfos = new ArrayList<MKPoiInfo>();
    private ListView listView;
    private int load_Index;

    public SearchActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        back = (ImageButton) findViewById(R.id.btn_back);
        back.setOnClickListener(this);

        search = (ImageButton) findViewById(R.id.btn_serach);
        search.setOnClickListener(this);

        bdLocation = getIntent().getParcelableExtra("bdLocation");

        myBaseAdapter = new MyAdapter();
        listView = (ListView) findViewById(R.id.type_listView);
        listView.setAdapter(myBaseAdapter);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back:
                this.finish();
                // public void overridePendingTransition(int enterAnim, int exitAnim)
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                break;
            case R.id.btn_serach:
                // 进行搜索
                SearchActivity.this.searchButtonProcess(view);
                break;
        }
    }

    private class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return poiInfos.size();
        }

        @Override
        public Object getItem(int i) {
            return poiInfos.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            RelativeLayout layout = (RelativeLayout) convertView;

            if(position < this.getCount() - 1){
                // 重用组件
                if (convertView == null) {
                    layout = (RelativeLayout) getLayoutInflater().inflate(R.layout.search_activity_item_list,null, false);
                }
                TextView poi_name = (TextView) layout.findViewById(R.id.poi_name);
                TextView poi_address = (TextView) layout.findViewById(R.id.poi_address);
                TextView poi_distance = (TextView) layout.findViewById(R.id.poi_distance);

                MKPoiInfo poiInfo = poiInfos.get(position);
                poi_name.setText(poiInfo.name);
                poi_address.setText(poiInfo.address);

                GeoPoint point = poiInfo.pt;
                Geocoder geocoder = new Geocoder(SearchActivity.this);
            } else {// 如果是最后一个数据，进行特殊处理
                TextView textView = new TextView(SearchActivity.this);
                layout.addView(textView);
                layout.setGravity(RelativeLayout.CENTER_IN_PARENT);
            }

            return layout;
        }
    }

    /**
     * 影响搜索按钮点击事件
     * @param v
     */
    private void searchButtonProcess(View v) {
//        EditText editSearchKey = (EditText)findViewById(R.id.searchkey);
//        try {
////            geocoder.get
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void updateListView(ArrayList<MKPoiInfo> mKPoiInfos) {
        this.poiInfos.addAll(mKPoiInfos);
        myBaseAdapter.notifyDataSetChanged();
    }
    public void goToNextPage(View v) {
        //搜索下一组poi
        int flag = mSearch.goToPoiPage(++load_Index);
        if (flag != 0) {
            Toast.makeText(SearchActivity.this, "先搜索开始，然后再搜索下一组数据", Toast.LENGTH_SHORT).show();
        }
    }
}
