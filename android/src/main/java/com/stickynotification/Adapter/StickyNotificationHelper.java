package com.stickynotification.Adapter;

import static com.stickynotification.StickyNotificationModule.props;

import android.content.Intent;

import android.os.Looper;

public class StickyNotificationHelper {

  public static void open(Intent intent){
    new android.os.Handler(Looper.getMainLooper()).postDelayed(
      new Runnable() {
        public void run() {
          String btn = intent.getStringExtra ("action");
          if(btn!=null){
            props.onPress(btn);

          }
        }
      },
      100);

  }
}
