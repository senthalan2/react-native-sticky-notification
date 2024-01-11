declare module "react-native-sticky-notification" {
    export interface ChannelConfig {
        channelId: string,
        channelName: string,
        importance: "high" | "low" | "default",
        totalProcessCount: number
    }

    export interface ServiceConfig {
        displayTexts?: [string, string?, string?, string?, string?],
        displayIcons?: [number, number?, number?, number?, number?]
        exitEnabled?: boolean,
        icon?: "app-icon" | "app-icon-rounded" | "other"
    }

    export type NotificationPermissionResponse = {
        status: boolean,
        permissionStatus: 'granted' | 'denied' | 'never_ask_again'
    }

    export type ButtonClickResponseProps = {
        action: string
    }

    export type ServiceProps = {
        onPressButton: (event: ButtonClickResponseProps) => void;
    }

    declare const StickyNotificationService: React.SFC<ServiceProps>;

    export default StickyNotificationService;

    export function createChannel(config: ChannelConfig): Promise<string>;
    export function startService(config: ServiceConfig): Promise<string>;
    export function stopService(): Promise<string>;
    export function removeOnClickListener(): null;
    export function isGrantedNotificationPermission(): Promise<NotificationPermissionResponse>

}

