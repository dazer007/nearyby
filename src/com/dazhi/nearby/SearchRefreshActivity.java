package com.dazhi.nearby;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.baidu.location.BDLocation;
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
import java.util.*;

/**
 * 搜索周边信息的Activity，用法参加百度map demo :PoiSearchDemo.java
 * 使用poi搜索功能
 */
public class SearchRefreshActivity extends Activity implements View.OnClickListener {
    private ImageButton back, search;
    private EditText searchkey;
    private BDLocation currentLocation;

    private SimpleAdapter simpleAdapter;
    private ArrayList<Map<String, String>> datas = new ArrayList<Map<String, String>>();
    private PullToRefreshListView mPullRefreshListView;
    private ListView listView;
    private int load_Index = 1;
    private JSONObject rootJsonObject;

    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        back = (ImageButton) findViewById(R.id.btn_back);
        back.setOnClickListener(this);
        search = (ImageButton) findViewById(R.id.btn_serach);
        search.setOnClickListener(this);
        searchkey = (EditText) findViewById(R.id.searchkey);

        currentLocation = getIntent().getParcelableExtra("currentLocation");


        simpleAdapter = new MySimpleAdapter(this, datas, R.layout.search_activity_item_list, new String[]{"name","address","distance"},new int[]{R.id.poi_name, R.id.poi_address, R.id.poi_distance});
        mPullRefreshListView = (PullToRefreshListView) findViewById(R.id.type_listView);
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

        /**
         * Add Sound Event Listener
         */
        SoundPullEventListener<ListView> soundListener = new SoundPullEventListener<ListView>(this);
        soundListener.addSoundEvent(PullToRefreshBase.State.PULL_TO_REFRESH, R.raw.pull_event);
        soundListener.addSoundEvent(PullToRefreshBase.State.RESET, R.raw.reset_sound);
        soundListener.addSoundEvent(PullToRefreshBase.State.REFRESHING, R.raw.refreshing_sound);
        mPullRefreshListView.setOnPullEventListener(soundListener);

        listView = mPullRefreshListView.getRefreshableView();
        listView.setAdapter(simpleAdapter);
    }

    private class MySimpleAdapter extends SimpleAdapter {
        public MySimpleAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(SearchRefreshActivity.this, "wwww", Toast.LENGTH_SHORT).show();
                    displayMap();
                }
            });
            return v;
        }
    }

    private void displayMap() {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_back: // 返回主界面
                this.finish();
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                break;
            case R.id.btn_serach: // 进行搜索

                if (currentLocation == null) {
                    Toast.makeText(this, "没有数据", Toast.LENGTH_SHORT).show();
                } else {
                    datas.clear();
                    load_Index = 0;
                    this.searchPoiByAsycTask();
                }
                break;
        }
    }

    private void searchPoiByAsycTask() {

        String url = "https://api.weibo.com/2/location/pois/search/by_geo.json";
        load_Index++;

        ArrayList<NameValuePair> getParams = new ArrayList<NameValuePair>();
        getParams.add(new BasicNameValuePair("access_token", "2.00O2b7ECTKvEwD8507bbee2fHNrTqD"));
        getParams.add(new BasicNameValuePair("coordinate", currentLocation.getLongitude() + "," + currentLocation.getLatitude()));
        getParams.add(new BasicNameValuePair("range", "5000"));
        getParams.add(new BasicNameValuePair("count", "20"));
        getParams.add(new BasicNameValuePair("page", load_Index + ""));
        Log.d(getClass().getName(), "q:" + searchkey.getText().toString());
        getParams.add(new BasicNameValuePair("q", searchkey.getText().toString()));


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
                    dialog = new ProgressDialog(SearchRefreshActivity.this);
                    dialog.setMessage("正在加载，请稍等");
                    dialog.setCancelable(false);
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
                } catch (NullPointerException e) {
                    Log.e(getClass().getName(), e.getMessage(), e);
                    return ERROR_NoMoreData;
                }

                return 0;
            }

            @Override
            protected void onPostExecute(Integer integer) {

                dialog.dismiss();

                if (integer == ERROR_IOEXCEPTION) {
                    Toast.makeText(SearchRefreshActivity.this, "网络错误，请稍后重试", Toast.LENGTH_SHORT).show();
                    return;
                } else if (integer == ERROR_JSONException || integer == ERROR_NoMoreData) {
                    Toast.makeText(SearchRefreshActivity.this, "没有数据了", Toast.LENGTH_SHORT).show();
                    // Call onRefreshComplete when the list has been refreshed.
                    mPullRefreshListView.onRefreshComplete();
                    return;
                }

                displayPoi();

            }
        };

        task.execute(0);
    }

    private void displayPoi() {

        JSONArray poilistJsonArray = rootJsonObject.optJSONArray("poilist");
        Map<String, String> map = null;
        for (int i = 0; i < poilistJsonArray.length(); i++) {
            JSONObject poiJsonObject = poilistJsonArray.optJSONObject(i);


            String name = poiJsonObject.optString("name");
            String address = poiJsonObject.optString("address");

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


            map = new HashMap<String, String>();
            map.put("name", name);
            map.put("address", address);
            map.put("distance", distanceStr);
            datas.add(map);
        }


        // Call onRefreshComplete when the list has been refreshed.
        mPullRefreshListView.onRefreshComplete();
        simpleAdapter.notifyDataSetChanged();

    }

    /**
     * google maps的脚本里代码
     */
    private static double EARTH_RADIUS = 6378.137;

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    /**
     * 根据两点间经纬度坐标（double值），计算两点间距离，单位为米
     */
    public static double getDistance(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lng1) - rad(lng2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
                Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 1000);
        return s;
    }

}
