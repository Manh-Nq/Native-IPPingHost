package com.tapi.ippingdemo;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class JSONParser {

    /* renamed from: is */
    static InputStream f97is = null;
    static JSONObject jObj = null;
    static String json = "";

    public JSONObject getJSONFromUrl(String str) {
        try {
            f97is = new DefaultHttpClient().execute(new HttpPost(str)).getEntity().getContent();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e3) {
            e3.printStackTrace();
        }
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(f97is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            while (true) {
                String readLine = bufferedReader.readLine();
                if (readLine == null) {
                    break;
                }
                sb.append(readLine + "n");
            }
            f97is.close();
            json = sb.toString();
        } catch (Exception e4) {
            Log.e("Buffer Error", "Error converting result " + e4.toString());
        }
        try {
            jObj = new JSONObject(json);
        } catch (JSONException e5) {
            Log.e("JSON Parser", "Error parsing data " + e5.toString());
        }
        return jObj;
    }
}