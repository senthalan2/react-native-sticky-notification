import { NativeModules, Platform, DeviceEventEmitter } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-sticky-notification' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

const StickyNotification = NativeModules.StickyNotification
  ? NativeModules.StickyNotification
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

const StickyNotificationService = ({ onPressButton }) => {
  DeviceEventEmitter.addListener('action', (buttonName) => {
    onPressButton(buttonName);
  });

  return null;
};

export const removeOnClickListener = () => {
  DeviceEventEmitter.removeAllListeners();
};

export const createChannel = ({ channelId, channelName, ...props }) => {
  return new Promise((resolve, reject) => {
    if (!channelId) {
      reject('Channel Id is required!');
      return;
    }

    if (!channelName) {
      reject('Channel Name is required!');
      return;
    }

    if (typeof channelId != 'string') {
      reject('Channel Id must be String!');
      return;
    }
    if (typeof channelName != 'string') {
      reject('Channel Name must be String!');
      return;
    }

    StickyNotification.createChannel({
      channelId: channelId,
      channelName: channelName,
      ...props,
    })
      .then((res) => {
        resolve(res);
      })
      .catch((error) => {
        reject(error);
      });
  });
};

export const startService = () => {
  return StickyNotification.startService();
};

export const stopService = () => {
  return StickyNotification.stopService();
};

export default StickyNotificationService;
