package com.stickynotification.Adapter;

import static com.stickynotification.Adapter.RNProps.DISPLAY_ICONS;
import static com.stickynotification.Adapter.RNProps.DISPLAY_TEXTS;
import static com.stickynotification.Adapter.RNProps.EXIT_ENABLED;
import static com.stickynotification.Adapter.RNProps.ICON;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.stickynotification.StickyNotificationModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import javax.annotation.Nullable;

public class StickyNotificationAdapter implements StickyNotificationProps {

  public ReadableMap props;
  public Promise promise;
  public int buttonsCount;
  public int iconsCount;

  public StickyNotificationAdapter(@Nullable ReadableMap props, Promise promise) {
    this.props = props;
    this.promise = promise;


  }

  @Override
  public String[] displayTexts() {
    return getStringArray(DISPLAY_TEXTS);
  }

  @Override
  public ArrayList<String> displayIcons() {
    return getIcons(DISPLAY_ICONS);
  }


  @Override
  public Boolean exitEnabled() {
    return !props.hasKey(EXIT_ENABLED.value()) || props.getBoolean(EXIT_ENABLED.value());

  }


  @Override
  public int buttonsCount() {
    return buttonsCount;
  }

  @Override
  public int iconsCount() {
    return iconsCount;
  }

  @Override
  public String icon() {
    return getStringValue(ICON,"app-icon");
  }

  @Override
  public void onPress(String clickedButton) {
    WritableMap map = Arguments.createMap();
    map.putString("action", clickedButton);

    try {
      StickyNotificationModule.reactContext.
        getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit("action", map);
      promise.resolve(clickedButton);

    } catch (Exception e){

      promise.reject("Error",e.getMessage());
    }

  }


  public ArrayList<String> getIcons(RNProps prop){
    ArrayList<String> displayIcons = new ArrayList<>();
    for(int i=0;i<displayTexts().length;i++){
      displayIcons.add(Integer.toString(i+1));
    }

    if(props.hasKey(prop.value())){

      ArrayList<Object> iconsList = Objects.requireNonNull(props.getArray("displayIcons")).toArrayList();
      iconsCount=iconsList.toArray().length;
      displayIcons.clear();
      for(int i=0;i<iconsList.toArray().length;i++){
        displayIcons.add(iconsList.get(i).toString());
      }
      return displayIcons;
    }
    return displayIcons;
  }


  public String[] getStringArray(RNProps prop){
    String[] displayTexts = new String[]{"b1", "b2", "b3", "b4", "b5"};

    if(props.hasKey(prop.value())){

      ArrayList<Object> str = Objects.requireNonNull(props.getArray("displayTexts")).toArrayList();
      buttonsCount=str.toArray().length;
      for(int i=0;i<str.toArray().length;i++){
        displayTexts[i]=str.get(i).toString();
      }

      return displayTexts;
    }
    return displayTexts;
  }


  private String getStringValue(RNProps prop, String defaultValue) {
    return props.hasKey(prop.value()) ? props.getString(prop.value()) : defaultValue;
  }
}
