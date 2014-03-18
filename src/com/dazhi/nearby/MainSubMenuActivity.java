package com.dazhi.nearby;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.dazhi.uitls.JsonUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainSubMenuActivity extends Activity {
    private List<String> datas = new ArrayList<String>(8);
    private ArrayAdapter<String> arrayAdapter;
    private ListView listView;
    private ImageButton backButton;
    private BDLocation currentLocation;

    private String bigTypeName, middleTypeName, curType;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_submenu);

        currentLocation = getIntent().getParcelableExtra("currentLocation");

        bigTypeName = getIntent().getStringExtra("bigTypeName");
        middleTypeName = getIntent().getStringExtra("middleTypeName");
        currentLocation = getIntent().getParcelableExtra("currentLocation");
        curType = getIntent().getStringExtra("curType");

        TextView textView = (TextView) findViewById(R.id.type);
        textView.setText(curType);

        backButton = (ImageButton) findViewById(R.id.btn_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainSubMenuActivity.this.finish();
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });

        // 中间ListView
        initActivityData();
        listView = (ListView) findViewById(R.id.type_listView);
        arrayAdapter = new ArrayAdapter<String>(this, R.layout.main_activity_item_list, R.id.listView_text, datas);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            Intent intent = null;
            if (bigTypeName == null && middleTypeName == null) { // 向二级菜单跳
                intent = new Intent(MainSubMenuActivity.this, MainSubMenuActivity.class);
                intent.putExtra("bigTypeName", datas.get(position));
            } else if (bigTypeName != null && middleTypeName == null && hasSubMenu()) { // 向三级级菜单跳
                intent = new Intent(MainSubMenuActivity.this, MainSubMenuActivity.class);
                intent.putExtra("bigTypeName", bigTypeName);
                intent.putExtra("middleTypeName", datas.get(position));
            } else {  // 向具体搜索页面跳
                intent = new Intent(MainSubMenuActivity.this, SearchRefreshWithRangeActivity.class);
            }
            intent.putExtra("curType", datas.get(position));
            intent.putExtra("currentLocation", currentLocation);
            startActivity(intent);

            }
        });
    }

    private void initActivityData() {
        if (bigTypeName == null && middleTypeName == null) { // 顶级分类
            initBigTypeDatas();
        } else if (bigTypeName != null && middleTypeName == null) { // 二级分类
            initMiddleTypeDatas();
        } else if (bigTypeName != null && middleTypeName != null) {  // 三级分类
            initSmallTypeDatas();
        }
    }

    /**
     * 判断(中类)是否有子菜单
     * @return 有，返回true
     */
    private boolean hasSubMenu() {
        String[] smallTypeArray = JsonUtils.getSmallTypeArray(getApplicationContext(), bigTypeName, middleTypeName);
        return smallTypeArray == null ? false : true;
    }

    // 读取分类代码：http://open.weibo.com/wiki/Location/category
    private void initBigTypeDatas() {
        JSONArray jsonArray = JsonUtils.getBigTypeJsonArray(getApplicationContext());
        for (int i = 0; jsonArray != null && i < jsonArray.length() ; ++i) {
            JSONObject obj = (JSONObject) jsonArray.opt(i);
            String bigTypeName = obj.opt("bigTypeName").toString();
            datas.add(bigTypeName);
            Log.d("bigTypeName value", i + ":" + bigTypeName);
        }
    }

    private void initMiddleTypeDatas() {
        JSONArray jsonArray = JsonUtils.getMiddleTypeJsonArray(getApplicationContext(), bigTypeName);
        for (int i = 0; jsonArray != null &&  i < jsonArray.length(); ++i) {
            JSONObject obj = (JSONObject) jsonArray.opt(i);
            String middleTypeName = obj.opt("middleTypeName").toString();
            datas.add(middleTypeName);
            Log.d("middleTypeName value", i + ":" + middleTypeName);
        }
    }

    private void initSmallTypeDatas() {
        String[] arr = JsonUtils.getSmallTypeArray(getApplicationContext(), bigTypeName, middleTypeName);
        for (int i = 0; arr != null && i < arr.length; ++i) {
            datas.add(arr[i]);
        }
    }

}
