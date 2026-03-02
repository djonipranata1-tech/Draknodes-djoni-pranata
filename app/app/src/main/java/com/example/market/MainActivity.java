package com.example.market;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

  private TextView tv;

  // UI state
  private String source = "BINANCE";  // BINANCE | OKX
  private String tf = "4h";           // 15m | 1h | 4h | 1d

  // Coins mapping
  // Binance symbols: BTCUSDT, ETHUSDT, ...
  // OKX instId: BTC-USDT, ETH-USDT, ...
  private final Map<String, String[]> coins = new LinkedHashMap<String, String[]>() {{
    put("BTC", new String[]{"BTCUSDT", "BTC-USDT"});
    put("ETH", new String[]{"ETHUSDT", "ETH-USDT"});
    put("SOL", new String[]{"SOLUSDT", "SOL-USDT"});
    put("XRP", new String[]{"XRPUSDT", "XRP-USDT"});
    put("BNB", new String[]{"BNBUSDT", "BNB-USDT"});
    // ASTER: aktifkan kalau lo yakin pair-nya sama di exchange yang lo pakai
    // put("ASTER", new String[]{"ASTERUSDT", "ASTER-USDT"});
  }};

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Root layout
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(24, 24, 24, 24);

    // Top buttons row
    LinearLayout topRow = new LinearLayout(this);
    topRow.setOrientation(LinearLayout.HORIZONTAL);
    topRow.setGravity(Gravity.CENTER_VERTICAL);

    Button btnNews = new Button(this);
    btnNews.setText("Berita Trader Tangguh");
    btnNews.setOnClickListener(v -> openUrl("https://iuwashtangguh.or.id/indeks-berita/"));

    Button btnTV = new Button(this);
    btnTV.setText("TradingView");
    btnTV.setOnClickListener(v -> openUrl("https://www.tradingview.com/markets/cryptocurrencies/prices-all/"));

    Button btnCoinglass = new Button(this);
    btnCoinglass.setText("Coinglass");
    btnCoinglass.setOnClickListener(v -> openUrl("https://www.coinglass.com/"));

    topRow.addView(btnNews);
    topRow.addView(btnTV);
    topRow.addView(btnCoinglass);

    // Controls row (source + timeframe + refresh)
    LinearLayout controls = new LinearLayout(this);
    controls.setOrientation(LinearLayout.HORIZONTAL);
    controls.setPadding(0, 16, 0, 16);
    controls.setGravity(Gravity.CENTER_VERTICAL);

    Spinner spSource = new Spinner(this);
    ArrayAdapter<String> srcAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
        new String[]{"BINANCE", "OKX"});
    srcAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spSource.setAdapter(srcAdapter);
    spSource.setSelection(0);
    spSource.setOnItemSelectedListener(new SimpleItemSelectedListener(v -> {
      source = v;
      logLine("\n[Source] " + source);
    }));

    Spinner spTf = new Spinner(this);
    ArrayAdapter<String> tfAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
        new String[]{"15m", "1h", "4h", "1d"});
    tfAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spTf.setAdapter(tfAdapter);
    spTf.setSelection(2); // default 4h
    spTf.setOnItemSelectedListener(new SimpleItemSelectedListener(v -> {
      tf = v;
      logLine("\n[Timeframe] " + tf);
    }));

    Button btnRefresh = new Button(this);
    btnRefresh.setText("Refresh");
    btnRefresh.setOnClickListener(v -> {
      tv.setText("");
      logHeader();
      new Thread(this::loadAll).start();
    });

    controls.addView(spSource);
    controls.addView(spTf);
    controls.addView(btnRefresh);

    // Output text
    tv = new TextView(this);
    tv.setTextSize(15);
    tv.setTextIsSelectable(true);

    ScrollView sv = new ScrollView(this);
    sv.addView(tv);

    root.addView(topRow);
    root.addView(controls);
    root.addView(sv);

    setContentView(root);

    // initial load
    logHeader();
    new Thread(this::loadAll).start();
  }

  private void openUrl(String url) {
    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    startActivity(i);
  }

  private void logHeader() {
    logLine("Market Analyzer (Online) ✅");
    logLine("Source: " + source + " | TF: " + tf);
    logLine("Indicators: EMA20/EMA50 + RSI14 + Liquidity Proxy (swing sweep)");
    logLine("====================================================\n");
  }

  private void loadAll() {
    try {
      for (Map.Entry<String, String[]> e : coins.entrySet()) {
        String name = e.getKey();
        String binanceSymbol = e.getValue()[0];
        String okxInstId = e.getValue()[1];
        analyzeOne(name, binanceSymbol, okxInstId);
      }
      runOnUiThread(() -> logLine("\nSelesai ✅"));
    } catch (Exception ex) {
      runOnUiThread(() -> logLine("\nERROR: " + ex.getMessage()));
    }
  }

  private void analyzeOne(String name, String binanceSymbol, String okxInstId) throws Exception {
    List<Double> closes;

    if ("BINANCE".equals(source)) {
      // interval Binance: 15m, 1h, 4h, 1d
      closes = MarketApi.fetchBinanceCloses(binanceSymbol, tf, 250);
    } else {
      // bar OKX: 15m, 1H, 4H, 1D
      String bar = tf.equals("15m") ? "15m" : (tf.equals("1h") ? "1H" : (tf.equals("4h") ? "4H" : "1D"));
      closes = MarketApi.fetchOkxCloses(okxInstId, bar, 250);
    }

    if (closes == null || closes.size() < 60) {
      runOnUiThread(() -> logLine(name + ": data kurang (butuh >= 60 candle)\n"));
      return;
    }

    double price = closes.get(closes.size() - 1);

    double ema20 = Indicators.ema(closes, 20);
    double ema50 = Indicators.ema(closes, 50);
    double rsi14 = Indicators.rsi(closes, 14);

    // Liquidity proxy: deteksi "sweep" sederhana dari close series (kasar tapi membantu)
    // Untuk lebih akurat, idealnya pakai OHLC, tapi MVP ini tetap berguna.
    boolean sweep = detectSweepProxy(closes, 20);

    String dir = Indicators.directionScore(price, ema20, ema50, rsi14);
    String bias = deriveBias(price, ema20, ema50, rsi14, sweep);

    // Entry/SL/TP sederhana berbasis ATR proxy (pakai volatility dari close)
    double atrProxy = volatilityProxy(closes, 14);
    double sl = bias.startsWith("BULL") ? (price - 1.2 * atrProxy) : (price + 1.2 * atrProxy);
    double tp1 = bias.startsWith("BULL") ? (price + 1.0 * atrProxy) : (price - 1.0 * atrProxy);
    double tp2 = bias.startsWith("BULL") ? (price + 2.0 * atrProxy) : (price - 2.0 * atrProxy);

    String block =
        "🟦 " + name + " (" + source + " " + tf + ")\n" +
        "Price : " + fmt(price) + "\n" +
        "EMA20 : " + fmt(ema20) + " | EMA50: " + fmt(ema50) + "\n" +
        "RSI14 : " + fmt(rsi14) + "\n" +
        "Sweep : " + (sweep ? "YES" : "NO") + "\n" +
        "Signal: " + dir + "\n" +
        "Bias  : " + bias + "\n" +
        "Plan  : Entry≈" + fmt(price) + " | SL " + fmt(sl) + " | TP1 " + fmt(tp1) + " | TP2 " + fmt(tp2) + "\n" +
        "----------------------------------------------------\n";

    runOnUiThread(() -> tv.append(block));
  }

  private String deriveBias(double price, double ema20, double ema50, double rsi14, boolean sweep) {
    // Gabungan “TradingView style” (EMA+RSI) + “Coinglass style” (liquidity hunt proxy)
    int score = 0;

    if (!Double.isNaN(ema20)) score += (price > ema20) ? 1 : -1;
    if (!Double.isNaN(ema50)) score += (price > ema50) ? 1 : -1;

    if (!Double.isNaN(rsi14)) {
      if (rsi14 >= 55) score += 1;
      else if (rsi14 <= 45) score -= 1;
    }

    if (sweep) score += 1; // sweep sering jadi sinyal reversal/pullback selesai

    if (score >= 3) return "BULLISH ✅ (score " + score + ")";
    if (score <= -3) return "BEARISH 🔻 (score " + score + ")";
    return "NEUTRAL ⚪ (score " + score + ")";
  }

  // Proxy volatility (ATR-like) from closes only: average absolute return * price scale
  private double volatilityProxy(List<Double> closes, int period) {
    int n = closes.size();
    int start = Math.max(1, n - period - 1);
    double sum = 0.0;
    int cnt = 0;
    for (int i = start; i < n; i++) {
      double diff = Math.abs(closes.get(i) - closes.get(i - 1));
      sum += diff;
      cnt++;
    }
    return (cnt == 0) ? 0.0 : (sum / cnt);
  }

  // Sweep proxy from closes: detect if last close broke recent min/max then returned
  private boolean detectSweepProxy(List<Double> closes, int lookback) {
    int n = closes.size();
    if (n < lookback + 5) return false;

    double last = closes.get(n - 1);
    double prev = closes.get(n - 2);

    // recent window excluding last candle
    double min = Double.MAX_VALUE;
    double max = -Double.MAX_VALUE;
    for (int i = n - lookback - 2; i <= n - 3; i++) {
      double v = closes.get(i);
      if (v < min) min = v;
      if (v > max) max = v;
    }

    // sweep down then reclaim: prev close below min, last close above min
    if (prev < min && last > min) return true;
    // sweep up then reject: prev close above max, last close below max
    if (prev > max && last < max) return true;

    return false;
  }

  private void logLine(String s) {
    runOnUiThread(() -> tv.append(s + "\n"));
  }

  private String fmt(double v) {
    if (Double.isNaN(v)) return "NaN";
    // simple formatting
    return String.format(java.util.Locale.US, "%.4f", v);
  }
}
