package de.upb.swt.tbviewer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

public class LocationTest {

  @Test
  public void test() {
    String javaMethod =
        "public T post(HttpPost request, String url, Map<String, String> data, Class<T> clazz)";
    assertEquals(
        javaMethod.replaceAll("\\<[^\\>]+\\>", ""),
        "public T post(HttpPost request, String url, Map data, Class clazz)");
  }

  @Test
  public void testPatterns() {
    String javaMethod = "public void onCreate(Bundle savedInstanceState)";
    String jimpleMethod =
        "com.example.bankmanager.BankNumActivity: void onCreate(android.os.Bundle)";
    Pattern java = Location.createJavaMethodPattern();
    Matcher m1 = java.matcher(javaMethod);
    if (m1.find()) {
      System.out.println(m1.group(1));
      System.out.println(m1.group(2));
      System.out.println(m1.group(3));
    }
    System.out.println();
    Pattern jimple = Location.createJimpleMethodPattern();
    Matcher m2 = jimple.matcher(jimpleMethod);
    if (m2.find()) {
      System.out.println(m2.group(1));
      System.out.println(m2.group(2));
      System.out.println(m2.group(3));
    }
    assertTrue(Location.compareMethod(javaMethod, jimpleMethod));
  }

  @Test
  public void testPatterns2() {
    String javaMethod = "public void onReceive(Context context, Intent intent)";
    String jimpleMethod =
        "com.example.smsmanager.BootCompleteBroadcastReceiver: void onReceive(android.content.Context,android.content.Intent)";
    assertTrue(Location.compareMethod(javaMethod, jimpleMethod));
  }

  @Test
  public void testPatterns3() {
    String javaMethod =
        "public static byte[] sendGetRequest(String path, Map<String, String> params, String enc)";
    String jimpleMethod =
        "cn.smsmanager.internet.HttpRequest: byte[] sendGetRequest(java.lang.String,java.util.Map,java.lang.String)";
    assertTrue(Location.compareMethod(javaMethod, jimpleMethod));
  }
}
