import {
  PermissionsAndroid,
  Platform,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { useEffect, useRef } from 'react';
import StickyNotification, {
  ActionPressEvent,
} from 'react-native-sticky-notification';

const App = () => {
  const subscription = useRef<{ remove: () => void } | null>(null);

  useEffect(() => {
    subscription.current = StickyNotification.addActionListener(
      (event: ActionPressEvent) => {
        console.log('Action pressed:', event.actionId, event.payload);

        if (event.actionId === 'stop') {
          StickyNotification.stopService();
        }
      }
    );

    return () => {
      subscription.current?.remove();
    };
  }, []);
  useEffect(() => {
    async function requestNotificationPermission(): Promise<boolean> {
      if (Platform.OS !== 'android' || Platform.Version < 33) return true;

      const result = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
      );
      return result === PermissionsAndroid.RESULTS.GRANTED;
    }
    requestNotificationPermission();
  }, []);

  const start = async () => {
    try {
      await StickyNotification.startService({
        // title: 'Music Player',
        // text: 'Now playing: Awesome Song',
        smallIcon: 'ic_notification',
        color: '#1DB954',
        ongoing: true,
        closeOnAction: true,
        showDivider: false,
        openAppOnAction: true,
        actionLabelColor: '#2200ff',
        actionIconTint: '#1DB954',
        actionSpacing: 4,
        actionIconSpacing: 15,
        actionBorderRadius: 10,
        actionsContainerBackground: '#f59074',
        collapsedActions: [
          { id: 'prev', title: 'Prev', icon: 'ic_skip_previous' },
          { id: 'pause', title: 'Pause', icon: 'ic_pause' },
          { id: 'next', title: 'Next', icon: 'ic_skip_next' },
          { id: 'next', title: 'Next', icon: 'ic_skip_next' },
          { id: 'next', title: 'Next', icon: 'ic_skip_next' },
        ],
        showLabelsInCollapsed: false, // icon-only in collapsed (default)
        buttonsPerRow: 5,
        containerBorderRadius: 10,
        actions: [
          {
            id: 'prev',
            title: 'Prev',
            icon: 'ic_skip_previous',
          },
          { id: 'pause', title: 'Pause', icon: 'ic_pause' },
          { id: 'next', title: 'Next', icon: 'ic_skip_next' },
          {
            id: 'prev',
            title: 'Prev',
            icon: 'ic_skip_previous',
          },
          {
            id: 'prev',
            title: 'Prev',
            icon: 'ic_skip_previous',
          },
        ],
      });
      console.log('Started');
    } catch (e) {
      console.log(e, 'ERRORRR');
    }
  };

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <TouchableOpacity activeOpacity={0.6} style={styles.btn} onPress={start}>
        <Text style={styles.btnText}>Start Service</Text>
      </TouchableOpacity>
      <TouchableOpacity
        activeOpacity={0.6}
        style={styles.btn}
        onPress={() => StickyNotification.stopService()}
      >
        <Text style={styles.btnText}>Stop Service</Text>
      </TouchableOpacity>
    </View>
  );
};

export default App;

const styles = StyleSheet.create({
  btn: {
    padding: 10,
    borderRadius: 15,
    marginVertical: 15,
    backgroundColor: '#2eb6b6',
  },
  btnText: {
    fontWeight: 'bold',
    color: 'white',
  },
});
