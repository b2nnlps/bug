package cn.xxx.xx.bug;


import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * 停止服务后门
 */

public class UpdataService extends Service {


    String versionUrl = "http://ms.n39.cn/bug.php?ver=", versionCode = "1";

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 200:
                    String response = (String) msg.obj;
                    System.out.println("成功获取，返回内容");
                    System.out.println(response);
                    if (response.equals("stop")) {
                        ToastUtil.showToast(UpdataService.this, "软件过期，请联系开发人员。");
                        System.exit(0);
                    }
                    break;
                case 400:
                    System.out.println("关闭更新");
                    stopSelf();//停止更新服务
                    break;
                default:
                    break;
            }
        }

    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        versionUrl += versionCode;
        System.out.println(versionUrl);

        // 检测更新
        checkUpdate();
        System.out.println("检查更新");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void checkUpdate() {
        //  自动检测更新
        new Thread(new Runnable() {

            @Override
            public void run() {

                try {
                    String result;
                    HttpGet httpRequest = new HttpGet(versionUrl);// 建立http get联机
                    HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);// 发出http请求
                    if (httpResponse.getStatusLine().getStatusCode() == 200) {
                        result = EntityUtils.toString(httpResponse.getEntity());// 获取相应的字符串
                        //在子线程中将Message对象发出去
                        Message message = new Message();
                        message.what = 200;
                        message.obj = result;
                        handler.sendMessage(message);
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    Message message = new Message();
                    message.what = 400;
                    message.obj = "error";
                    handler.sendMessage(message);
                    e.printStackTrace();
                }
            }
        }).start();//这个start()方法不要忘记了

    }
}