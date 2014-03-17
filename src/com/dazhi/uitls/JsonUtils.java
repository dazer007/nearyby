package com.dazhi.uitls;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * weibo open 定位分类数(json)操作类
 */
public class JsonUtils {
    /**
     * 获取本地assets目录中的json数据
     *
     * @param context
     * @param jsonPath assert目录中的json文件,如"app.json"
     * @return
     * @throws IOException
     * @throws org.json.JSONException
     */
    public static JSONObject readAssertJSON(Context context, String jsonPath) throws IOException, JSONException {
        // 1：读取Assets的文本数据
        String jsonStr = "";
        AssetManager assetManager = context.getAssets();
        InputStream in = assetManager.open(jsonPath);
        jsonStr = readInputStremText(in);
        // 2 :获取JSONObject
        JSONObject jsonObject = null;
        if (!jsonStr.isEmpty()) {
            jsonObject = new JSONObject(jsonStr);
        }
        return jsonObject;
    }

    /**
     * 从服务器读取文本数据
     *
     * @param in
     * @return
     */
    public static String readInputStremText(InputStream in) throws IOException {
        String str = "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (in != null) {
            byte[] buf = new byte[1024]; // 一次读取1k的数据
            int len = 0;
            try {
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                str = out.toString("UTF-8");
            } catch (IOException e) {
                throw e;
            } finally {
                if (null != out) {
                    out.close();
                }
                if (null != in) {
                    in.close();
                }
                ;
            }
        }
        return str;
    }


    /**
     * 获取大类数组
     *
     * @return
     */
    public static JSONArray getBigTypeJsonArray(Context context) {
        JSONArray jsonArray = null;
        try {
            JSONObject jsonObject = readAssertJSON(context, "weibo_location_type.json");
            jsonArray = jsonObject.optJSONArray("大类");
        } catch (IOException e) {
            Log.e(JsonUtils.class.getName(), e.getMessage(), e);
        } catch (JSONException e) {
            Log.e(JsonUtils.class.getName(), e.getMessage(), e);
        }
        return jsonArray;
    }

    /**
     * 获取中类数组
     *
     * @param bigTypeName
     * @return
     */
    public static JSONArray getMiddleTypeJsonArray(Context context, String bigTypeName) {
        JSONObject middleTypeObj = null;
        JSONArray bigJsonArray = getBigTypeJsonArray(context);
        for (int i = 0; i < bigJsonArray.length(); ++i) {
            try {
                JSONObject obj = (JSONObject) bigJsonArray.get(i);
                String name = obj.opt("bigTypeName").toString();
                if (name.equals(bigTypeName)) {
                    middleTypeObj = obj;
                    break;
                }
            } catch (JSONException e) {
                Log.e(JsonUtils.class.getName(), e.getMessage(), e);
            }
        }
        return (JSONArray) middleTypeObj.opt("中类");
    }


    /**
     * 根据中类名称获取小类名称数组
     *
     * @param bigTypeName    大类名称
     * @param middleTypeName 中类名称
     * @return
     */
    public static String[] getSmallTypeArray(Context context, String bigTypeName, String middleTypeName) {
        String[] strArr = null;

        try {
            JSONArray middleTypeJsonArray = getMiddleTypeJsonArray(context, bigTypeName);
            JSONObject smallTypeObj = null;
            for (int i = 0; i < middleTypeJsonArray.length(); ++i) {
                JSONObject obj = (JSONObject) middleTypeJsonArray.get(i);
                String name = obj.opt("middleTypeName").toString();
                if (name.equals(middleTypeName)) {
                    smallTypeObj = obj;
                    break;
                }
            }

            JSONArray jsonArr = smallTypeObj.optJSONArray("小类");
            if (jsonArr != null) {
                strArr = new String[jsonArr.length()];

                strArr = new String[jsonArr.length()];
                for (int i = 0; i < strArr.length; ++i) {
                    strArr[i] = jsonArr.getString(i);
                }

            }
        } catch (JSONException e) {
            Log.e(JsonUtils.class.getName(), e.getMessage(), e);
        }

        return strArr;
    }

}
