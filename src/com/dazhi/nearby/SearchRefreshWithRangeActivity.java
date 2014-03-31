package com.dazhi.nearby;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.baidu.location.BDLocation;
import com.baidu.mapapi.map.*;
import com.baidu.platform.comapi.basestruct.GeoPoint;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.handmark.pulltorefresh.library.extras.SoundPullEventListener;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.dazhi.uitls.MapUitls.getDistance;

/**
 * 搜索周边信息的Activity，用法参加百度map demo :PoiSearchDemo.java
 * 使用poi搜索功能,指定搜索范围
 */
public class SearchRefreshWithRangeActivity extends Activity implements View.OnClickListener {
    private ImageButton back, btn_refresh, btn_action;
    private TextView textView;
    private Spinner spinner;
    private BDLocation currentLocation;

    private PoiListAdatpter myBaseAdapter = null;
    private ArrayList<Map<String, Object>> datas = new ArrayList<Map<String, Object>>();
    private PullToRefreshListView mPullRefreshListView;
    private ListView listView;
    private LinearLayout root;
    private int load_Index = 1;
    private JSONObject rootJsonObject;


    private ProgressDialog dialog;
    private String searchkey;
    private int range;
    private int[] ranges = {10000, 9000, 8000, 7000, 6000, 5000, 4000, 3000, 2000, 1000};
    private boolean actionFlag = true;


    private MapView mapView;
    private MapController mapController;
    private MyLocationOverlay myLocationOverlay; // 当前位置的覆盖层

