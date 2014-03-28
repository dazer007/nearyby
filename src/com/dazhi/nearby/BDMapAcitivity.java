package com.dazhi.nearby;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import com.baidu.location.BDLocation;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.map.*;
import com.baidu.mapapi.search.*;
import com.baidu.platform.comapi.basestruct.GeoPoint;

import java.util.ArrayList;

public class BDMapAcitivity extends Activity implements View.OnClickListener {
    private ImageButton back;
    private Button foot, bus, car;
    private ImageButton go;
    private MapView mMapView;
    private MapController mapController;
    private MyLocationOverlay myLocationOverlay;
    private MKSearch mkSearch;
    private RouteOverlay routeOverlay;
    private TransitOverlay transitOverlay;
    private MKPlanNode startNode, endNode;
    private BDLocation currentLocation, searchLocation;
    private ProgressDialog dialog;

    private static final int MODE_WALKING = 1;
    private static final int MODE_TRANSIT = 2;
    private static final int MODE_DRIVING = 3;
    private boolean routeFlag = false;
    private int routeMode = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /**
         * 使用地图sdk前需先初始化BMapManager.
         * BMapManager是全局的，可为多个MapView共用，它需要地图模块创建前创建，
         * 并在地图地图模块销毁后销毁，只要还有地图模块在使用，BMapManager就不应该销毁
         */
        BaiduMapApplication app = (BaiduMapApplication) this.getApplication();
        if (app.mBMapManager == null) {
            app.mBMapManager = new BMapManager(getApplicationContext());
            /**
             * 如果BMapManager没有初始化则初始化BMapManager
             */
            app.mBMapManager.init(BaiduMapApplication.strKey, new BaiduMapApplication.MyGeneralListener());
        }
        setContentView(R.layout.activity_going);

        dialog = new ProgressDialog(this);
        dialog.setMessage("路径规划中...");

        currentLocation = getIntent().getParcelableExtra("currentLocation");
        GeoPoint startPoint = new GeoPoint((int) (currentLocation.getLatitude() * 1E6), (int) (currentLocation.getLongitude() * 1E6));
        startNode = new MKPlanNode();
        startNode.pt = startPoint;

        searchLocation = getIntent().getParcelableExtra("searchLocation");
        GeoPoint endPoint = new GeoPoint((int) (searchLocation.getLatitude() * 1E6), (int) (searchLocation.getLongitude() * 1E6));
        endNode = new MKPlanNode();
        endNode.pt = endPoint;

        back = (ImageButton) findViewById(R.id.btn_back);
        back.setOnClickListener(this);

        foot = (Button) findViewById(R.id.foot);
        bus = (Button) findViewById(R.id.bus);
        car = (Button) findViewById(R.id.car);
        foot.setOnClickListener(this);
        bus.setOnClickListener(this);
        car.setOnClickListener(this);

        go = (ImageButton) findViewById(R.id.btn_go);
        go.setOnClickListener(this);

        //初始化地图
        mMapView = (MapView) findViewById(R.id.bmapview);
        mMapView.getController().enableClick(true);
        mMapView.getController().setZoom(13);
        mapController = mMapView.getController();

        addMyLocationOverlay(currentLocation);

        mkSearch = new MKSearch();
        mkSearch.init(BaiduMapApplication.getInstance().mBMapManager, new MKSearchListener() {
            @Override
            public void onGetPoiResult(MKPoiResult mkPoiResult, int i, int i2) {
            }

            @Override
            public void onGetTransitRouteResult(MKTransitRouteResult mkTransitRouteResult, int i) {
                if (routeFlag) {
                    displayTransiteRoute(mkTransitRouteResult, i); // 公交路线
                } else {
                    try {
                        addTransitePath(mkTransitRouteResult, i);
                    } catch (Exception e) {
                        Log.e(getClass().getName(), e.getMessage(), e);
                    }
                }
                dialog.dismiss();
            }

            @Override
            public void onGetDrivingRouteResult(MKDrivingRouteResult mkDrivingRouteResult, int i) {
                if (routeFlag) {
                    displayDrivingRoute(mkDrivingRouteResult, i); // 驾车路线
                } else {
                    addDrivingRath(mkDrivingRouteResult, i); // 驾车路线
                }
                dialog.dismiss();
            }

            @Override
            public void onGetWalkingRouteResult(MKWalkingRouteResult mkWalkingRouteResult, int i) {
                if (routeFlag) {
                    displayWalingRoute(mkWalkingRouteResult, i); // 步行路线
                } else {
                    addWalingPath(mkWalkingRouteResult, i); // 步行路线
                }
                dialog.dismiss();
                Log.d(getClass().getName(), "onGetWalkingRouteResult " + mkWalkingRouteResult);
            }

            @Override
            public void onGetAddrResult(MKAddrInfo mkAddrInfo, int i) {
            }

            @Override
            public void onGetBusDetailResult(MKBusLineResult mkBusLineResult, int i) {

            }

            @Override
            public void onGetSuggestionResult(MKSuggestionResult mkSuggestionResult, int i) {
            }

            @Override
            public void onGetPoiDetailSearchResult(int i, int i2) {
            }

            @Override
            public void onGetShareUrlResult(MKShareUrlResult mkShareUrlResult, int i, int i2) {
            }
        });


