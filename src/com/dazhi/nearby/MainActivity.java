package com.dazhi.nearby;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.dazhi.uitls.JsonUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private List<String> datas = new ArrayList<String>(8);
    private ArrayAdapter<String> arrayAdapter;
    private ListView listView;
    private ImageButton searchButton, settingButton, locatioButton;
    private TextView locationText;
    public static int TIME_OUT = 30 * 1000; // 30m

    // 百度定位请求的客户端类 baidu LocationClient类必须在主线程中声明。需要Context类型的参数。
    private LocationClient mLocClient;
    private MyLocationListener bdLocationListener;
    private BDLocation currentLocation;

    private String bigTypeName, middleTypeName;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bigTypeName = getIntent().getStringExtra("bigTypeName");
        middleTypeName = getIntent().getStringExtra("middleTypeName");

        // 中间ListView
        initBigTypeDatas();
        listView = (ListView) findViewById(R.id.type_listView);
        arrayAdapter = new MyArrayAdapter<String>(this, R.layout.main_activity_item_list, R.id.listView_text, datas);
        listView.setAdapter(arrayAdapter);

        // 定位和定位按钮
        locationText = (TextView) findViewById(R.id.current_location_text);
        locatioButton = (ImageButton) findViewById(R.id.btn_location);
        initLocation();
        locatioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLocation();
            }
        });

        // 设置按钮
        settingButton = (ImageButton) findViewById(R.id.btn_setting);
        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AbountActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });

        // 搜索按钮
        searchButton = (ImageButton) findViewById(R.id.btn_serach);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SearchRefreshActivity.class);
                intent.putExtra("currentLocation", currentLocation);
                MainActivity.this.startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        if (currentLocation == null) {
            startLocation();
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        stopLocation();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        //stopLocation();
        super.onPause();
    }

    /**
     * 初始化百度定位的 Location类
     */
    private void initLocation() {
        mLocClient = new LocationClient(getApplicationContext());

        // 设置定位参数,参考API http://developer.baidu.com/map/loc_refer/index.html
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//设置定位模式
        option.setCoorType("bd09ll");//返回的定位(坐标类型)结果是百度经纬度，默认值gcj02
        option.setScanSpan(10000);//设置发起定位请求的间隔时间为5000ms
        option.setIsNeedAddress(true);//返回的定位结果包含地址信息
        option.setNeedDeviceDirect(true);//返回的定位结果包含手机机头的方向
        mLocClient.setLocOption(option);

        bdLocationListener = new MyLocationListener();
        mLocClient.registerLocationListener(bdLocationListener);
    }

    /**
     * 请求开启定位
     */
    private void startLocation() {
        currentLocation = null;
        locationText.setText("" + "定位中，请稍等...");
        mLocClient.start(); // 开启地图服务
        if (mLocClient != null && mLocClient.isStarted()) {
            mLocClient.requestLocation();
        } else {
            Log.d("LocSDK3", "locClient is null or not started");
        }
    }

    /**
     * 停止定位
     */
    private void stopLocation() {
        if (mLocClient != null && mLocClient.isStarted()) {
            mLocClient.stop();
        }
    }


    private class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation bdLocation) { // 当接收到地图信息的时候，异步请求
            if (bdLocation != null) {
                String curAddress = bdLocation.getAddrStr(); // 地理位置
                if (curAddress != null && !curAddress.trim().isEmpty()) {
                    locationText.setText("" + curAddress);
                    currentLocation = bdLocation;
                    stopLocation();
                }
            }
            Log.d("BDMapDemo", "onReceiveLocation");
        }

        @Override
        public void onReceivePoi(BDLocation bdLocation) {
            Log.d("BDMapDemo", "onReceivePoi=" + bdLocation.getAltitude());
        }
    }

    // 读取分类代码：http://open.weibo.com/wiki/Location/category
    private void initBigTypeDatas() {
        JSONArray jsonArray = JsonUtils.getInstance().getBigTypeJsonArray(getApplicationContext());
        for (int i = 0; jsonArray != null && i < jsonArray.length(); ++i) {
            JSONObject obj = (JSONObject) jsonArray.opt(i);
            String bigTypeName = obj.opt("bigTypeName").toString();
            datas.add(bigTypeName);
            Log.d("bigTypeName value", i + ":" + bigTypeName);
        }
    }


    private class MyArrayAdapter<T> extends ArrayAdapter {

        public MyArrayAdapter(Context context, int resource, int textViewResourceId, List objects) {
            super(context, resource, textViewResourceId, objects);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            ImageButton btn = (ImageButton) view.findViewById(R.id.btn_location);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startMyActivity(position);
                }
            });
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startMyActivity(position);
                }
            });
            return view;
        }
    }

    private void startMyActivity(int position) {
        // 向二级菜单跳
        Intent intent = new Intent(MainActivity.this, MainSubMenuActivity.class);
        intent.putExtra("bigTypeName", datas.get(position));
        intent.putExtra("currentLocation", currentLocation);
        intent.putExtra("curType", datas.get(position));
        startActivity(intent);
    }
}
