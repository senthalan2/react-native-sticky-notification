package com.stickynotification;

import static com.stickynotification.StickyNotificationModule.CHANNEL_ID;
import static com.stickynotification.StickyNotificationModule.ICONS_LIST;
import static com.stickynotification.StickyNotificationModule.promise;
import static com.stickynotification.StickyNotificationModule.props;


import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.List;

public class StickyNotificationService extends Service{
  @Nullable
  @Override
  public IBinder onBind(Intent intent) {

    return null;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {

    if(intent!=null && props != null) {

      switch (intent.getAction()) {
        case "startService": {
          startForegroundService();

          break;
        }
        case "b1": {
          if (isAppOnForeground(this)) {
            props.onPress(props.displayTexts()[0]);
          } else {
            enableActivity(props.displayTexts()[0]);
          }
          exit(1);
          break;
        }
        case "b2": {
          if (isAppOnForeground(this)) {
            props.onPress(props.displayTexts()[1]);
          } else {
            enableActivity(props.displayTexts()[1]);
          }

          exit(2);
          break;
        }
        case "b3": {
          if (isAppOnForeground(this)) {
            props.onPress(props.displayTexts()[2]);
          } else {
              enableActivity(props.displayTexts()[2]);


          }

          exit(3);
          break;
        }
        case "b4": {
          if (isAppOnForeground(this)) {
            props.onPress(props.displayTexts()[3]);
          } else {
            enableActivity(props.displayTexts()[3]);
          }

          exit(4);
          break;
        }
        case "b5": {
          if (isAppOnForeground(this)) {
            props.onPress(props.displayTexts()[4]);
          } else {
            enableActivity(props.displayTexts()[4]);
          }
          exit(5);
          if (props.buttonsCount() == 0) {

            stopForeground(true);
            stopSelf();
          }

          break;
        }
      }
      Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
  try{
    sendBroadcast(it);
  } catch (Exception e) {
    Log.d("StartNotification", "onStartCommand: "+e);
  }

    }

    return START_STICKY;
  }

  private void enableActivity(String pressedButton) {

    if(!isAppOnForeground(this)) {
      Intent intent1 = new Intent(this, getMainActivityClass(StickyNotificationModule.reactContext));
      intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent1.putExtra("action",pressedButton);
      startActivity(intent1);

    }
  }

  private boolean isAppOnForeground(Context context) {
    ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
    if (appProcesses == null) {
      return false;
    }
    final String packageName = context.getPackageName();
    for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
      if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
        return true;
      }
    }
    return false;
  }


  public void startForegroundService(){


    RemoteViews notificationLayout = new RemoteViews(getPackageName(),R.layout.notification_panel);
    Intent notificationIntent = new Intent(this, getMainActivityClass(StickyNotificationModule.reactContext));
    PendingIntent pendingIntent = PendingIntent.getActivity(this,
      0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

    if(props!=null){

      Integer[] integer =new Integer[]{R.id.t1,R.id.t2,R.id.t3,R.id.t4,R.id.t5};
      for(int i =0;i<props.displayTexts().length;i++){
        notificationLayout.setTextViewText(integer[i],StickyNotificationModule.props.displayTexts()[i]);
      }
      try{
        for(int i=0;i<props.displayIcons().size();i++){
          if(i< props.iconsCount()){
            notificationLayout.setImageViewResource(getSourceIconID(i),ICONS_LIST.get(Integer.parseInt(props.displayIcons().get(i))));
          }
        }
      }
      catch (Exception e){
        promise.reject("ICONS_ERROR",e.getMessage());
      }

      if(props.buttonsCount()==5){
        notificationLayout.setViewVisibility(R.id.b5, View.VISIBLE);
        notificationLayout.setViewVisibility(R.id.b4,View.VISIBLE);
        notificationLayout.setViewVisibility(R.id.b3,View.VISIBLE);
        notificationLayout.setViewVisibility(R.id.b2,View.VISIBLE);
        notificationLayout.setViewVisibility(R.id.b1,View.VISIBLE);
      }
     else if(props.buttonsCount()==4){
        notificationLayout.setViewVisibility(R.id.b5, View.GONE);
        notificationLayout.setViewVisibility(R.id.b4,View.VISIBLE);
        notificationLayout.setViewVisibility(R.id.b3,View.VISIBLE);
        notificationLayout.setViewVisibility(R.id.b2,View.VISIBLE);
        notificationLayout.setViewVisibility(R.id.b1,View.VISIBLE);
      }
      else if(props.buttonsCount()==3){

        notificationLayout.setViewVisibility(R.id.b5,View.GONE);
        notificationLayout.setViewVisibility(R.id.b4,View.GONE);
        notificationLayout.setViewVisibility(R.id.b3,View.VISIBLE);
        notificationLayout.setViewVisibility(R.id.b2,View.VISIBLE);
        notificationLayout.setViewVisibility(R.id.b1,View.VISIBLE);
      }
      else if(props.buttonsCount()==2){
        notificationLayout.setViewVisibility(R.id.b5,View.GONE);
        notificationLayout.setViewVisibility(R.id.b4,View.GONE);
        notificationLayout.setViewVisibility(R.id.b3,View.GONE);
        notificationLayout.setViewVisibility(R.id.b2,View.VISIBLE);
        notificationLayout.setViewVisibility(R.id.b1,View.VISIBLE);
      }

      else if(props.buttonsCount()==1){

        notificationLayout.setViewVisibility(R.id.b5,View.GONE);
        notificationLayout.setViewVisibility(R.id.b4,View.GONE);
        notificationLayout.setViewVisibility(R.id.b3,View.GONE);
        notificationLayout.setViewVisibility(R.id.b2,View.GONE);
        notificationLayout.setViewVisibility(R.id.b1,View.VISIBLE);

      }



    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Notification Service")
      .setContentText("Notification Service")
      .setCustomContentView(notificationLayout)
      .setContentIntent(pendingIntent)
      .setAutoCancel(false)
      .setOngoing(true).setPriority(1);

if(props.icon().equals("app-icon")){
  notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
}
else if(props.icon().equals("app-icon-rounded")){
  notificationBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
}
else if(props.icon().equals("other")){
  notificationBuilder.setSmallIcon(R.drawable.notification_icon);
}



    Intent recordIntent = new Intent(this, StickyNotificationService.class);
    recordIntent.setAction("b1");
    PendingIntent pendingRecordIntent = PendingIntent.getService(this, 0, recordIntent, PendingIntent.FLAG_IMMUTABLE);
    notificationLayout.setOnClickPendingIntent(R.id.b1,pendingRecordIntent);

    Intent screenshotIntent = new Intent(this, StickyNotificationService.class);
    screenshotIntent.setAction("b2");
    PendingIntent pendingScreenshotIntent = PendingIntent.getService(this, 0, screenshotIntent, PendingIntent.FLAG_IMMUTABLE);

    notificationLayout.setOnClickPendingIntent(R.id.b2,pendingScreenshotIntent);

    Intent toolsIntent = new Intent(this, StickyNotificationService.class);
    toolsIntent.setAction("b3");
    PendingIntent pendingToolsIntent = PendingIntent.getService(this, 0, toolsIntent, PendingIntent.FLAG_IMMUTABLE);
    notificationLayout.setOnClickPendingIntent(R.id.b3,pendingToolsIntent);

    Intent homeIntent = new Intent(this, StickyNotificationService.class);
    homeIntent.setAction("b4");
    PendingIntent pendingHomeIntent = PendingIntent.getService(this, 0, homeIntent, PendingIntent.FLAG_IMMUTABLE);
    notificationLayout.setOnClickPendingIntent(R.id.b4,pendingHomeIntent);

    Intent exitIntent = new Intent(this, StickyNotificationService.class);
    exitIntent.setAction("b5");
    PendingIntent pendingExitIntent = PendingIntent.getService(this, 0, exitIntent, PendingIntent.FLAG_IMMUTABLE);
    notificationLayout.setOnClickPendingIntent(R.id.b5,pendingExitIntent);

    Notification notification = notificationBuilder.build();

    startForeground(1, notification);

    }
  }

  public void exit(int position){

    if( props.buttonsCount()==position && props.exitEnabled() ){
      stopForeground(true);
      stopSelf();
    }
  }


  public static Bitmap getDefaultAlbumArt(Context context, int resId) {
    Bitmap bm = null;
    try {
      bm = BitmapFactory.decodeResource(context.getResources(), resId, new BitmapFactory.Options());
    } catch (Error e) {
    } catch (Exception e2) {
    }
    return bm;
  }

  int getSourceIconID(int index){
    if(index == 0){
      return R.id.icon_1;
    }
    else  if(index == 1){
      return R.id.icon_2;
    }
    else  if(index == 2){
      return R.id.icon_3;
    }
    else  if(index == 3){
      return R.id.icon_4;
    }
      return R.id.icon_5;

  }

  private Class getMainActivityClass(Context context) {
    String packageName = context.getPackageName();
    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
    if (launchIntent == null || launchIntent.getComponent() == null) {
      return null;
    }
    try {
      return Class.forName(launchIntent.getComponent().getClassName());
    } catch (ClassNotFoundException e) {
      return null;
    }
  }
}
