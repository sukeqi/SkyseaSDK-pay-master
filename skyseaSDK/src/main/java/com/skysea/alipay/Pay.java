package com.skysea.alipay;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.alipay.sdk.app.PayTask;
import com.skysea.android.app.lib.MResource;
import com.skysea.app.BaseActivity;
import com.skysea.async.AutoCancelServiceFramework;
import com.skysea.exception.ResponseException;
import com.skysea.sdk.main.MainActivity;
import com.skysea.sdk.main.PaymentInfoActivity;
import com.skysea.utils.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CancellationException;

/**
 * Created by 44631 on 2016/8/22.
 */
public class Pay {
    private static final int SDK_PAY_FLAG = 1;
    private Activity activity;

    public Pay(Activity activity) {
        this.activity = activity;
    }

    /**
     * call alipay sdk pay. 调用SDK支付
     */
    public void pay(String name, String msg, String orderno, String money) {
        // 订单
        String orderInfo = getOrderInfo(name, msg, orderno, money);
        // 对订单做RSA 签名
        String sign = sign(orderInfo);
        try {
            // 仅需对sign 做URL编码
            sign = URLEncoder.encode(sign, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 完整的符合支付宝参数规范的订单信息
        final String payInfo = orderInfo + "&sign=\"" + sign + "\"&"
                + getSignType();

        Runnable payRunnable = new Runnable() {
            @Override
            public void run() {
                // 构造PayTask 对象
                PayTask alipay = new PayTask(activity);
                // 调用支付接口，获取支付结果
                String result = alipay.pay(payInfo, true);

                Message msg = new Message();
                msg.what = SDK_PAY_FLAG;
                msg.obj = result;
                handler.sendMessage(msg);
            }
        };

        // 必须异步调用
        Thread payThread = new Thread(payRunnable);
        payThread.start();
    }

    /**
     * create the order info. 创建订单信息
     */
    public String getOrderInfo(String subject, String body, String orderno,
                               String price) {

        // 签约合作者身份ID
        String orderInfo = "partner=" + "\"" + PartnerConfig.PARTNER + "\"";

        // 签约卖家支付宝账号
        orderInfo += "&seller_id=" + "\"" + PartnerConfig.SELLER + "\"";

        // 商户网站唯一订单号
        orderInfo += "&out_trade_no=" + "\"" + orderno + "\"";

        // 商品名称
        orderInfo += "&subject=" + "\"" + subject + "\"";

        // 商品详情
        orderInfo += "&body=" + "\"" + body + "\"";

        // 商品金额
        orderInfo += "&total_fee=" + "\"" + price + "\"";

        // 服务器异步通知页面路径
        orderInfo += "&notify_url=" + "\"" + Constant.NOTIFY_URL + "\"";

        // 服务接口名称， 固定值
        orderInfo += "&service=\"mobile.securitypay.pay\"";

        // 支付类型， 固定值
        orderInfo += "&payment_type=\"1\"";
        // 参数编码， 固定值
        orderInfo += "&_input_charset=\"utf-8\"";
        // 设置未付款交易的超时时间
        // 默认30分钟，一旦超时，该笔交易就会自动被关闭。
        // 取值范围：1m～15d。
        // m-分钟，h-小时，d-天，1c-当天（无论交易何时创建，都在0点关闭）。
        // 该参数数值不接受小数点，如1.5h，可转换为90m。
        orderInfo += "&it_b_pay=\"30m\"";

        // extern_token为经过快登授权获取到的alipay_open_id,带上此参数用户将使用授权的账户进行支付
        // orderInfo += "&extern_token=" + "\"" + extern_token + "\"";
        // 支付宝处理完请求后，当前页面跳转到商户指定页面的路径，可空
        orderInfo += "&return_url=\"m.alipay.com\"";

        // 调用银行卡支付，需配置此参数，参与签名， 固定值 （需要签约《无线银行卡快捷支付》才能使用）
        // orderInfo += "&paymethod=\"expressGateway\"";

        return orderInfo;
    }

    /**
     * get the out_trade_no for an order. 生成商户订单号，该值在商户端应保持唯一（可自定义格式规范）
     */
    public String getOutTradeNo() {
        SimpleDateFormat format = new SimpleDateFormat("MMddHHmmss",
                Locale.getDefault());
        Date date = new Date();
        String key = format.format(date);
        Random r = new Random();
        key = key + r.nextInt();
        key = key.substring(0, 15);
        return key;
    }

    /**
     * sign the order info. 对订单信息进行签名
     *
     * @param content 待签名订单信息
     */
    public String sign(String content) {
        return Rsa.sign(content, PartnerConfig.RSA_PRIVATE);
    }

    /**
     * get the sign type we use. 获取签名方式
     */
    public String getSignType() {
        return "sign_type=\"RSA\"";
    }

    Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String strRet = (String) msg.obj;
            String tradeStatus = "resultStatus={";
            int imemoStart = strRet.indexOf("resultStatus=");
            imemoStart += tradeStatus.length();
            int imemoEnd = strRet.indexOf("};memo=");
            tradeStatus = strRet.substring(imemoStart, imemoEnd);

            switch (msg.what) {
                case SDK_PAY_FLAG: {
                    try {

                        ResultChecker resultChecker = new ResultChecker(strRet);
                        int retVal = resultChecker.checkSign();

                        if (retVal == ResultChecker.RESULT_CHECK_SIGN_FAILED) {
                            BaseHelper.showDialog(
                                    activity,
                                    "提示",
                                    activity.getResources().getString(
                                            MResource.getIdByName(activity
                                                            .getApplicationContext(),
                                                    "string",
                                                    "check_sign_failed")),
                                    android.R.drawable.ic_dialog_alert);
                        } else {
                            if (tradeStatus.equals("9000")) {
                                postResult(activity, Utils.subPayResultString(tradeStatus));
                                Toast.makeText(activity, "支付成功",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                // “8000”代表支付结果因为支付渠道原因或者系统原因还在等待支付结果确认，最终交易是否成功以服务端异步通知为准（小概率状态）
                                if (tradeStatus.equals("9000")) {
                                    Toast.makeText(activity, "支付结果确认中",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(activity, "支付失败",
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    }catch (Exception e){

                    }
                    Intent data = new Intent();
                    data.putExtra("tradeStatus",tradeStatus);
                    activity.setResult(200,data);
                    activity.finish();
                    break;
                }
            }
        }
    };

    private void postResult(Activity context, String result) {
        ((BaseActivity) context)
                .autoCancel(new AutoCancelServiceFramework<String, Void, String>(
                        (BaseActivity) context) {

                    @Override
                    protected void onPreExecute() {
                        // TODO Auto-generated method stub
                        super.onPreExecute();
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        // TODO Auto-generated method stub
                    }

                    @Override
                    protected String doInBackground(String... params) {
                        createIPlatCokeService();
                        try {
                            return mIPlatService.toPostResult(params[0]);
                        } catch (CancellationException e) {
                            e.printStackTrace();
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (ResponseException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }.execute(result));
    }
}
