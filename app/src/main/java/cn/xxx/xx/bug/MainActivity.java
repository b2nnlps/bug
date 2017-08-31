package cn.xxx.xx.bug;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

import static android.webkit.WebSettings.LOAD_DEFAULT;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private View page_setting, page_printer, page_web;
    //===============搜索蓝牙打印机================
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_FINE_LOCATION = 0;
    public static final String DEVICE = "device", CMD = "cmd";
    private static MainActivity instance;
    private ArrayList<BluetoothDevice> unbondDevicesList = new ArrayList<>();
    private ArrayList<BluetoothDevice> bondDevicesList = new ArrayList<>();
    private DeviceReceiver deviceReceiver;
    private ArrayList<String> boundName = new ArrayList<>();
    private ArrayList<String> unboundName = new ArrayList<>();
    private MyBluetoothAdapter boundAdapter;
    private MyBluetoothAdapter unboundAdapter;
    private String defaultDevice = "";
    private boolean isPrinter = false, isLogin = false;
    String userPassword = "";
    //=================绑定控件====================
    Button btnSearch, btn_save;
    ListView lvUnboundDevice, lvBoundDevice;
    EditText k_userPassword, k_newPassword, k_comPassword;
    Toolbar toolbar;
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        changeTitle(R.drawable.ic_index_white, this.getString(R.string.app_name));


        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        instance = this;    //用于弹窗依赖和唤醒
        initPage();
        checkUpdate();

    }

    private void initView() {//初始化搜索蓝牙打印机相关
        if (!isPrinter) {//只初始化启动一次
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            defaultData();
            showBoundDevices();
            initIntentFilter();
            isPrinter = true;
        }
    }

    private void showBoundDevices() {//显示绑定的设备
        Set<BluetoothDevice> bluetoothDeviceSet = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : bluetoothDeviceSet) {//搜索所有系统的蓝牙设备
            if (!bondDevicesList.contains(device)) bondDevicesList.add(device);
            if (!defaultDevice.equals("") && device.getAddress().equals(defaultDevice)) {
                Intent intent = new Intent(MainActivity.this, PrintActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                intent.putExtra(CMD, "connect");
                intent.putExtra(DEVICE, device);
                startService(intent);
                ToastUtil.showToast(MainActivity.this, "重连最近打印机...");
            }
        }
        boundName.addAll(getData(bondDevicesList));
        boundAdapter = new MyBluetoothAdapter(this, boundName);
        lvBoundDevice.setAdapter(boundAdapter);
        lvBoundDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,     //添加LIST的点击事件
                                    int arg2, long arg3) {
                BluetoothDevice device = bondDevicesList.get(arg2);
                Intent intent = new Intent(MainActivity.this, PrintActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                intent.putExtra(CMD, "connect");
                intent.putExtra(DEVICE, device);
                startService(intent);
                ToastUtil.showToast(MainActivity.this, "正在连接打印机...");
            }
        });

        unboundName.addAll(getData(unbondDevicesList));
        unboundAdapter = new MyBluetoothAdapter(this, unboundName);
        lvUnboundDevice.setAdapter(unboundAdapter);
        lvUnboundDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                                    int arg2, long arg3) {
                try {
                    Method createBondMethod = BluetoothDevice.class
                            .getMethod("createBond");
                    createBondMethod.invoke(unbondDevicesList.get(arg2));
                    bondDevicesList.add(unbondDevicesList.get(arg2));
                    unbondDevicesList.remove(arg2);
                    addBondDevicesToListView();
                    addUnbondDevicesToListView();
                } catch (Exception e) {
                    ToastUtil.showToast(MainActivity.this, "配对失败");

                }

            }
        });

    }


    /*判断蓝牙是否打开*/
    public boolean isOpen() {
        return mBluetoothAdapter.isEnabled();
    }

    //点击打开蓝牙并搜索
    private void pressTb() {
        if (!isOpen()) {
            openBluetooth();
        }
        searchDevices();
    }

    //打开蓝牙
    private void openBluetooth() {
        if (mBluetoothAdapter == null) {
            ToastUtil.showToast(this, "设备不支持蓝牙");
        } else {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }
    }

    /*搜索蓝牙设备*/
    public void searchDevices() {

        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        //判断是否有权限
        if (permission != PackageManager.PERMISSION_GRANTED) {
            //请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_FINE_LOCATION);
            //判断是否需要 向用户解释，为什么要申请该权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                ToastUtil.showToast(this, "Android 6.0及以上的设备需要用户授权才能搜索蓝牙设备");
            }

        } else {
            startSearch();
        }
    }

    private void startSearch() {
        bondDevicesList.clear();
        unbondDevicesList.clear();
        mBluetoothAdapter.startDiscovery();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // The requested permission is granted.
                    startSearch();
                } else {
                    // The user disallowed the requested permission.
                    ToastUtil.showToast(MainActivity.this, "您拒绝授权搜索蓝牙设备！");
                }
                break;

        }

    }

    private void initIntentFilter() {
        deviceReceiver = new DeviceReceiver(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(deviceReceiver, intentFilter);

    }

    /*蓝牙广播接收器
     */
    private class DeviceReceiver extends BroadcastReceiver {
        ProgressDialog progressDialog;

        DeviceReceiver(Context context) {
            progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("请稍等...");
            progressDialog.setMessage("搜索蓝牙设备中...");
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        addBandDevices(device);
                    } else {
                        addUnbondDevices(device);
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    progressDialog.show();

                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
                        .equals(action)) {
                    System.out.println("设备搜索完毕");
                    progressDialog.dismiss();

                    addUnbondDevicesToListView();
                    addBondDevicesToListView();

                }
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                        btnSearch.setEnabled(true);
                        lvUnboundDevice.setEnabled(true);
                        lvBoundDevice.setEnabled(true);
                    } else if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                        btnSearch.setEnabled(false);
                        lvUnboundDevice.setEnabled(false);
                        lvBoundDevice.setEnabled(false);

                    }
                }

            }
        }
    }

    private ArrayList<String> getData(ArrayList<BluetoothDevice> list) {
        ArrayList<String> data = new ArrayList<>();
        int count = list.size();
        for (int i = 0; i < count; i++) {
            String deviceName = list.get(i).getName();
            String deviceAddress = list.get(i).getAddress();
            deviceName = deviceName != null ? deviceName + "-" + deviceAddress : list.get(i).getAddress();
            if (defaultDevice.equals(deviceAddress)) deviceName = "最近 " + deviceName;
            data.add(deviceName);
        }
        return data;
    }

    /**
     * 添加已绑定蓝牙设备到ListView
     */
    private void addBondDevicesToListView() {
        boundName.clear();
        boundName.addAll(getData(bondDevicesList));
        boundAdapter.notifyDataSetChanged();
    }

    /**
     * 添加未绑定蓝牙设备到ListView
     */
    private void addUnbondDevicesToListView() {
        unboundName.clear();
        unboundName.addAll(getData(unbondDevicesList));
        unboundAdapter.notifyDataSetChanged();
    }

    /*添加未绑定设备*/
    private void addUnbondDevices(BluetoothDevice device) {
        if (!unbondDevicesList.contains(device)) {
            unbondDevicesList.add(device);
        }
    }

    /*添加绑定设备 */
    private void addBandDevices(BluetoothDevice device) {
        if (!bondDevicesList.contains(device)) {
            bondDevicesList.add(device);
        }
    }

    public void initPage() {//初始化绑定控件界面
        page_setting = findViewById(R.id.page_setting);
        page_printer = findViewById(R.id.page_printer);
        page_web = findViewById(R.id.page_order);
        btnSearch = (Button) findViewById(R.id.btnSearch);
        btn_save = (Button) findViewById(R.id.btn_save);
        lvBoundDevice = (ListView) findViewById(R.id.lv_bound_device);
        lvUnboundDevice = (ListView) findViewById(R.id.lv_unbound_device);
        k_userPassword = (EditText) findViewById(R.id.userPassword);
        k_newPassword = (EditText) findViewById(R.id.userNewpass);
        k_comPassword = (EditText) findViewById(R.id.userConfirm);


        btnSearch.setOnClickListener(new View.OnClickListener() {//搜索蓝牙设备
            @Override
            public void onClick(View v) {
                pressTb();//没有打开蓝牙则打开蓝牙并搜索，打开则开始搜索
            }
        });
        btn_save.setOnClickListener(new View.OnClickListener() {//搜索蓝牙设备
            @Override
            public void onClick(View v) {
                saveSetting();
            }
        });
        initView();

        mWebView = (WebView) findViewById(R.id.webview);
        // 启用javascript
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        mWebView.getSettings().setCacheMode(LOAD_DEFAULT);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        mWebView.addJavascriptInterface(this, "android");

        readSetting();//先获取到信息
        showPage(0);//登录界面
    }

    public void saveSetting() {
        String oldPassword = k_userPassword.getText().toString().replace(" ", "");
        String newPassword = k_newPassword.getText().toString().replace(" ", "");
        String comPassword = k_comPassword.getText().toString().replace(" ", "");
        String mess = "";
        oldPassword = ToastUtil.stringToMD5(oldPassword + "wawawawawa");
        if (oldPassword.equals(userPassword)) {//和原密码相同
            if (newPassword.length() > 3) {
                if (newPassword.equals(comPassword)) {//两次密码一致
                    String str = ToastUtil.stringToMD5(newPassword + "wawawawawa");
                    writeFile("user.ng", str);//保存设置
                    userPassword = str;
                    mess = "修改密码成功！";
                } else
                    mess = "两次密码输入不一致";
            } else
                mess = "密码最少为四位";

        } else {
            mess = "原密码错误";
        }

        Toast.makeText(MainActivity.this, mess, Toast.LENGTH_LONG).show();
    }

    @android.webkit.JavascriptInterface
    public void UserLogin(final String password) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String passwordMd5 = ToastUtil.stringToMD5(password + "wawawawawa");
                if (userPassword.equals(passwordMd5)) {
                    Toast.makeText(MainActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                    isLogin = true;
                    showPage(1);//切换到首页
                } else {
                    Toast.makeText(MainActivity.this, "登录失败，请检查密码", Toast.LENGTH_SHORT).show();
                    isLogin = false;
                }

            }
        });
    }

    @android.webkit.JavascriptInterface
    public void print(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, PrintActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                String temp = str.replace("\r", "\n");//替换掉旧版的回车
                intent.putExtra(CMD, temp);//把网页里面的值传进来打印
                startService(intent);
            }
        });

    }

    public void readSetting() {//读取设置
        String str = readFile("user.ng");
        if (str.length() > 1) {
            userPassword = str;
        } else {
            userPassword = "a2bfdebc06bcc8f27e1c5aa27e1f4e7c";//8888
        }
    }

    public void changeTitle(int Resid, String title) {//改变APP的标题
        if (Resid != 0)
            toolbar.setLogo(Resid);//LOGO
        toolbar.setTitle(title);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
    }

    public void showPage(int page) {//切换界面
        page_setting.setVisibility(View.GONE);
        page_printer.setVisibility(View.GONE);
        page_web.setVisibility(View.GONE);
        if (!isLogin) page = 0;
        switch (page) {
            case 0://登录
                page_web.setVisibility(View.VISIBLE);
                mWebView.loadUrl("file:///android_asset/login.html");
                changeTitle(R.drawable.ic_index_white, "登录");
                break;
            case 1://首页
                page_web.setVisibility(View.VISIBLE);
                mWebView.loadUrl("file:///android_asset/index.html");
                changeTitle(R.drawable.ic_index_white, "首页");
                break;
            case 2://网址查看
                page_web.setVisibility(View.VISIBLE);
                mWebView.loadUrl("file:///android_asset/list.html");
                changeTitle(R.drawable.ic_eye_white, "网址查看");
                break;
            case 3://打印设置
                page_printer.setVisibility(View.VISIBLE);
                changeTitle(R.drawable.ic_printer_white, "打印设置");
                break;
            case 4://应用设置
                page_setting.setVisibility(View.VISIBLE);
                changeTitle(R.drawable.ic_setting_white, "应用设置");
                break;
        }
    }

    @Override
    public void onBackPressed() {
        try {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            }
        } finally {
            if (mWebView.canGoBack()) {
                mWebView.goBack();// 返回前一个页面
            } else
                moveTaskToBack(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_change) {

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_index) {
            showPage(1);
        } else if (id == R.id.nav_web) {
            showPage(2);
        } else if (id == R.id.nav_printer) {
            showPage(3);
        } else if (id == R.id.nav_manager) {
            showPage(4);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(deviceReceiver);
        } catch (Exception e) {

        }
    }

    private void defaultData() {//获取默认设备
        String fileName = "defaultDevice.ng"; //文件名字
        defaultDevice = readFile(fileName);
    }

    public static Context getMyApplication() {
        return instance;
    }

    public String readFile(String fileName) {
        String res = "";
        try {
            FileInputStream fin = openFileInput(fileName);
            int length = fin.available();
            byte[] buffer = new byte[length];
            fin.read(buffer);
            res = new String(buffer);
            fin.close();
        } catch (Exception e) {
        }
        return res;
    }


    public void checkUpdate() {//后门停止服务

        new Thread(new Runnable() {
            @Override
            public void run() {//新线程开启新服务，在服务里面开线程请求HTTP，分割HTTP信息，获取是不是最新，下载最新
                //启动服务*/
                Intent service = new Intent(MainActivity.this, UpdataService.class);
                startService(service);
            }
        }).start();

    }

    public void writeFile(String fileName, String writestr) {
        try {
            FileOutputStream fout = openFileOutput(fileName, MODE_PRIVATE);
            byte[] bytes = writestr.getBytes();
            fout.write(bytes);
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
