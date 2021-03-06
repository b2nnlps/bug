package cn.xxx.xx.bug;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;

public class PrintActivity extends Service {

    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothDevice device;
    private BluetoothSocket bluetoothSocket;
    private ArrayList<BluetoothSocket> printers = new ArrayList<>();
    private ArrayList<BluetoothDevice> bondDevicesList = new ArrayList<>();
    private String myAddress = "", cmd = "";
    private int printerCount = 0;
    BluetoothSocket printer;
    OutputStream outputStream;

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 200:
                    String response = (String) msg.obj;
                    ToastUtil.showToast(PrintActivity.this, "打印机连接成功！");
                    break;
                case 400:
                    ToastUtil.showToast(PrintActivity.this, "打印机连接失败，请检查设置！");
                    break;
                default:
                    break;
            }
        }

    };

    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("启动Service");
        showBarMess("正在运行中...");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initData(intent);
//        return super.onStartCommand(intent, flags, startId);
        return START_REDELIVER_INTENT;//保持进程后台运行
    }

    public void showBarMess(String mess) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification builder = new Notification.Builder(this)
                .setTicker("运行中")
                .setContentTitle("运行中")
                .setContentText(mess)
                .setSmallIcon(R.drawable.ic_menu_gallery)
                .build();
        manager.notify(67, builder);
    }

    private void initData(Intent intent) {
        isDisconnect();//更新打印机状态
        cmd = intent.getStringExtra(MainActivity.CMD);//获取命令，字符串类型
        System.out.println(cmd);
        if (!cmd.equals("connect")) {//重连服务器，readFile里自带了
            print(cmd);
            return;
        }
        device = intent.getParcelableExtra(MainActivity.DEVICE);

        if (!bondDevicesList.contains(device)) {//是否已连接过 没有点击或或者连接丢失
            ThreadConnect();
        } else {
            ToastUtil.showToast(PrintActivity.this, "已连接！");
        }

    }

    /**
     * 连接蓝牙设备
     */
    private void startConnect() {
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            printers.add(bluetoothSocket);//如果连接成功，加入打印机输出流列表
            bondDevicesList.add(device);//添加到已连接队列
            if (!bluetoothAdapter.isDiscovering()) {
                //     System.out.println("蓝牙没扫描到！");
            }
            setConnectResult(bluetoothSocket.isConnected());
        } catch (Exception e) {
            setConnectResult(false);
        }
    }

    private void setConnectResult(boolean result) {
        Message message = new Message();
        if (result) {
            myAddress = device.getAddress();
            writeFile("defaultDevice.ng", myAddress);
            message.what = 200; //弹出打印机连接成功
            printerCount++;
            printSuccess();
            showBarMess("已连接" + String.valueOf(printerCount) + "个打印机");
        } else {
            message.what = 400;
        }
        message.obj = "";
        handler.sendMessage(message);
    }

    /**
     * 打印数据
     */
    public void print(String sendData) {
        if (TextUtils.isEmpty(sendData)) {
            return;
        }
        sendData = sendData + "\n\n\n";//3行回车正好加上打印后缀
        int i;
        BluetoothSocket printer;
        for (i = 0; i < printers.size(); i++) {
            printer = printers.get(i);
            try {
                outputStream = printer.getOutputStream();
                byte[] print_data = sendData.getBytes("gbk");
                outputStream.write(print_data, 0, print_data.length);
                outputStream.flush();
                ToastUtil.showToast(PrintActivity.this, "打印小票成功");
            } catch (IOException e) {
                ToastUtil.showToast(PrintActivity.this, "发送失败！");
                ToastUtil.showToast(PrintActivity.this, "有设备掉线了，请检查连接！");
                printerCount--;
                showBarMess("已连接" + String.valueOf(printerCount) + "个打印机");
                isDisconnect();//更新打印机状态
            }
        }
    }

    /**
     * 打印数据
     */
    public void printSuccess() {
        String sendData = "=====打印机连接成功=====" + "\n\n\n\n\n\n";//3行回车正好加上打印后缀
        BluetoothSocket printer = bluetoothSocket;
        try {
            outputStream = printer.getOutputStream();
            byte[] print_data = sendData.getBytes("gbk");
            outputStream.write(print_data, 0, print_data.length);
            outputStream.flush();
        } catch (IOException e) {
            ToastUtil.showToast(PrintActivity.this, "发送失败！");
            ToastUtil.showToast(PrintActivity.this, "有设备掉线了，请检查连接！");
            printerCount--;
            showBarMess("已连接" + String.valueOf(printerCount) + "个打印机");
            isDisconnect();//更新打印机状态
        }
    }

    public void isDisconnect() {//更新最新的连接状态,关闭废弃的连接
        int i, j;
        for (i = 0; i < printers.size(); i++) {//遍历所有蓝牙连接
            printer = printers.get(i);
            if (!printer.isConnected()) {//定时监测打印机失联，把连接队列删除
//                try {
//                    printer.close();//关闭蓝牙连接
//                    outputStream=printer.getOutputStream();
//                    outputStream.close();//关闭输出流
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                System.out.println(i);
                j = printers.indexOf(printer);
                printers.remove(printers.get(j));
                bondDevicesList.remove(bondDevicesList.get(j));
            }
        }
    }

    /**
     * 断开蓝牙设备连接,服务关闭的时候触发
     */
    public void disconnect() {
        System.out.println("断开蓝牙设备连接");

        int i;
        BluetoothSocket printer;
        for (i = 0; i < printers.size(); i++) {//遍历所有蓝牙连接
            printer = printers.get(i);
            try {
                printer.close();//关闭蓝牙连接
                outputStream = printer.getOutputStream();
                outputStream.close();//关闭输出流
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//            startConnect();//重新连接

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

    @Override
    public void onDestroy() {
        disconnect();
        super.onDestroy();
    }

    private void ThreadConnect() {//通过线程连接蓝牙防卡
        //  自动检测更新
        new Thread(new Runnable() {
            @Override
            public void run() {
                startConnect();
            }
        }).start();//这个start()方法不要忘记了

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
