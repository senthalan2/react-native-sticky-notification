package com.stickynotification.Adapter;


public enum RNProps {


  DISPLAY_TEXTS("displayTexts"),
  DISPLAY_ICONS("displayIcons"),
  EXIT_ENABLED("exitEnabled"),
  ICON("icon");
  private String value;

  RNProps(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
