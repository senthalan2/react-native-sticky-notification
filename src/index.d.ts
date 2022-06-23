export interface ChannelConfig {
    channelId: string,
    channelName: string,
    importance : "high" | "low" | "default",
    totalProcessCount:number
}

export interface ServiceConfig{
    displayTexts:["b1","b2","b3","b4","b5"] | undefined,
    displayIcons:[0,1,2,3,4] | undefined
    exitEnabled: Boolean,
    icon: "app-icon" | "app-icon-rounded" | "other"
}

export type ButtonClickResponseProps = {
    action:String
}

export type ServiceProps ={
    onPressButton: (event:ButtonClickResponseProps) => void;
}

declare const StickyNotificationService: React.SFC<ServiceProps>;

export default StickyNotificationService;

export function createChannel(config:ChannelConfig):Promise<string>;
export function startService(config:ServiceConfig):Promise<string>;
export function stopService():Promise<string>;
export function removeOnClickListener():null;


