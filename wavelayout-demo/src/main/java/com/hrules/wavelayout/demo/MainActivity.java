package com.hrules.wavelayout.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import com.hrules.wavelayout.WaveLayout;
import com.hrules.wavelayout.WaveLayoutListener;

public class MainActivity extends AppCompatActivity {
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    final WaveLayout waveLayout = (WaveLayout) findViewById(R.id.waveLayout);

    waveLayout.addWaveLayoutListener(new WaveLayoutListener() {
      @Override public void onStart() {
        Log.d(getString(R.string.app_name), "onStart()");
      }

      @Override public void onStopping() {
        Log.d(getString(R.string.app_name), "onStopping()");
      }

      @Override public void onStop() {
        Log.d(getString(R.string.app_name), "onStop()");
      }

      @Override public void onRestart() {
        Log.d(getString(R.string.app_name), "onRestart()");
      }
    });

    waveLayout.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        if (waveLayout.isRunning()) {
          waveLayout.stop(true);
        } else {
          waveLayout.start();
        }
      }
    });
  }
}
