import { useEffect } from 'react';
import {
  NativeModules,
  Platform,
  DeviceEventEmitter,
  PermissionsAndroid,
} from 'react-native';
const LINKING_ERROR =
  `The package 'react-native-sticky-notification' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

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
  useEffect(() => {
    DeviceEventEmitter.addListener('action', (buttonName) => {
      onPressButton(buttonName);
    });
  }, []);

  return null;
};

export const removeOnClickListener = () => {
  DeviceEventEmitter.removeAllListeners();
};

export const createChannel = ({
  channelId,
  channelName,
  importance,
  totalProcessCount,
}) => {
  return new Promise((resolve, reject) => {
    if (!channelId) {
      reject('Channel Id is required!');
    } else if (typeof channelId != 'string') {
      reject('Channel Id must be String!');
    } else if (!channelName) {
      reject('Channel Name is required!');
    } else if (typeof channelName != 'string') {
      reject('Channel Name must be String!');
    } else if (totalProcessCount > 60) {
      reject('Total Process Count must be less than or equal to 60!');
    } else if (totalProcessCount <= 0) {
      reject('Total Process Count must be greater than 0!');
    } else {
      StickyNotification.createChannel({
        channelId: channelId,
        channelName: channelName,
        importance: importance,
        totalProcessButtonsCount: totalProcessCount,
      })
        .then((res) => {
          resolve(res);
        })
        .catch((error) => {
          reject(error);
        });
    }
  });
};

export const startService = ({
  displayTexts = ['b1', 'b2', 'b3', 'b4', 'b5'],
  displayIcons = [0, 1, 2, 3, 4],
  exitEnabled = false,
  icon = 'app-icon',
}) => {
  return new Promise((resolve, reject) => {
    if (!Array.isArray(displayTexts)) {
      reject('Invalid Display Texts List!');
    } else if (!Array.isArray(displayIcons)) {
      reject('Invalid Display Icons List!');
    } else if (displayTexts.length > 5) {
      reject('Length of Display Texts List must be less than or equal to 5!');
    } else if (displayTexts.length <= 0) {
      reject('Length of Display Texts List must be greater than 0!');
    } else if (displayIcons.length > 5) {
      reject('Length of Display Icons List must be less than or equal to 5!');
    } else if (displayIcons.length <= 0) {
      reject('Length of Display Icons List must be greater than 0!');
    } else if (displayTexts.length !== displayIcons.length) {
      reject(
        'Length of Display Icons List and Length of Display Texts List must be same!'
      );
    } else {
      StickyNotification.startService({
        displayTexts: displayTexts,
        displayIcons: [...displayIcons].map((itemIndex) =>
          itemIndex.toString()
        ),
        exitEnabled: exitEnabled,
        icon: icon,
      })
        .then((res) => {
          resolve(res);
        })
        .catch((err) => {
          reject(err);
        });
    }
  });
};

export const stopService = () => {
  return StickyNotification.stopService();
};

export const isGrantedNotificationPermission = () => {
  return new Promise(async (resolve, reject) => {
    if (Platform.OS === 'android' && Platform.Version >= 33) {
      try {
        PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
        )
          .then((res) => {
            if (res === 'granted') {
              resolve({
                status: true,
                permissionStatus: res,
              });
            } else {
              resolve({
                status: false,
                permissionStatus: res,
              });
            }
          })
          .catch((err) => {
            reject(err);
          });
      } catch (error) {
        reject(error);
      }
    } else {
      resolve({
        status: true,
        permissionStatus: 'granted',
      });
    }
  });
};

export default StickyNotificationService;
