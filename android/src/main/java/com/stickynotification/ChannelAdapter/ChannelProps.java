package com.stickynotification.ChannelAdapter;

public enum ChannelProps {

  CHANNEL_ID("channelId"),
  CHANNEL_NAME("channelName"),
  IMPORTANCE("importance"),
  TOTAL_PROCESS_BUTTONS_COUNT("totalProcessButtonsCount");

  private String value;

  ChannelProps(String value) {
    this.value = value;
  }
  public String value() {
    return value;
  }
}
