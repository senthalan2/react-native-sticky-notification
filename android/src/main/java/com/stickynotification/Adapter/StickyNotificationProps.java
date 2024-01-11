package com.stickynotification.Adapter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public interface StickyNotificationProps {

  String[] displayTexts();
  ArrayList<String> displayIcons();
  Boolean exitEnabled();
  int buttonsCount();
  int iconsCount();
  String icon();
  void onPress(String clickedButton);
}
