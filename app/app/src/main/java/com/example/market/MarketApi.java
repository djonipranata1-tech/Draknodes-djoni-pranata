package com.example.market;

import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MarketApi {

  // Binance Klines (public)
  public static List<Double> fetchBinanceCloses(String symbol, String interval, int limit) throws Exception {
    String url = "https://api.binance.com/api/v3/klines?symbol=" + symbol + "&interval=" + interval + "&limit=" + limit;
    String json = httpGet(url);
    JSONArray arr = new JSONArray(json);
    List<Double> closes = new ArrayList<>();
    for (int i = 0; i < arr.length(); i++) {
      JSONArray k = arr.getJSONArray(i);
      closes.add(Double.parseDouble(k.getString(4))); // close
    }
    return closes;
  }

  // OKX Candles (public) instId like BTC-USDT, bar like 1H/4H/1D
  public static List<Double> fetchOkxCloses(String instId, String bar, int limit) throws Exception {
    String url = "https://www.okx.com/api/v5/market/candles?instId=" + instId + "&bar=" + bar + "&limit=" + limit;
    String json = httpGet(url);
    JSONArray root = new JSONArray(new org.json.JSONObject(json).getJSONArray("data").toString());
    List<Double> closes = new ArrayList<>();
    // OKX returns newest first -> reverse
    for (int i = root.length() - 1; i >= 0; i--) {
      JSONArray k = root.getJSONArray(i);
      closes.add(Double.parseDouble(k.getString(4))); // close
    }
    return closes;
  }

  private static String httpGet(String urlStr) throws Exception {
    URL url = new URL(urlStr);
    HttpURLConnection con = (HttpURLConnection) url.openConnection();
    con.setRequestMethod("GET");
    con.setConnectTimeout(15000);
    con.setReadTimeout(15000);
    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String inputLine;
    StringBuilder content = new StringBuilder();
    while ((inputLine = in.readLine()) != null) content.append(inputLine);
    in.close();
    con.disconnect();
    return content.toString();
  }
}
