package com.tapi.ippingdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.DecimalFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    private static final double BYTE_TO_KILOBIT = 0.0078125d;
    private static final int EXPECTED_SIZE_IN_BYTES = 10485760;
    private static final double KILOBIT_TO_MEGABIT = 9.765625E-4d;
    private static final String TAG = "MainActivity";
    private static final int UPDATE_THRESHOLD = 300;
    ArrayList<String> IPs = new ArrayList<>();
    private final int MSG_COMPLETE_STATUS = 2;
    private final int MSG_UPDATE_CONNECTION_TIME = 1;
    private final int MSG_UPDATE_STATUS = 0;
    TwoTextViewAdapter adapterIpInfo;
    TwoTextViewAdapter adapterSpeedInfo;
    Button btnSpeedTestStart;
    Button btnStart;
    int failed = 0;
    String host = "";
    ArrayList<Map<String, String>> listIpInfo = new ArrayList<>();
    ArrayList<Map<String, String>> listSpeedInfo = new ArrayList<>();
    AdView mAdView;
    Context mContext;
    private DecimalFormat mDecimalFormater;
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler() {
        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 0) {
                SpeedInfo speedInfo = (SpeedInfo) message.obj;
                MainActivity.this.setProgress(message.arg1 * 100);
            } else if (i == 1) {
            } else {
                if (i != 2) {
                    super.handleMessage(message);
                    return;
                }
                HashMap hashMap = new HashMap();
                String convertBitToString = MainActivity.this.convertBitToString((double) ((int) Math.floor(((SpeedInfo) message.obj).kilobits * 1024.0d)));
                hashMap.put("Download speed", convertBitToString + "/s");
                MainActivity.this.listSpeedInfo.add(hashMap);
                MainActivity.this.adapterSpeedInfo.notifyDataSetChanged();
                MainActivity.this.speedTimer.cancel();
                MainActivity.this.btnSpeedTestStart.setText("Start");
                MainActivity.this.btnSpeedTestStart.setEnabled(true);
                MainActivity.this.setProgressBarVisibility(false);
            }
        }
    };
    /* access modifiers changed from: private */
    public Runnable mWorker;
    MyTimerTask myTimerTask;
    boolean pingError = false;
    SharedPreferences prefs;
    boolean running = false;
    ScrollView scrollViewMain;
    Timer speedTestTimer;
    Timer speedTimer;
    ArrayAdapter<String> spinnerAdapter;
    Spinner spinnerHosts;
    int steps = 0;
    int success = 0;
    TabHost tabHost;
    ArrayList<Double> timeList = new ArrayList<>();
    Timer timer;
    Tracker tracker;
    EditText txtHost;
    TextView txtResult;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(C0565R.layout.activity_main);
        this.mContext = this;
        this.prefs = getSharedPreferences(getPackageName(), 0);
        this.tabHost = (TabHost) findViewById(C0565R.C0567id.tabHost);
        this.tabHost.setup();
        TabHost.TabSpec newTabSpec = this.tabHost.newTabSpec("Ping");
        newTabSpec.setContent(C0565R.C0567id.tab1);
        newTabSpec.setIndicator("Ping");
        this.tabHost.addTab(newTabSpec);
        TabHost.TabSpec newTabSpec2 = this.tabHost.newTabSpec("My IP");
        newTabSpec2.setContent(C0565R.C0567id.tab2);
        newTabSpec2.setIndicator("My IP");
        this.tabHost.addTab(newTabSpec2);
        TabHost.TabSpec newTabSpec3 = this.tabHost.newTabSpec("Speed test");
        newTabSpec3.setContent(C0565R.C0567id.tab3);
        newTabSpec3.setIndicator("Speed test");
        this.tabHost.addTab(newTabSpec3);
        ListView listView = (ListView) findViewById(C0565R.C0567id.lstIpInfo);
        this.adapterIpInfo = new TwoTextViewAdapter(this, C0565R.layout.list_item_title_value, this.listIpInfo);
        listView.setAdapter(this.adapterIpInfo);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
                Map.Entry entry = (Map.Entry) ((Map) MainActivity.this.adapterIpInfo.getItem(i)).entrySet().iterator().next();
                ((ClipboardManager) MainActivity.this.getSystemService("clipboard")).setPrimaryClip(ClipData.newPlainText((CharSequence) entry.getKey(), (CharSequence) entry.getValue()));
                Toast.makeText(MainActivity.this.mContext, "Copied to clipboard", 0).show();
            }
        });
        this.spinnerHosts = (Spinner) findViewById(C0565R.C0567id.spinnerHosts);
        this.spinnerHosts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> adapterView) {
            }

            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
                MainActivity.this.txtHost.setText(MainActivity.this.spinnerHosts.getSelectedItem().toString());
            }
        });
        this.tracker = GoogleAnalytics.getInstance(this).newTracker("UA-54901921-3");
        this.tracker.setScreenName("Main");
        this.tracker.send(new HitBuilders.AppViewBuilder().build());
        this.txtHost = (EditText) findViewById(C0565R.C0567id.editTextHost);
        this.btnStart = (Button) findViewById(C0565R.C0567id.btnStart);
        this.txtResult = (TextView) findViewById(C0565R.C0567id.txtResult);
        this.scrollViewMain = (ScrollView) findViewById(C0565R.C0567id.scrollViewMain);
        this.btnStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (MainActivity.this.running) {
                    MainActivity.this.stopTimer();
                } else if (MainActivity.this.txtHost.getText().toString().equals("")) {
                    MainActivity.this.txtHost.setError("This field cannot be empty.");
                } else {
                    MainActivity.this.tracker.send(new HitBuilders.EventBuilder().setCategory("Main").setAction("Ping").setLabel(String.valueOf(MainActivity.this.txtHost.getText().toString())).build());
                    MainActivity.this.txtResult.setText("");
                    MainActivity mainActivity = MainActivity.this;
                    mainActivity.running = true;
                    mainActivity.timer = new Timer();
                    MainActivity mainActivity2 = MainActivity.this;
                    mainActivity2.myTimerTask = new MyTimerTask();
                    MainActivity.this.timer.schedule(MainActivity.this.myTimerTask, 1000, 1000);
                    ((Button) view).setText(MainActivity.this.mContext.getResources().getString(C0565R.string.stop));
                    String obj = MainActivity.this.txtHost.getText().toString();
                    MainActivity.this.prefs.edit().putString("host", obj).commit();
                    if (MainActivity.this.IPs.contains(obj)) {
                        MainActivity.this.IPs.remove(obj);
                    }
                    int i = 0;
                    MainActivity.this.IPs.add(0, obj);
                    MainActivity mainActivity3 = MainActivity.this;
                    mainActivity3.spinnerAdapter = new ArrayAdapter<>(mainActivity3.mContext, 17367048, MainActivity.this.IPs.toArray(new String[MainActivity.this.IPs.size()]));
                    MainActivity.this.spinnerHosts.setAdapter(MainActivity.this.spinnerAdapter);
                    MainActivity.this.spinnerAdapter.notifyDataSetChanged();
                    String str = "";
                    while (i < MainActivity.this.IPs.size() && i < 10) {
                        str = str + "," + MainActivity.this.IPs.get(i);
                        i++;
                    }
                    if (str != "") {
                        str = str.substring(1);
                    }
                    MainActivity.this.prefs.edit().putString("hosts", str).commit();
                }
            }
        });
        setDefaults();
        initBanner();
        getMyIp();
        setupSpeedTest();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(C0565R.C0569menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case C0565R.C0567id.action_no_ads:
                try {
                    startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=com.lipinic.pingpaid")));
                } catch (ActivityNotFoundException unused) {
                    startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://play.google.com/store/apps/details?id=com.lipinic.pingpaid")));
                }
                return true;
            case C0565R.C0567id.action_privacy_policy:
                startActivity(new Intent(this, PrivacyWebViewActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    public void initBanner() {
        this.mAdView = (AdView) findViewById(C0565R.C0567id.adView);
        final AdRequest build = new AdRequest.Builder().addTestDevice("FU4DGIKV55P7PNRK").build();
        new AsyncTask<String, String, String>() {
            /* access modifiers changed from: protected */
            public String doInBackground(String... strArr) {
                try {
                    if (!new JSONParser().getJSONFromUrl("http://www.lipinic.com/ping/banner.php").getBoolean("showAd")) {
                        return null;
                    }
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            MainActivity.this.mAdView.setVisibility(0);
                            MainActivity.this.mAdView.loadAd(build);
                        }
                    });
                    return null;
                } catch (Exception unused) {
                    return null;
                }
            }
        }.execute(new String[]{""});
    }

    public void setDefaults() {
        this.txtHost.setText(this.prefs.getString("host", ""));
        String string = this.prefs.getString("hosts", "");
        if (string == "") {
            string = "192.168.1.1,192.168.0.1,8.8.8.8,8.8.4.4,4.2.2.4";
        }
        for (String add : string.split(",")) {
            this.IPs.add(add);
        }
        ArrayList<String> arrayList = this.IPs;
        this.spinnerAdapter = new ArrayAdapter<>(this, 17367048, arrayList.toArray(new String[arrayList.size()]));
        this.spinnerHosts.setAdapter(this.spinnerAdapter);
    }

    public void stopTimer() {
        this.running = false;
        this.timer.cancel();
        int i = (this.failed * 100) / this.steps;
        if (this.timeList.size() > 0) {
            TextView textView = this.txtResult;
            textView.setText(this.txtResult.getText() + " \n" + ("Ping statistics for " + this.host + ": Packets: Sent = " + this.steps + " , Received = " + this.success + " Lost = " + this.failed + " (" + i + "% loss),  Approximate round trip times in milli-seconds:     Minimum = " + calculateMinTime() + " ms, Maximum = " + calculateMaxTime() + "ms , Average =" + calculateAverage() + "ms"));
        }
        if (this.timeList.size() > 0) {
            this.timeList.clear();
        }
        this.steps = 0;
        this.success = 0;
        this.failed = 0;
        this.btnStart.setText(this.mContext.getResources().getString(C0565R.string.start));
        this.prefs.edit().putString("host", this.txtHost.getText().toString()).commit();
    }

    private Double calculateMaxTime() {
        Double d = this.timeList.get(0);
        for (int i = 0; i < this.timeList.size(); i++) {
            if (this.timeList.get(i).doubleValue() > d.doubleValue()) {
                d = this.timeList.get(i);
            }
        }
        return d;
    }

    private Double calculateMinTime() {
        Double d = this.timeList.get(0);
        for (int i = 0; i < this.timeList.size(); i++) {
            if (this.timeList.get(i).doubleValue() < d.doubleValue()) {
                d = this.timeList.get(i);
            }
        }
        return d;
    }

    private int calculateAverage() {
        double d = 0.0d;
        for (int i = 0; i < this.timeList.size(); i++) {
            d += this.timeList.get(i).doubleValue();
        }
        double size = (double) this.timeList.size();
        Double.isNaN(size);
        return (int) (d / size);
    }

    public String ping(String str) {
        String str2;
        try {
            this.host = str;
            int i = 0;
            Process executeCmd = executeCmd("ping -c 1 -w 5 " + str, false);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(executeCmd.getInputStream()));
            BufferedReader bufferedReader2 = new BufferedReader(new InputStreamReader(executeCmd.getErrorStream()));
            int waitFor = executeCmd.waitFor();
            this.pingError = false;
            this.steps++;
            if (waitFor != 0) {
                if (waitFor != 1) {
                    this.pingError = true;
                    str2 = "";
                    while (true) {
                        String readLine = bufferedReader2.readLine();
                        if (readLine == null) {
                            break;
                        }
                        str2 = str2 + readLine + "\n";
                        this.failed++;
                    }
                } else {
                    str2 = "" + "Request timed out\n";
                    this.failed++;
                }
            } else {
                while (true) {
                    String readLine2 = bufferedReader.readLine();
                    if (readLine2 == null) {
                        str2 = "";
                        break;
                    } else if (i == 1) {
                        str2 = "" + readLine2 + "\n";
                        break;
                    } else {
                        i++;
                        this.success++;
                    }
                }
            }
            executeCmd.destroy();
            return str2;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static Process executeCmd(String str, boolean z) {
        if (!z) {
            try {
                return Runtime.getRuntime().exec(str);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return Runtime.getRuntime().exec(new String[]{"su", "-c", str});
        }
    }

    public void getMyIp() {
        String wifiIp = getWifiIp();
        HashMap hashMap = new HashMap();
        hashMap.put("Wifi IP", wifiIp);
        this.listIpInfo.add(hashMap);
        this.adapterIpInfo.notifyDataSetChanged();
        if (isNetworkAvailable()) {
            new AsyncTask<String, String, String>() {
                /* access modifiers changed from: protected */
                public String doInBackground(String... strArr) {
                    try {
                        JSONObject jSONFromUrl = new JSONParser().getJSONFromUrl("http://www.lipinic.com/ping/ip.php");
                        if (!jSONFromUrl.getBoolean("success")) {
                            return null;
                        }
                        final String string = jSONFromUrl.getString("ip");
                        final String string2 = jSONFromUrl.getString("country");
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                HashMap hashMap = new HashMap();
                                hashMap.put("Internet IP", string);
                                MainActivity.this.listIpInfo.add(hashMap);
                                if (!string2.equals("")) {
                                    HashMap hashMap2 = new HashMap();
                                    hashMap2.put("Country", string2);
                                    MainActivity.this.listIpInfo.add(hashMap2);
                                }
                                MainActivity.this.adapterIpInfo.notifyDataSetChanged();
                            }
                        });
                        return null;
                    } catch (Exception unused) {
                        return null;
                    }
                }
            }.execute(new String[]{""});
        }
    }

    private String getWifiIp() {
        try {
            int ipAddress = ((WifiManager) getApplicationContext().getSystemService("wifi")).getConnectionInfo().getIpAddress();
            return String.format(Locale.getDefault(), "%d.%d.%d.%d", new Object[]{Integer.valueOf(ipAddress & 255), Integer.valueOf((ipAddress >> 8) & 255), Integer.valueOf((ipAddress >> 16) & 255), Integer.valueOf((ipAddress >> 24) & 255)});
        } catch (Exception unused) {
            return "";
        }
    }

    public TextView getTextView(int i) {
        TextView textView = (TextView) findViewById(i);
        textView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ((ClipboardManager) MainActivity.this.getSystemService("clipboard")).setPrimaryClip(ClipData.newPlainText("Ping", ((TextView) view).getText()));
                Toast.makeText(MainActivity.this.mContext, "Copied to clipboard", 0).show();
            }
        });
        return textView;
    }

    public void setupSpeedTest() {
        this.adapterSpeedInfo = new TwoTextViewAdapter(this, C0565R.layout.list_item_title_value, this.listSpeedInfo);
        ((ListView) findViewById(C0565R.C0567id.lstSpeedInfo)).setAdapter(this.adapterSpeedInfo);
        this.speedTestTimer = new Timer();
        this.btnSpeedTestStart = (Button) findViewById(C0565R.C0567id.btnSpeedTestStart);
        this.btnSpeedTestStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (!MainActivity.this.isNetworkAvailable()) {
                    Toast.makeText(MainActivity.this.mContext, "No connection available", 0).show();
                    return;
                }
                MainActivity.this.btnSpeedTestStart.setEnabled(false);
                MainActivity.this.listSpeedInfo.clear();
                MainActivity.this.adapterSpeedInfo.notifyDataSetChanged();
                new AsyncTask<String, String, String>() {
                    /* access modifiers changed from: protected */
                    public String doInBackground(String... strArr) {
                        try {
                            JSONObject jSONFromUrl = new JSONParser().getJSONFromUrl("http://www.lipinic.com/ping/speed_test.php");
                            String string = jSONFromUrl.getString("hostname");
                            String string2 = jSONFromUrl.getString("uri");
                            MainActivity mainActivity = MainActivity.this;
                            C05571 r3 = new Runnable() {
                                private String _url;

                                public Runnable init(String str) {
                                    this._url = str;
                                    return this;
                                }

                                public void run() {
                                    InputStream inputStream = null;
                                    try {
                                        String str = this._url;
                                        long currentTimeMillis = System.currentTimeMillis();
                                        URLConnection openConnection = new URL(str).openConnection();
                                        openConnection.setUseCaches(false);
                                        inputStream = openConnection.getInputStream();
                                        Message obtain = Message.obtain(MainActivity.this.mHandler, 1);
                                        obtain.arg1 = (int) (System.currentTimeMillis() - currentTimeMillis);
                                        MainActivity.this.mHandler.sendMessage(obtain);
                                        long currentTimeMillis2 = System.currentTimeMillis();
                                        long currentTimeMillis3 = System.currentTimeMillis();
                                        int i = 0;
                                        int i2 = 0;
                                        long j = 0;
                                        long j2 = 0;
                                        while (inputStream.read() != -1 && j < 6000) {
                                            i++;
                                            i2++;
                                            if (j2 >= 300) {
                                                double d = (double) i;
                                                Double.isNaN(d);
                                                int i3 = (int) ((d / 1.048576E7d) * 100.0d);
                                                Message obtain2 = Message.obtain(MainActivity.this.mHandler, 0, MainActivity.this.calculate(j2, (long) i2));
                                                obtain2.arg1 = i3;
                                                obtain2.arg2 = i;
                                                MainActivity.this.mHandler.sendMessage(obtain2);
                                                currentTimeMillis3 = System.currentTimeMillis();
                                                i2 = 0;
                                            }
                                            j2 = System.currentTimeMillis() - currentTimeMillis3;
                                            j = System.currentTimeMillis() - currentTimeMillis2;
                                        }
                                        if (j == 0) {
                                            j = 1;
                                        }
                                        Message obtain3 = Message.obtain(MainActivity.this.mHandler, 2, MainActivity.this.calculate(j, (long) i));
                                        obtain3.arg1 = i;
                                        MainActivity.this.mHandler.sendMessage(obtain3);
                                        if (inputStream == null) {
                                            return;
                                        }
                                    } catch (MalformedURLException unused) {
                                        if (inputStream == null) {
                                            return;
                                        }
                                    } catch (IOException unused2) {
                                        if (inputStream == null) {
                                            return;
                                        }
                                    } catch (Throwable th) {
                                        if (inputStream != null) {
                                            try {
                                                inputStream.close();
                                            } catch (IOException unused3) {
                                            }
                                        }
                                        throw th;
                                    }
                                    try {
                                        inputStream.close();
                                    } catch (IOException unused4) {
                                    }
                                }
                            };
                            Runnable unused = mainActivity.mWorker = r3.init(string + string2);
                            new Thread(MainActivity.this.mWorker).start();
                            final String string3 = jSONFromUrl.getString("country");
                            MainActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    HashMap hashMap = new HashMap();
                                    hashMap.put("Server location", string3);
                                    MainActivity.this.listSpeedInfo.add(hashMap);
                                    MainActivity.this.adapterSpeedInfo.notifyDataSetChanged();
                                }
                            });
                            return null;
                        } catch (Exception e) {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(MainActivity.this.mContext, e.getMessage(), 1).show();
                                }
                            });
                            return null;
                        }
                    }
                }.execute(new String[0]);
                C05602 r2 = new TimerTask() {
                    public void run() {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                String str;
                                String charSequence = MainActivity.this.btnSpeedTestStart.getText().toString();
                                if (charSequence.length() == 1) {
                                    str = "..";
                                } else if (charSequence.length() == 2) {
                                    str = "...";
                                } else {
                                    str = charSequence.length() == 3 ? "...." : ".";
                                }
                                MainActivity.this.btnSpeedTestStart.setText(str);
                            }
                        });
                    }
                };
                MainActivity.this.speedTimer = new Timer();
                MainActivity.this.speedTimer.scheduleAtFixedRate(r2, 500, 500);
            }
        });
    }

    public String convertBitToString(double d) {
        String str = String.format("%.2f", new Object[]{Double.valueOf(d)}) + " bits";
        if (d > 1024.0d) {
            d /= 1024.0d;
            str = String.format("%.2f", new Object[]{Double.valueOf(d)}) + " Kb";
        }
        if (d <= 1024.0d) {
            return str;
        }
        return String.format("%.2f", new Object[]{Double.valueOf(d / 1024.0d)}) + " Mb";
    }

    public String convertByteToString(double d) {
        String str = String.format("%.2f", new Object[]{Double.valueOf(d)}) + " Bytes";
        if (d > 1024.0d) {
            d /= 1024.0d;
            str = String.format("%.2f", new Object[]{Double.valueOf(d)}) + " KB";
        }
        if (d <= 1024.0d) {
            return str;
        }
        return String.format("%.2f", new Object[]{Double.valueOf(d / 1024.0d)}) + " MB";
    }

    /* access modifiers changed from: private */
    public boolean isNetworkAvailable() {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) getSystemService("connectivity")).getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    class MyTimerTask extends TimerTask {
        MyTimerTask() {
        }

        public void run() {
            MainActivity mainActivity = MainActivity.this;
            final String ping = mainActivity.ping(mainActivity.txtHost.getText().toString());
            if (ping.contains("time=")) {
                MainActivity.this.timeList.add(Double.valueOf(Double.parseDouble(ping.split("time=")[1].split(" ")[0])));
            }
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    TextView textView = MainActivity.this.txtResult;
                    textView.setText(MainActivity.this.txtResult.getText() + ping);
                    if (MainActivity.this.pingError) {
                        MainActivity.this.stopTimer();
                    }
                    MainActivity.this.scrollViewMain.post(new Runnable() {
                        public void run() {
                            MainActivity.this.scrollViewMain.fullScroll(130);
                        }
                    });
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public SpeedInfo calculate(long j, long j2) {
        SpeedInfo speedInfo = new SpeedInfo();
        double d = (double) ((j2 / j) * 1000);
        Double.isNaN(d);
        double d2 = BYTE_TO_KILOBIT * d;
        double d3 = KILOBIT_TO_MEGABIT * d2;
        speedInfo.downspeed = d;
        speedInfo.kilobits = d2;
        speedInfo.megabits = d3;
        return speedInfo;
    }

    private static class SpeedInfo {
        public double downspeed;
        public double kilobits;
        public double megabits;

        private SpeedInfo() {
            this.kilobits = 0.0d;
            this.megabits = 0.0d;
            this.downspeed = 0.0d;
        }
    }
}