        //模拟执行点击
        foot.performClick();
    }

    private void addDrivingRath(MKDrivingRouteResult res, int error) {
        //起点或终点有歧义，需要选择具体的城市列表或地址列表
        if (error == MKEvent.ERROR_ROUTE_ADDR) {
            return;
        }
        //清除其他图层
        clearOverlay();

        // 错误号可参考MKEvent中的定义
        if (error != 0 || res == null) {
            Toast.makeText(this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
            return;
        }

        /** 演示自定义路线使用方法
         *  在北京地图上画一个北斗七星
         *  想知道某个点的百度经纬度坐标请点击：http://api.map.baidu.com/lbsapi/getpoint/index.html
         */
        ArrayList<ArrayList<GeoPoint>> geoPoints = res.getPlan(0).getRoute(0).getArrayPoints();
        ArrayList<GeoPoint> startPoints = geoPoints.get(0);
        ArrayList<GeoPoint> enddPoints = geoPoints.get(geoPoints.size() - 1);
        //计算所有的点数
        int size = 0;
        for (ArrayList<GeoPoint> arrs : geoPoints) {
            size += arrs.size();
        }
        //起点坐标
        GeoPoint start = startPoints.get(0);
        //终点坐标
        GeoPoint stop = enddPoints.get(enddPoints.size() - 1);
        //站点数据保存在一个二维数据中
        ArrayList<GeoPoint> allList = new ArrayList<GeoPoint>(size);
        GeoPoint[][] routeData = new GeoPoint[1][];
        //获取所有的点
        for (int i = 0; i < geoPoints.size() - 1; ++i) {
            allList.addAll(geoPoints.get(i));
        }
        routeData[0] = new GeoPoint[size];
        allList.toArray(routeData[0]);
        //用站点数据构建一个MKRoute
        MKRoute route = new MKRoute();
        route.customizeRoute(start, stop, routeData);
        //将包含站点信息的MKRoute添加到RouteOverlay中
        routeOverlay = new RouteOverlay(this, mMapView);
        routeOverlay.setData(route);
        //向地图添加构造好的RouteOverlay
        mMapView.getOverlays().add(routeOverlay);
        // 使用zoomToSpan()绽放地图，使路线能完全显示在地图上
        mMapView.getController().zoomToSpan(routeOverlay.getLatSpanE6(), routeOverlay.getLonSpanE6());
        //移动地图到起点
        mMapView.getController().animateTo(res.getStart().pt);
        //执行刷新使生效
        mMapView.refresh();
    }

    private void addWalingPath(MKWalkingRouteResult res, int error) {
        //起点或终点有歧义，需要选择具体的城市列表或地址列表
        if (error == MKEvent.ERROR_ROUTE_ADDR) {
            return;
        }
        //清除其他图层
        clearOverlay();

        // 错误号可参考MKEvent中的定义
        if (error != 0 || res == null) {
            Toast.makeText(this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
            return;
        }

        /** 演示自定义路线使用方法
         *  在北京地图上画一个北斗七星
         *  想知道某个点的百度经纬度坐标请点击：http://api.map.baidu.com/lbsapi/getpoint/index.html
         */
        ArrayList<ArrayList<GeoPoint>> geoPoints = res.getPlan(0).getRoute(0).getArrayPoints();
        ArrayList<GeoPoint> startPoints = geoPoints.get(0);
        ArrayList<GeoPoint> enddPoints = geoPoints.get(geoPoints.size() - 1);
        //计算所有的点数
        int size = 0;
        for (ArrayList<GeoPoint> arrs : geoPoints) {
            size += arrs.size();
        }
        //起点坐标
        GeoPoint start = startPoints.get(0);
        //终点坐标
        GeoPoint stop = enddPoints.get(enddPoints.size() - 1);
        //站点数据保存在一个二维数据中
        ArrayList<GeoPoint> allList = new ArrayList<GeoPoint>(size);
        GeoPoint[][] routeData = new GeoPoint[1][];
        //获取所有的点
        for (int i = 0; i < geoPoints.size() - 1; ++i) {
            allList.addAll(geoPoints.get(i));
        }
        routeData[0] = new GeoPoint[size];
        allList.toArray(routeData[0]);
        //用站点数据构建一个MKRoute
        MKRoute route = new MKRoute();
        route.customizeRoute(start, stop, routeData);
        //将包含站点信息的MKRoute添加到RouteOverlay中
        routeOverlay = new RouteOverlay(this, mMapView);
        routeOverlay.setData(route);
        //向地图添加构造好的RouteOverlay
        mMapView.getOverlays().add(routeOverlay);
        // 使用zoomToSpan()绽放地图，使路线能完全显示在地图上
        mMapView.getController().zoomToSpan(routeOverlay.getLatSpanE6(), routeOverlay.getLonSpanE6());
        //移动地图到起点
        mMapView.getController().animateTo(res.getStart().pt);
        //执行刷新使生效
        mMapView.refresh();
    }

    private void addTransitePath(MKTransitRouteResult res, int error) throws Exception {
        //起点或终点有歧义，需要选择具体的城市列表或地址列表
        if (error == MKEvent.ERROR_ROUTE_ADDR) {
            return;
        }
        //清除其他图层
        clearOverlay();

        // 错误号可参考MKEvent中的定义
        if (error != 0 || res == null) {
            Toast.makeText(this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
            return;
        }

        /** 演示自定义路线使用方法
         *  在北京地图上画一个北斗七星
         *  想知道某个点的百度经纬度坐标请点击：http://api.map.baidu.com/lbsapi/getpoint/index.html
         */
        ArrayList<ArrayList<GeoPoint>> geoPoints = res.getPlan(0).getRoute(0).getArrayPoints();
        ArrayList<GeoPoint> startPoints = geoPoints.get(0);
        ArrayList<GeoPoint> enddPoints = geoPoints.get(geoPoints.size() - 1);
        //计算所有的点数
        int size = 0;
        for (ArrayList<GeoPoint> arrs : geoPoints) {
            size += arrs.size();
        }
        //起点坐标
        GeoPoint start = startPoints.get(0);
        //终点坐标
        //GeoPoint stop = enddPoints.get(enddPoints.size() - 1);
        GeoPoint stop = enddPoints.get(enddPoints.size() - 1);
        //站点数据保存在一个二维数据中
        ArrayList<GeoPoint> allList = new ArrayList<GeoPoint>(size);
        GeoPoint[][] routeData = new GeoPoint[1][];
        //获取所有的点
        for (int i = 0; i < geoPoints.size() - 1; ++i) {
            allList.addAll(geoPoints.get(i));
        }
        routeData[0] = new GeoPoint[size];
        allList.toArray(routeData[0]);
        //用站点数据构建一个MKRoute
        MKRoute route = new MKRoute();
        route.customizeRoute(start, stop, routeData);
        //将包含站点信息的MKRoute添加到RouteOverlay中
        routeOverlay = new RouteOverlay(this, mMapView);
        routeOverlay.setData(route);
        //向地图添加构造好的RouteOverlay
        mMapView.getOverlays().add(routeOverlay);
        // 使用zoomToSpan()绽放地图，使路线能完全显示在地图上
        mMapView.getController().zoomToSpan(routeOverlay.getLatSpanE6(), routeOverlay.getLonSpanE6());
        //移动地图到起点
        mMapView.getController().animateTo(res.getStart().pt);
        //执行刷新使生效
        mMapView.refresh();
    }

    private void displayWalingRoute(MKWalkingRouteResult res, int error) {
        //起点或终点有歧义，需要选择具体的城市列表或地址列表
        if (error == MKEvent.ERROR_ROUTE_ADDR) {
            //遍历所有地址
            return;
        }
        //清除其他图层
        //mMapView.getOverlays().clear();
        clearOverlay();

        // 错误号可参考MKEvent中的定义
        if (error != 0 || res == null) {
            Toast.makeText(this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
            return;
        }

        routeOverlay = new RouteOverlay(this, mMapView);
        // 此处仅展示一个方案作为示例
        MKRoute route = res.getPlan(0).getRoute(0);
        route.getArrayPoints();
        routeOverlay.setData(res.getPlan(0).getRoute(0));

        //添加路线图层
        mMapView.getOverlays().add(routeOverlay);
        //执行刷新使生效
        mMapView.refresh();
    }

    private void displayDrivingRoute(MKDrivingRouteResult res, int error) {
        //起点或终点有歧义，需要选择具体的城市列表或地址列表
        if (error == MKEvent.ERROR_ROUTE_ADDR) {
            //遍历所有地址
            return;
        }
        //清除其他图层
        //mMapView.getOverlays().clear();
        clearOverlay();

        // 错误号可参考MKEvent中的定义
        if (error != 0 || res == null) {
            Toast.makeText(this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
            return;
        }


        int searchType = 0;
        routeOverlay = new RouteOverlay(this, mMapView);
        // 此处仅展示一个方案作为示例
        routeOverlay.setData(res.getPlan(0).getRoute(0));
        //添加路线图层
        mMapView.getOverlays().add(routeOverlay);
        //执行刷新使生效
        mMapView.refresh();
    }

    private void displayTransiteRoute(MKTransitRouteResult res, int error) {
        //起点或终点有歧义，需要选择具体的城市列表或地址列表
        if (error == MKEvent.ERROR_ROUTE_ADDR) {
            return;
        }
        //清除其他图层
        clearOverlay();

        // 错误号可参考MKEvent中的定义
        if (error != 0 || res == null) {
            Toast.makeText(this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
            return;
        }
        transitOverlay = new TransitOverlay(this, mMapView);
        // 此处仅展示一个方案作为示例
        transitOverlay.setData(res.getPlan(0));
        //添加路线图层
        mMapView.getOverlays().add(transitOverlay);
        //执行刷新使生效
        mMapView.refresh();
    }

    // 清除图层
    private void clearOverlay() {
        if (routeOverlay != null) {
            mMapView.getOverlays().remove(routeOverlay);
        }
        if (transitOverlay != null) {
            mMapView.getOverlays().remove(transitOverlay);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.foot:
                foot.setBackgroundResource(R.drawable.bg_tab_top);
                car.setBackgroundColor(Color.WHITE);
                bus.setBackgroundColor(Color.WHITE);
                routeMode = BDMapAcitivity.MODE_WALKING;
                routeFlag = false;
                routeMode = MODE_WALKING;
                dialog.show();
                mkSearch.walkingSearch(currentLocation.getCity(), startNode, currentLocation.getCity(), endNode);
                break;
            case R.id.car:
                car.setBackgroundResource(R.drawable.bg_tab_top);
                foot.setBackgroundColor(Color.WHITE);
                bus.setBackgroundColor(Color.WHITE);
                routeMode = BDMapAcitivity.MODE_DRIVING;
                routeFlag = false;
                routeMode = MODE_DRIVING;
                dialog.show();
                mkSearch.drivingSearch(currentLocation.getCity(), startNode, currentLocation.getCity(), endNode);
                break;
            case R.id.bus:
                bus.setBackgroundResource(R.drawable.bg_tab_top);
                car.setBackgroundColor(Color.WHITE);
                foot.setBackgroundColor(Color.WHITE);
                routeMode = BDMapAcitivity.MODE_TRANSIT;
                routeMode = MODE_TRANSIT;
                routeFlag = false;
                dialog.show();
                mkSearch.transitSearch(currentLocation.getCity(), startNode, endNode);
                break;
            case R.id.btn_back:
                finish();
                break;
            case R.id.btn_go:
                dialog.show();
                routeFlag = true;
                if (routeMode == MODE_WALKING) {
                    mkSearch.walkingSearch(currentLocation.getCity(), startNode, currentLocation.getCity(), endNode);
                } else if (routeMode == MODE_DRIVING) {
                    mkSearch.drivingSearch(currentLocation.getCity(), startNode, currentLocation.getCity(), endNode);
                } else if (routeMode == MODE_TRANSIT) {
                    mkSearch.transitSearch(currentLocation.getCity(), startNode, endNode);
                }
                break;
        }
    }

    /**
     * 添加我的位置图层
     *
     * @param bdLocation
     */
    private void addMyLocationOverlay(BDLocation bdLocation) {

        setTitle(bdLocation.getAddrStr());

        //让地图中心点移动
        GeoPoint geoPoint = new GeoPoint((int) (bdLocation.getLatitude() * 1E6), (int) (bdLocation.getLongitude() * 1E6));
        //mapController.setCenter(geoPoint);
        mapController.animateTo(geoPoint);

        //添加当前位置覆盖物
        if (myLocationOverlay != null) {
            mMapView.getOverlays().remove(myLocationOverlay);
        }

        myLocationOverlay = new MyLocationOverlay(mMapView);
        LocationData locationData = new LocationData();
        locationData.latitude = bdLocation.getLatitude();
        locationData.longitude = bdLocation.getLongitude();
        myLocationOverlay.setData(locationData);

        //添加图层
        mMapView.getOverlays().add(myLocationOverlay);

        mMapView.refresh();
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mMapView.destroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mMapView.onRestoreInstanceState(savedInstanceState);
    }
}