    private PoiOverlay poiOverlay;
    private PopupOverlay popupOverlay;
    private int selectedPoiItemIndex = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_range);

        textView = (TextView) findViewById(R.id.searchkey);
        searchkey = getIntent().getStringExtra("curType");
        textView.setText("" + searchkey);

        spinner = (Spinner) findViewById(R.id.select_range);
        spinner.setSelection(7);
        range = ranges[7];
        spinner.setOnItemSelectedListener(new OnSpinnerItemSelectedImpl());

        currentLocation = getIntent().getParcelableExtra("currentLocation");

        back = (ImageButton) findViewById(R.id.btn_back);
        back.setOnClickListener(this);
        btn_refresh = (ImageButton) findViewById(R.id.btn_refresh);
        btn_refresh.setOnClickListener(this);
        btn_action = (ImageButton) findViewById(R.id.btn_action);
        btn_action.setOnClickListener(this);

        root = (LinearLayout) findViewById(R.id.root);

        this.loadListView();

    }

    private void loadListView() {
        root.removeAllViews();
        getLayoutInflater().inflate(R.layout.layout_listview, root, true);
        mPullRefreshListView = (PullToRefreshListView) root.findViewById(R.id.type_listView);

        myBaseAdapter = new PoiListAdatpter();

        // Set a listener to be invoked when the list should be refreshed.
        mPullRefreshListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                String label = DateUtils.formatDateTime(getApplicationContext(), System.currentTimeMillis(),
                        DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);

                // Update the LastUpdatedLabel
                refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(label);

                // Do work to refresh the list here.
                searchPoiByAsycTask();
            }
        });

        mPullRefreshListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i2, int i3) {
                mPullRefreshListView.getLoadingLayoutProxy().setRefreshingLabel("正在加载");
                mPullRefreshListView.getLoadingLayoutProxy().setPullLabel("上拉加载更多");
                mPullRefreshListView.getLoadingLayoutProxy().setReleaseLabel("释放开始加载");

            }
        });

        listView = mPullRefreshListView.getRefreshableView();
        listView.setAdapter(myBaseAdapter);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back: // 返回上一层界面
                this.finish();
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                break;
            case R.id.btn_refresh: // 刷新
                toRefresh();
                break;
            case R.id.btn_action: // 查询结果展示在地图中

                if (actionFlag) {
                    btn_action.setImageResource(R.drawable.ic_action_map);
                    initMapView();
                } else {
                    btn_action.setImageResource(R.drawable.ic_action_list);
                    loadListView();
                }
                actionFlag = !actionFlag;

                break;
        }
    }

    private void startSearch() {
        if (currentLocation != null) {
            searchPoiByAsycTask();
        } else {
            Toast.makeText(this, "没有数据", Toast.LENGTH_SHORT).show();
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }

    private void toRefresh() { // 刷新操作
        datas.clear();
        load_Index = 0;
        startSearch();
        if (!actionFlag ) {
            displayMapPOI();
        }
    }

    private void searchPoiByAsycTask() {
        String url = "https://api.weibo.com/2/location/pois/search/by_geo.json";
        load_Index++;

        ArrayList<NameValuePair> getParams = new ArrayList<NameValuePair>();
        getParams.add(new BasicNameValuePair("access_token", "2.00O2b7ECTKvEwD8507bbee2fHNrTqD"));
        getParams.add(new BasicNameValuePair("coordinate", currentLocation.getLongitude() + "," + currentLocation.getLatitude()));
        getParams.add(new BasicNameValuePair("range", range + ""));
        getParams.add(new BasicNameValuePair("count", "20"));
        getParams.add(new BasicNameValuePair("page", load_Index + ""));
        Log.d(getClass().getName(), "q:" + searchkey);
        getParams.add(new BasicNameValuePair("q", searchkey));


        String getParamsStr = URLEncodedUtils.format(getParams, "UTF-8");

        Log.d(getClass().getName(), "getParamsStr " + getParamsStr);

        url += "?" + getParamsStr;

        final HttpGet request = new HttpGet(url);

        final DefaultHttpClient client = new DefaultHttpClient();

        // httpClient参数设置，设置超时时间
        HttpParams httpParameters = new BasicHttpParams();
        // Sets the timeout until a connection is etablished.
        HttpConnectionParams.setConnectionTimeout(httpParameters, MainActivity.TIME_OUT);
        // Sets the default socket timeout (SO_TIMEOUT) in milliseconds which is the timeout for waiting for data.
        HttpConnectionParams.setSoTimeout(httpParameters, MainActivity.TIME_OUT);
        client.setParams(httpParameters);

        AsyncTask<Integer, Integer, Integer> task = new AsyncTask<Integer, Integer, Integer>() {
            private static final int ERROR_IOEXCEPTION = 1;
            private static final int ERROR_JSONException = 2;
            private static final int ERROR_NoMoreData = 3;

            @Override
            protected void onPreExecute() {
                if (datas.isEmpty()) {
                    dialog = new ProgressDialog(SearchRefreshWithRangeActivity.this);
                    dialog.setMessage("正在加载，请稍等");
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();
                }
                super.onPreExecute();
            }

            @Override
            protected Integer doInBackground(Integer... params) {
                try {
                    HttpResponse response = client.execute(request);


                    String resultJsonStr = EntityUtils.toString(response.getEntity());

                    Log.d(getClass().getName(), "Response json " + resultJsonStr);

                    rootJsonObject = new JSONObject(resultJsonStr);

                    if (rootJsonObject.optJSONArray("poilist") == null) {
                        throw new NullPointerException("没有数据了");
                    }

                } catch (IOException e) {
                    Log.e(getClass().getName(), e.getMessage(), e);
                    return ERROR_IOEXCEPTION;
                } catch (JSONException e) {
                    Log.e(getClass().getName(), e.getMessage(), e);
                    return ERROR_JSONException;
                } catch (Exception e) {
                    Log.e(getClass().getName(), e.getMessage(), e);
                    return ERROR_NoMoreData;
                }

                return 0;
            }

            @Override
            protected void onPostExecute(Integer integer) {

                dialog.dismiss();
                // Call onRefreshComplete when the list has been refreshed.
                mPullRefreshListView.onRefreshComplete();

                if (integer == ERROR_IOEXCEPTION) {
                    Toast.makeText(SearchRefreshWithRangeActivity.this, "网络错误，请稍后重试", Toast.LENGTH_SHORT).show();
                    return;
                } else if (integer == ERROR_JSONException || integer == ERROR_NoMoreData) {
                    Toast.makeText(SearchRefreshWithRangeActivity.this, "亲，没有数据了", Toast.LENGTH_SHORT).show();
                    return;
                }

                displayPoi();

            }
        };

        task.execute(0);
    }

    private void displayPoi() {

        JSONArray poilistJsonArray = rootJsonObject.optJSONArray("poilist");
        Map<String, Object> map = null;
        for (int i = 0; i < poilistJsonArray.length(); i++) {
            JSONObject poiJsonObject = poilistJsonArray.optJSONObject(i);


            String name = poiJsonObject.optString("name");
            String address = poiJsonObject.optString("address");
            String citycode = poiJsonObject.optString("citycode"); // 城市编码
            String tel = poiJsonObject.optString("tel");

            double longitude = poiJsonObject.optDouble("x");
            double latitude = poiJsonObject.optDouble("y");
            // 距离单位M（米）
            double distance = getDistance(latitude, longitude, currentLocation.getLatitude(), currentLocation.getLongitude());
            String distanceStr = "";
            if (distance > 1000) {
                distanceStr = distance / 1000 + "km";
            } else {
                distanceStr = distance + "m";
            }


            map = new HashMap<String, Object>();
            map.put("name", name);
            map.put("address", address);
            map.put("distance", distanceStr);
            map.put("citycode", citycode);
            map.put("tel", tel);
            map.put("x",longitude);
            map.put("y",latitude);

            BDLocation bdLocation = new BDLocation();
            bdLocation.setLatitude(latitude);
            bdLocation.setLongitude(longitude);
            map.put("bdLocation", bdLocation);
            datas.add(map);
        }

        // Call onRefreshComplete when the list has been refreshed.
        mPullRefreshListView.onRefreshComplete();
        myBaseAdapter.notifyDataSetChanged();

    }

    private class PoiListAdatpter extends BaseAdapter {

        @Override
        public int getCount() {
            return datas.size();
        }

        @Override
        public Object getItem(int i) {
            return datas.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            RelativeLayout layout = (RelativeLayout) convertView;
            // 重用组件
            if (convertView == null) {
                layout = (RelativeLayout) getLayoutInflater().inflate(R.layout.search_activity_item_list, null, false);
            }


            Map<String, Object> currentLineMap = datas.get(position);

            TextView poi_name = (TextView) layout.findViewById(R.id.poi_name);
            TextView poi_address = (TextView) layout.findViewById(R.id.poi_address);
            TextView poi_distance = (TextView) layout.findViewById(R.id.poi_distance);

            poi_name.setText(currentLineMap.get("name").toString());
            poi_address.setText(currentLineMap.get("address").toString());
            poi_distance.setText(currentLineMap.get("distance").toString());

        	layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                	 Intent intent = new Intent(SearchRefreshWithRangeActivity.this, BDMapAcitivity.class);
                     intent.putExtra("currentLocation", currentLocation);
                     intent.putExtra("searchLocation",(Parcelable) (datas.get(position).get("bdLocation")));
                     intent.putExtra("distance", datas.get(position).get("distance").toString());
                     intent.putExtra("poiName", datas.get(position).get("name").toString());
                     intent.putExtra("address", datas.get(position).get("address").toString());
                     intent.putExtra("citycode", datas.get(position).get("citycode").toString());
                     intent.putExtra("tel", datas.get(position).get("tel").toString());
                     intent.putExtra("x", (Double) datas.get(position).get("x"));
                     intent.putExtra("y", (Double) datas.get(position).get("y"));
                     startActivity(intent);
                }
            });
            return layout;
        }
    }

    private class OnSpinnerItemSelectedImpl implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            range = ranges[position];
            load_Index = 0;
            datas.clear();
            startSearch();
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }



    private void initMapView() {
        root.removeAllViews();
        getLayoutInflater().inflate(R.layout.layout_bdmap, root, true);
        mapView = (MapView) root.findViewById(R.id.mapview);
        //设置启用内置的缩放控件
        mapController = mapView.getController();

        popupOverlay = new   PopupOverlay(mapView, new PopupClick()); // 弹出窗口，实例化，不用添加
        poiOverlay = new PoiOverlay(getResources().getDrawable(R.drawable.ic_loc_normal), mapView);
        mapView.getOverlays().add(poiOverlay);

        mapController.setZoom(14);


        displayMapPOI();
    }

    /**
     * 显示所有的数据在地图中
     */
    private void displayMapPOI() {


        if (currentLocation != null) {
            displayMyLocation(currentLocation);
        }
        displayAllPoiPoint();
    }

    private class PopupClick implements PopupClickListener {

        @Override
        public void onClickedPopup(int i) {
            Toast.makeText(SearchRefreshWithRangeActivity.this, "", Toast.LENGTH_LONG).show();
        }
    }

    private void displayMyLocation(BDLocation bdLocation) {

        currentLocation = bdLocation;

        Log.d(getClass().getName(), "Current location, " + bdLocation.getLatitude() + " " + bdLocation.getLongitude());

        //让地图中心点移动
        GeoPoint geoPoint = new GeoPoint((int) (bdLocation.getLatitude() * 1E6), (int) (bdLocation.getLongitude() * 1E6));

        mapController.setCenter(geoPoint);

        mapController.animateTo(geoPoint);

        //添加当前位置覆盖物
        if (myLocationOverlay != null) {
            mapView.getOverlays().remove(myLocationOverlay);
        }

        myLocationOverlay = new MyLocationOverlay(mapView);
        LocationData locationData = new LocationData();
        locationData.latitude = bdLocation.getLatitude();
        locationData.longitude = bdLocation.getLongitude();
        myLocationOverlay.setData(locationData);

        //添加自己位置的图层
        mapView.getOverlays().add(myLocationOverlay);
        mapView.refresh();

    }

    private void displayAllPoiPoint() {

        poiOverlay.removeAll();

        for (int i = 0; i < datas.size(); ++i) {
            Map<String, Object> map = datas.get(i);
            double longitude = (Double) map.get("x");
            double latitude = (Double) map.get("y");
            String name = map.get("name").toString();
            String address = map.get("address").toString();

            Log.d("xxxxx", "longitude:" + longitude);

            GeoPoint p = new GeoPoint((int) (latitude * 1E6), (int) (longitude * 1E6));

            OverlayItem item = new OverlayItem(p, name, address);

            item.setMarker(getResources().getDrawable(R.drawable.ic_loc_normal));
            poiOverlay.addItem(item);
        }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mapController.zoomToSpan(poiOverlay.getLatSpanE6(), poiOverlay.getLonSpanE6());
                mapController.animateTo(poiOverlay.getCenter());
            }
        }, 1);



        mapView.refresh();
    }

    @Override
    protected void onDestroy() {

        if (mapView != null) {
            mapView.destroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {

        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {

        if (mapView != null) {

            mapView.onResume();
        }
        super.onResume();
    }


    class PoiOverlay extends ItemizedOverlay {

        public PoiOverlay(Drawable drawable, MapView mapView) {
            super(drawable, mapView);
        }

        @Override
        public boolean onTap(GeoPoint geoPoint, MapView mapView) {
            Toast.makeText(SearchRefreshWithRangeActivity.this, "PoiOverlay onTap", Toast.LENGTH_SHORT).show();
            if (popupOverlay != null ) {
                popupOverlay.hidePop();
            }
            return super.onTap(geoPoint, mapView);
        }

        @Override
        protected boolean onTap(int i) {
            selectedPoiItemIndex = i;
            //Toast.makeText(MainActivity.this, "PoiOverlay onTap at " + i, Toast.LENGTH_SHORT).show();
            OverlayItem item = (OverlayItem) poiOverlay.getItem(i);
            View popup = getLayoutInflater().inflate(R.layout.popup, null);
             //popup.setBackgroundResource(R.drawable.placemark_transportation_noarrow);
            TextView titleTextView = (TextView) popup.findViewById(R.id.titleTextView);
            titleTextView.setText(item.getTitle());

            TextView snippetTextView = (TextView) popup.findViewById(R.id.snippetTextView);
            snippetTextView.setText(item.getSnippet());

            popupOverlay.showPopup(popup, item.getPoint(), item.getMarker().getIntrinsicHeight());

            return super.onTap(i);
        }
    }
}
