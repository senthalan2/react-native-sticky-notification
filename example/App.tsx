import { Button, PermissionsAndroid, Platform, View } from 'react-native';
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
        actionBackground: '#b3e9c6',
        actionBorderRadius: 30,
        actionIconTint: '#1DB954',
        actionLabelColor: '#0d43f5',

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
      <Button title="Start notification" onPress={start} />
      <Button
        title="Stop notification"
        onPress={() => StickyNotification.stopService()}
      />
    </View>
  );
};

export default App;
