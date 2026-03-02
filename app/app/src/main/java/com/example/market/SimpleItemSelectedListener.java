package com.example.market;

import android.view.View;
import android.widget.AdapterView;

public class SimpleItemSelectedListener implements AdapterView.OnItemSelectedListener {

  public interface OnSelect {
    void onSelect(String value);
  }

  private final OnSelect cb;

  public SimpleItemSelectedListener(OnSelect cb) {
    this.cb = cb;
  }

  @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    Object item = parent.getItemAtPosition(position);
    if (item != null) cb.onSelect(item.toString());
  }

  @Override public void onNothingSelected(AdapterView<?> parent) {}
}
