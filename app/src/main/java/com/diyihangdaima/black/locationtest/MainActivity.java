package com.diyihangdaima.black.locationtest;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class MainActivity extends Activity {
    private TextView positionTextView;
    private LocationManager locationManager;
    private String provider;
    private static final int SHOW_LOCATION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        positionTextView = (TextView) findViewById(R.id.position_text_view);
        //获取LocaltionManager对象
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //获取所有可用的位置提供器
        List<String> providerList = locationManager.getProviders(true);
        //true表示只有启用的位置提供器才会被返回
        if (providerList.contains(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        } else if (providerList.contains(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        } else {
            //没有位置提供器使用时，Toast提示
            Toast.makeText(this, "No location provider to use", Toast.LENGTH_SHORT).show();
            return;
        }

        Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            //显示当前设备的位置信息
            showLocation(location);
        }
        locationManager.requestLocationUpdates(provider, 5000, 1, locationListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            //更新当前设备的位置信息
            showLocation(location);

        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_LOCATION:
                    String currentPosition = (String) msg.obj;
                    positionTextView.setText(currentPosition);
                    break;
                default:
                    break;
            }
        }
    };

    private void showLocation(final Location location) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //组装反向地理编码的接口地址
                StringBuilder urlStringBuilder = new StringBuilder();
                urlStringBuilder.append("http://maps.googleapis.com/maps/api/geocode/json?latlng=");
                urlStringBuilder.append(location.getLatitude()).append(",");
                urlStringBuilder.append(location.getLongitude());
                urlStringBuilder.append("&sensor=false");
                urlStringBuilder.append("&oe=utf8");
                urlStringBuilder.append("zh-CN");
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(urlStringBuilder.toString());
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(50000);
                    connection.setReadTimeout(50000);

                    connection.setRequestProperty("Accept-Language", "zh-CN");
                    connection.connect();
//                    connection.setRequestProperty("Accept-Charset", "UTF-8");
//                    connection.setRequestProperty("Content-type", "application/x-java-serialized-object");
                    InputStream in = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf-8"));
                    StringBuilder response = new StringBuilder();
                    String lines;
                    while ((lines = reader.readLine()) != null) {
                        response.append(lines);
                    }
                    JSONObject jsonObject = new JSONObject(String.valueOf(response));
                    //获取requests节点下的位置信息
                    JSONArray resultArray = jsonObject.getJSONArray("results");
                    if (resultArray.length() > 0) {
                        JSONObject subObject = resultArray.getJSONObject(0);
                        //取出格式化后的位置信息
                        String address = subObject.getString("formatted_address");
                        Message message = new Message();
                        message.what = SHOW_LOCATION;
                        message.obj = address;
                        handler.sendMessage(message);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
//        //使用HttpClient方式
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                //组装反向地理编码的接口地址
//                StringBuilder url = new StringBuilder();
//                url.append("http://maps.googleapis.com/maps/api/geocode/json?latlng=");
//                url.append(location.getLatitude()).append(",");
//                url.append(location.getLongitude());
//                url.append("&sensor=false");
//                HttpClient httpClient = new DefaultHttpClient();
//                HttpGet httpGet = new HttpGet(url.toString());
//                //在请求头中指定语言，保证服务器会返回中文数据
//                httpGet.addHeader("Accept-Language", "zh-CN");
//                try {
//                    HttpResponse httpResponse = httpClient.execute(httpGet);
//                    if (httpResponse.getStatusLine().getStatusCode() == 200) {
//                        HttpEntity entity = httpResponse.getEntity();
//                        String response = EntityUtils.toString(entity, "utf-8");
//                        JSONObject jsonObject = new JSONObject(response);
//                        //获取results节点下的位置信息
//                        JSONArray resultsArray = jsonObject.getJSONArray("results");
//                        if (resultsArray.length() > 0) {
//                            JSONObject subObject = resultsArray.getJSONObject(0);
//                            //取出格式化后的位置信息
//                            String address = subObject.getString("formatted_address");
//                            Message message = new Message();
//                            message.what = SHOW_LOCATION;
//                            message.obj = address;
//                            handler.sendMessage(message);
//                        }
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
    }
}