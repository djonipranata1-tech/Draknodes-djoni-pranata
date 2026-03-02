package com.example.market;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends Activity {

  private TextView tv;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    tv = new TextView(this);
    tv.setTextSize(16);

    ScrollView sv = new ScrollView(this);
    sv.addView(tv);
    setContentView(sv);

    tv.setText("Loading market data...\n\n");
    new Thread(this::load).start();
  }

  private void load() {
    try {
      // coins
      show("BTC", "BTCUSDT", "BTC-USDT");
      show("ETH", "ETHUSDT", "ETH-USDT");
      show("SOL", "SOLUSDT", "SOL-USDT");
      show("XRP", "XRPUSDT", "XRP-USDT");
      show("BNB", "BNBUSDT", "BNB-USDT");
      // ASTER might not exist on OKX/Binance spot; we’ll handle later
      runOnUiThread(() -> tv.append("\nDone.\n"));
    } catch (Exception e) {
      runOnUiThread(() -> tv.append("\nERROR: " + e.getMessage() + "\n"));
    }
  }

  private void show(String name, String binanceSymbol, String okxInstId) throws Exception {
    List<Double> b = MarketApi.fetchBinanceCloses(binanceSymbol, "4h", 200);
    double price = b.get(b.size() - 1);
    double ema20 = Indicators.ema(b, 20);
    double ema50 = Indicators.ema(b, 50);
    double rsi14 = Indicators.rsi(b, 14);
    String dir = Indicators.directionScore(price, ema20, ema50, rsi14);

    String line =
      name + " (Binance 4H)\n" +
      "Price: " + price + "\n" +
      "EMA20: " + ema20 + " | EMA50: " + ema50 + "\n" +
      "RSI14: " + rsi14 + "\n" +
      "Signal: " + dir + "\n\n";

    runOnUiThread(() -> tv.append(line));
  }
}
