import React, { useEffect } from 'react';

import { StyleSheet, View, Text, TouchableOpacity } from 'react-native';
import StickyNotificationService, {
  createChannel,
  stopService,
  startService,
  removeOnClickListener,
  type ButtonClickResponseProps,
  type ChannelConfig,
  type ServiceConfig,
  isGrantedNotificationPermission,
  type NotificationPermissionResponse,
} from 'react-native-sticky-notification';

const CHANNEL_ID = 'sampleproject';
const CHANNEL_NAME = 'sampleproject';

export default function App() {
  let channelConfig: ChannelConfig = {
    channelId: CHANNEL_ID, //required
    channelName: CHANNEL_NAME, //required
    importance: 'default', // default importance is "default"
    totalProcessCount: 4, // Total Icons which are added in android drawable,
  };

  let serviceConfig: ServiceConfig = {
    displayTexts: ['abc', 'bcd', 'FFF', 'HHH'], //default value is ["b1","b2","b3","b4","b5"] ->>>>>>> max array length 5
    displayIcons: [0, 1, 2, 3], // Icons Indices  - - ->>>> starts from 0 to ( totalProcessCount - 1 ) default value is [0,1,2,3,4] ->>>>>>> max array length 5
    exitEnabled: false, //default value is false            //If true Service stopped when click the last button
    icon: 'app-icon', //1. app-icon 2.app-icon-rounded 3.other  //default "app-icon"
  };

  useEffect(() => {
    isGrantedNotificationPermission() // For Android 13 and above this permission is required
      .then((res: NotificationPermissionResponse) => {
        console.log(res, 'RESPONSE');
      })
      .catch((e: any) => {
        console.log(e, 'ERROR');
      });
  }, []);

  useEffect(() => {
    return () => {
      removeOnClickListener();
    };
  }, []);

  const onPressButton = (clickedButton: ButtonClickResponseProps) => {
    console.log(clickedButton, 'onPressed');
  };

  return (
    <View style={styles.container}>
      <StickyNotificationService onPressButton={onPressButton} />

      <TouchableOpacity
        style={styles.button}
        onPress={() => {
          createChannel(channelConfig)
            .then((e) => {
              console.log(e);
            })
            .catch((e) => {
              console.log(e);
            });
        }}
      >
        <Text>Create Channel</Text>
      </TouchableOpacity>
      <TouchableOpacity
        style={styles.button}
        onPress={() => {
          startService(serviceConfig)
            .then((res) => {
              console.log(res, 'response');
            })
            .catch((err) => {
              console.log(err, 'error');
            });
        }}
      >
        <Text>Start Service</Text>
      </TouchableOpacity>
      <TouchableOpacity
        style={styles.button}
        onPress={() => {
          stopService()
            .then((res) => {
              console.log(res, 'res');
            })
            .catch((err) => {
              console.log(err, 'err');
            });
        }}
      >
        <Text>Stop Service</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
  button: {
    height: 50,
    padding: 10,
    borderRadius: 10,
    backgroundColor: '#7FFFD4',
    elevation: 3,
    marginVertical: 10,
  },
});
