package com.reactnativestickynotification;


import android.app.NotificationChannel;
import android.app.NotificationManager;

import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.reactnativestickynotification.Adapter.StickyNotificationAdapter;
import com.reactnativestickynotification.Adapter.StickyNotificationProps;
import com.reactnativestickynotification.ChannelAdapter.CreateChannelAdapter;
import com.reactnativestickynotification.ChannelAdapter.CreateChannelProps;

import java.util.ArrayList;


@ReactModule(name = StickyNotificationModule.NAME)
public class StickyNotificationModule extends ReactContextBaseJavaModule {
  public static final String NAME = "StickyNotification";
  public static ReactApplicationContext reactContext;
  public static String CHANNEL_ID = null;
  public static ArrayList<Integer> ICONS_LIST = new ArrayList<>();
  public static StickyNotificationProps props;
  public static CreateChannelProps channelProps;
  public static Promise promise = null;

  public StickyNotificationModule(ReactApplicationContext reactContext) {
    super(reactContext);
    StickyNotificationModule.reactContext = reactContext;
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }


  @RequiresApi(api = Build.VERSION_CODES.N)
  int getImportance(String value){
    if(value.equals("high")){
      return NotificationManager.IMPORTANCE_HIGH;
    }
    else if(value.equals("low")){
      return NotificationManager.IMPORTANCE_LOW;
    }
    else{
      return NotificationManager.IMPORTANCE_DEFAULT;
    }

  }


  @ReactMethod
  public void createChannel(ReadableMap options, Promise promise) {
    if(options!=null){
      try{
        StickyNotificationModule.channelProps = new CreateChannelAdapter(options,promise);
        int totalProcessButtonsCount = options.getInt("totalProcessButtonsCount");
        ICONS_LIST.clear();
        for(int i=0;i<totalProcessButtonsCount;i++){
          ICONS_LIST.add(getIcon(i));
        }
        CHANNEL_ID = channelProps.channelId(); //Set for global use
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          NotificationChannel serviceChannel = new NotificationChannel(
            channelProps.channelId(),
            channelProps.channelName(),
            getImportance(channelProps.importance())
          );
          NotificationManager manager = reactContext.getSystemService(NotificationManager.class);
          manager.createNotificationChannel(serviceChannel);
        }
        promise.resolve("Channel Created Successfully");
      }
      catch (Exception e){
        promise.reject("Error",e.getMessage());
      }
    }
    else{
      promise.reject("Error","Values must not be Null");
    }
  }

  @ReactMethod
  public void startService(ReadableMap options,Promise promise) {
    StickyNotificationModule.promise = promise;
    try{
      if(CHANNEL_ID!=null){

        StickyNotificationModule.props = new StickyNotificationAdapter(options,promise);

        if(props.displayIcons().size() <= channelProps.totalProcessButtonsCount()){
          if(isValid(props.displayIcons(),channelProps.totalProcessButtonsCount())){
            Intent intent = new Intent(reactContext,StickyNotificationService.class);
            intent.setAction("startService");
            reactContext.startService(intent);
            promise.resolve("SERVICE_STARTED");
          }
          else{
            promise.reject("SERVICE_NOT_STARTED","Display Icons List contains out of range or invalid index!");
          }

        }
        else{
          promise.reject("SERVICE_NOT_STARTED","Total Process Count must be greater than or equal to the length of Display Icons List and Display Texts List!");
        }

      }
      else{
        promise.reject("SERVICE_NOT_STARTED","Invalid Channel Id");
      }
    }
    catch(Exception e){
      promise.reject("SERVICE_NOT_STARTED",e.getMessage());
    }
  }


  @ReactMethod
  public void stopService(Promise promise) {
    try{
      Intent intent = new Intent(reactContext,StickyNotificationService.class);
      reactContext.stopService(intent);
      promise.resolve("SERVICE_STOPPED");
    }
    catch(Exception e){
      promise.reject("ERROR",e.getMessage());
    }

  }

  int getIcon(int index){
    if(index == 0){
      return R.drawable.img1;
    }
    else  if(index == 1){
      return R.drawable.img2;
    }
    else  if(index == 2){
      return R.drawable.img3;
    }
    else  if(index == 3){
      return R.drawable.img4;
    }
    else  if(index == 4){
      return R.drawable.img5;
    }
    else  if(index == 5){
      return R.drawable.img6;
    }
    else  if(index == 6){
      return R.drawable.img7;
    }
    else  if(index == 7){
      return R.drawable.img8;
    }
    else  if(index == 8){
      return R.drawable.img9;
    }
    else  if(index == 9){
      return R.drawable.img10;
    }
    else  if(index == 10){
      return R.drawable.img11;
    }
    else  if(index == 11){
      return R.drawable.img12;
    }
    else  if(index == 12){
      return R.drawable.img13;
    }
    else  if(index == 13){
      return R.drawable.img14;
    }
    else  if(index == 14){
      return R.drawable.img15;
    }
    else  if(index == 15){
      return R.drawable.img16;
    }
    else  if(index == 16){
      return R.drawable.img17;
    }
    else  if(index == 17){
      return R.drawable.img18;
    }
    else  if(index == 18){
      return R.drawable.img19;
    }
    return R.drawable.img20;
  }


  private boolean isValid(ArrayList<String> displayIcons, int totalProcessButtonsCount) {
    ArrayList<Integer> tempDisplayIcons = new ArrayList<>();
    for(int i = 0;i<displayIcons.size();i++){
      tempDisplayIcons.add(Integer.parseInt((displayIcons.get(i))));
    }
    int tempValue;
    for(int i=0;i<tempDisplayIcons.size();i++){
      for(int j=i+1;j<tempDisplayIcons.size();j++){
        if (tempDisplayIcons.get(i) > tempDisplayIcons.get(j))
        {
          tempValue = tempDisplayIcons.get(i);
          tempDisplayIcons.set(i,tempDisplayIcons.get(j));
          tempDisplayIcons.set(j,tempValue);
        }
      }
    }
    return tempDisplayIcons.get(tempDisplayIcons.size() - 1) < totalProcessButtonsCount && tempDisplayIcons.get(0) >= 0;
  }
}
