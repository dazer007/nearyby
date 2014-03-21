package com.dazhi.nearby;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import com.baidu.location.BDLocation;
import com.baidu.mapapi.map.LocationData;
import com.baidu.mapapi.map.MapController;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationOverlay;
import com.baidu.platform.comapi.basestruct.GeoPoint;

public class BDMapAcitivity extends Activity implements View.OnClickListener{
    private ImageButton back;
    private Button foot, bus, car;
    private MapView mapView;
    private MapController mapController;
    private MyLocationOverlay myLocationOverlay;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_going);

        back = (ImageButton) findViewById(R.id.btn_back);
        back.setOnClickListener(this);

        foot = (Button) findViewById(R.id.foot);
        bus = (Button) findViewById(R.id.bus);
        car = (Button) findViewById(R.id.car);
        foot.setOnClickListener(this);
        bus.setOnClickListener(this);
        car.setOnClickListener(this);

        foot.setBackgroundResource(R.drawable.bg_tab_top);
        car.setBackgroundColor(Color.WHITE);
        bus.setBackgroundColor(Color.WHITE);

        mapView = (MapView) findViewById(R.id.mapview);
        mapController = mapView.getController();

        BDLocation currentLocation = getIntent().getParcelableExtra("currentLocation");
        addMyLocationOverlay(currentLocation);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.foot:
                foot.setBackgroundResource(R.drawable.bg_tab_top);
                car.setBackgroundColor(Color.WHITE);
                bus.setBackgroundColor(Color.WHITE);
            case R.id.car:
                car.setBackgroundResource(R.drawable.bg_tab_top);
                foot.setBackgroundColor(Color.WHITE);
                bus.setBackgroundColor(Color.WHITE);
            case R.id.bus:
                bus.setBackgroundResource(R.drawable.bg_tab_top);
                car.setBackgroundColor(Color.WHITE);
                foot.setBackgroundColor(Color.WHITE);
                break;

            case R.id.btn_back:
                finish();
                break;
        }
    }

    private void addMyLocationOverlay(BDLocation bdLocation) {

        setTitle(bdLocation.getAddrStr());

        //让地图中心点移动
        GeoPoint geoPoint = new GeoPoint((int) (bdLocation.getLatitude() * 1E6), (int) (bdLocation.getLongitude() * 1E6));
        //mapController.setCenter(geoPoint);
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

        //添加图层
        mapView.getOverlays().add(myLocationOverlay);

        mapView.refresh();
    }
}
