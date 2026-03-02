package com.example.market;

import java.util.ArrayList;
import java.util.List;

public class Indicators {

  public static double ema(List<Double> closes, int period) {
    if (closes == null || closes.size() < period) return Double.NaN;
    double k = 2.0 / (period + 1.0);
    double ema = 0.0;
    // SMA awal
    for (int i = 0; i < period; i++) ema += closes.get(i);
    ema /= period;
    for (int i = period; i < closes.size(); i++) {
      ema = closes.get(i) * k + ema * (1.0 - k);
    }
    return ema;
  }

  public static double rsi(List<Double> closes, int period) {
    if (closes == null || closes.size() < period + 1) return Double.NaN;
    double gain = 0.0, loss = 0.0;
    for (int i = 1; i <= period; i++) {
      double diff = closes.get(i) - closes.get(i - 1);
      if (diff >= 0) gain += diff; else loss -= diff;
    }
    gain /= period; loss /= period;
    double rs = (loss == 0) ? 999 : (gain / loss);
    double rsi = 100.0 - (100.0 / (1.0 + rs));

    for (int i = period + 1; i < closes.size(); i++) {
      double diff = closes.get(i) - closes.get(i - 1);
      double g = diff > 0 ? diff : 0;
      double l = diff < 0 ? -diff : 0;
      gain = (gain * (period - 1) + g) / period;
      loss = (loss * (period - 1) + l) / period;
      rs = (loss == 0) ? 999 : (gain / loss);
      rsi = 100.0 - (100.0 / (1.0 + rs));
    }
    return rsi;
  }

  public static String directionScore(double price, double ema20, double ema50, double rsi14) {
    int score = 0;
    if (!Double.isNaN(ema20) && price > ema20) score += 1; else score -= 1;
    if (!Double.isNaN(ema50) && price > ema50) score += 1; else score -= 1;

    if (!Double.isNaN(rsi14)) {
      if (rsi14 >= 55) score += 1;
      else if (rsi14 <= 45) score -= 1;
    }

    if (score >= 2) return "BULLISH ✅ (score " + score + ")";
    if (score <= -2) return "BEARISH 🔻 (score " + score + ")";
    return "SIDEWAYS ⚪ (score " + score + ")";
  }
}
