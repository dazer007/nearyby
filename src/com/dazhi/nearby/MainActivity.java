package com.dazhi.nearby;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private List<String> datas = new ArrayList<String>(8);
    private ArrayAdapter<String> arrayAdapter;
    private ListView listView;
    private ImageButton searchButton, settingButton, locatioButton;
    private TextView locationText;

    // 百度定位请求的客户端类 baidu LocationClient类必须在主线程中声明。需要Context类型的参数。
    private LocationClient mLocClient;
    private MyLocationListener bdLocationListener;
    private BDLocation currentLocation;

    private ProgressDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 中间ListView
        initDatas();
        listView = (ListView) findViewById(R.id.type_listView);
        arrayAdapter = new ArrayAdapter<String>(this, R.layout.main_activity_item_list, R.id.listView_text, datas);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "" + datas.get(position), Toast.LENGTH_SHORT).show();
            }
        });

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
//                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                Intent intent = new Intent(MainActivity.this, SearchRefreshActivity.class);
                intent.putExtra("currentLocation", currentLocation);
                MainActivity.this.startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        startLocation();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        stopLocation();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        stopLocation();
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
        if(dialog != null) { dialog.dismiss();}
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

    private void initDatas() {
        datas.add("餐饮服务");
        datas.add("购物服务");
        datas.add("生活服务");
        datas.add("体育休闲服务");
        datas.add("医疗保健服务");
        datas.add("住宿服务");
        datas.add("科技文化服务");
        datas.add("交通设施服务");
    }

}
