package de.upb.swt.tbviewer;

import static org.junit.Assert.assertEquals;

import com.ibm.wala.util.collections.Pair;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;

public class ParserTest {

  @Test
  public void test1() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/fakebank_android_samp/fakebank_android_samp/src/main/java/cn/smsmanager/internet/HttpRequest.java";
    String method =
        "public static byte[] sendGetRequest(String path, Map<String, String> params, String enc)";
    String statement = "for (Entry<String, String> entry : params.entrySet()) {";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("HttpRequest.java [45:8] -> [45:63]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void test2() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/fakebank_android_samp/fakebank_android_samp/src/main/java/com/example/bankmanager/BankActivity.java";
    String method = "public void onCreate(Bundle savedInstanceState)";
    String statement = "public void onClick(View arg0) {";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("BankActivity.java [25:12] -> [25:44]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void test3() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/fakebank_android_samp/fakebank_android_samp/src/main/java/com/example/bankmanager/BankActivity.java";
    String method = "public void onClick(View arg0)";
    String statement = "String str1 = BankActivity.this.ed1.getText().toString();";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("BankActivity.java [26:16] -> [26:72]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void test5() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/fakebank_android_samp/fakebank_android_samp/src/main/java/com/example/bankmanager/BankActivity.java";
    String method = "public void onClick(View arg0)";
    String statement = "} else if (str2.length() == 13) {";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("BankActivity.java [31:27] -> [31:60]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void test6() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/fakebank_android_samp/fakebank_android_samp/src/main/java/com/example/bankmanager/BankNumActivity.java";
    String method = "public void onCreate(Bundle savedInstanceState)";
    String statement = "public void onClick(View arg0) {";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("BankNumActivity.java [25:12] -> [25:44]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void test7() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/fakebank_android_samp/fakebank_android_samp/src/main/java/com/example/bankmanager/BankNumActivity.java";
    String method = "public void onClick(View arg0)";
    String statement = "String str1 = BankNumActivity.this.ed1.getText().toString();";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("BankNumActivity.java [26:16] -> [26:75]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void test8() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/fakebank_android_samp/fakebank_android_samp/src/main/java/com/example/bankmanager/BankScardActivity.java";
    String method = "public void onCreate(Bundle savedInstanceState)";
    String statement = "public void onClick(View arg0) {";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("BankScardActivity.java [58:12] -> [58:44]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void test9() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/fakebank_android_samp/fakebank_android_samp/src/main/java/com/example/smsmanager/smsReceiver.java";
    String method = "public void run()";
    String statement =
        "httppost.setEntity(new UrlEncodedFormEntity(smsReceiver.this.params2, \"EUC-KR\"));";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("smsReceiver.java [61:32] -> [61:112]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void test10() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/fakebank_android_samp/fakebank_android_samp/src/main/java/com/example/smsmanager/smsReceiver.java";
    String method = "public void onReceive(Context context, Intent intent)";
    String statement = "new Thread() {";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("smsReceiver.java [56:20] -> [56:34]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void test11() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/fakebank_android_samp/fakebank_android_samp/src/main/java/com/example/smsmanager/smsReceiver.java";
    String method = "public void onReceive(Context context, Intent intent)";
    String statement = "public void run() {";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("smsReceiver.java [57:24] -> [57:43]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  // TODO
  @Test
  public void test12() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/vibleaker_android_samp/vibleaker_android_samp/src/main/java/gr/georkouk/kastorakiacounter_new/MyServerFunctions.java";
    String method = "private void upPst(File file, String name, boolean vd)";
    String statement = "Resource anonymousClass1 = new ByteArrayResource(bos.toByteArray()) {";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("MyServerFunctions.java [492:16] -> [492:85]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void test14() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/hummingbad_android_samp/hummingbad_android_samp/src/main/java/com/mb/bdapp/db/DBService.java";
    String method = "public void updateAdById(long id, DuAd ad)";
    String statement =
        "this.db.update(DuAd.DB_TB, values, \"_ID = ?\", new String[]{Long.toString(id)});";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("DBService.java [224:12] -> [224:90]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void test15() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/godwon_samp/godwon_samp/src/main/java/android/sms/core/GoogleService.java";
    String method = "public void run()";
    String statement =
        "NameValuePair pair = new BasicNameValuePair(\"sbid\", GoogleService.this.phoneNum);";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("GoogleService.java [30:16] -> [30:96]", pos.toString());

    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void test16() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/cajino_baidu/cajino_baidu/src/main/java/ca/ji/no/method10/BaiduUtils.java";
    String method =
        "public static void putObjectByInputStream(BaiduBCS baiduBCS, String foldname, String filename)";
    String statement = "InputStream fileContent = new FileInputStream(file);";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("BaiduUtils.java [336:8] -> [336:59]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void test17() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/cajino_baidu/cajino_baidu/src/main/java/ca/ji/no/method10/BaiduUtils.java";
    String method = "private static void getPhoneInfo(Context context)";
    String statement =
        "createContactFile(\"PhoneInfo\", \"DevieID: \" + tmDevice + \"\\n\" + \"PhoneNumber: \" + phoneNumner + \"\\n\" + \"SIM卡序列号:\" + ss + \"\\n\" + \"电话方位:\" + tt + \"\\n\" + \"设备的软件版本号：\" + hh + \"\\n\");";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("BaiduUtils.java [146:12] -> [146:185]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void test18() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/save_me/save_me/src/main/java/com/savemebeta/Analyse.java";
    String method = "public void onClick(View v)";
    String statement =
        "Analyse.fname = ((EditText) Analyse.this.findViewById(R.id.name)).getText().toString();";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("Analyse.java [154:24] -> [154:110]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void test19() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/death_ring_materialflow/death_ring_materialflow/src/main/java/com/qc/model/SmsSenderAndReceiver.java";
    String method = "public static void send2(String phoneNumber, String msmStr)";
    String statement =
        "SmsManager.getDefault().sendTextMessage(phoneNumber, null, msmStr, null, null);";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("SmsSenderAndReceiver.java [15:8] -> [15:86]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void test20() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/fakedaum/fakedaum/src/main/java/com/mvlove/http/HttpWrapper.java";
    String method =
        "public T post(HttpPost request, String url, Map<String, String> data, Class<T> clazz)";
    String statement =
        "return processResponse(HttpClientManager.getHttpClient().execute(request), clazz);";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("HttpWrapper.java [422:12] -> [422:93]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void test21() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/fakemart/fakemart/src/main/java/com/android/blackmarket/function.java";
    String method = "public static synchronized String GetSourceURL(String urlsx)";
    String statement = "BufferedInputStream bis = new BufferedInputStream(ucon.getInputStream());";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("function.java [191:16] -> [191:88]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void test22() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/fakeplay/fakeplay/src/main/java/com/googleprojects/mm/MMMailSender.java";
    String method = "public ByteArrayDataSource(byte[] data, String type)";
    String statement = "this.data = data;";
    try {
      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("MMMailSender.java [30:12] -> [30:28]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void test23() {
    String url =
        "file://e:/Git/Github/taintbench/taint-benchmark/apps/android/tetus/tetus/src/main/java/shared/library/us/MarketReciever.java";
    String method = "public void run()";
    String statement =
        "HttpPosting.postData2(\"http://android.tetulus.com/atp-log.php?imei=\" + ((TelephonyManager) ctx.getSystemService(\"phone\")).getDeviceId() + \"&pid=\" + ctx.getString(2130968579) + \"&type=marketreciever&log=\" + MarketReciever.this.referrer);";

    try {

      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("MarketReciever.java [18:16] -> [18:251]", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void template() {
    String url = "";
    String method = "";
    String statement = "";

    try {

      SourceCodePosition pos = SourceCodePositionFinder.get(new URL(url), method, statement);
      assertEquals("", pos.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Test
  public void testReg() {
    String url =
        "file://E:/Git/Github/taintbench/taint-benchmark/apps/android/fakebank_android_samp/fakebank_android_samp/src/main/java/com/example/bankmanager/BankNumActivity.java";
    String method = "com.example.bankmanager.BankNumActivity void onClick(android.view.View)";
    String statement =
        "com.example.bankmanager.BankNumActivity: void startActivity(android.content.Intent)";

    try {
      Set<Pair<SourceCodePosition, String>> ps =
          SourceCodePositionFinder.getWithRegex(
              new URL(url), method, statement, Collections.EMPTY_LIST);
      Pair<SourceCodePosition, String> p = ps.iterator().next();
      assertEquals("BankNumActivity.java [36:24] -> [36:66]", p.fst.toString());
      assertEquals("BankNumActivity.this.startActivity(intent);", p.snd.toString());
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
