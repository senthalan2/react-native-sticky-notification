package com.reactnativestickynotification.ChannelAdapter;
import static com.reactnativestickynotification.ChannelAdapter.ChannelProps.IMPORTANCE;
import static com.reactnativestickynotification.ChannelAdapter.ChannelProps.CHANNEL_NAME;
import static com.reactnativestickynotification.ChannelAdapter.ChannelProps.CHANNEL_ID;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;

import javax.annotation.Nullable;

public class CreateChannelAdapter implements CreateChannelProps{

  public ReadableMap props;
  public Promise promise;
  public CreateChannelAdapter(@Nullable ReadableMap props, Promise promise) {
    this.props = props;
    this.promise = promise;

  }

  @Override
  public String channelId() {
    return getStringValue(CHANNEL_ID,"sticky_notification_service");
  }

  @Override
  public String channelName() {
    return getStringValue(CHANNEL_NAME,"sticky_notification_service");
  }

  @Override
  public String importance() {
    return getStringValue(IMPORTANCE,"default");
  }

  @Override
  public int totalProcessButtonsCount() {
    return getTotalCount();
  }

  private String getStringValue(ChannelProps prop, String defaultValue) {
    return props.hasKey(prop.value()) ? props.getString(prop.value()) : defaultValue;
  }

  private int getTotalCount() {

    if(props.hasKey(ChannelProps.TOTAL_PROCESS_BUTTONS_COUNT.value())){
      if( props.getInt(ChannelProps.TOTAL_PROCESS_BUTTONS_COUNT.value()) <= 40 && props.getInt(ChannelProps.TOTAL_PROCESS_BUTTONS_COUNT.value()) > 0){
        return props.getInt(ChannelProps.TOTAL_PROCESS_BUTTONS_COUNT.value());
      }
      return 40;
    }
    return 40;
  }
}
