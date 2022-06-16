export interface ChannelConfig {
    channelId: number,
    channelName: string,
    displayTexts:["b1","b2","b3","b4","b5"] | undefined,
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
export function startService():Promise<string>;
export function stopService():Promise<string>;


// declare module "react-native-sticky-notification" {
    
//     export type channelConfig ={
//         channelId: number,
//         channelName: string,
//         displayTexts:["b1","b2","b3","b4","b5"] | undefined,
//         exitEnabled: Boolean,
//         icon: "app-icon" | "app-icon-rounded" | "other"
//     }

//     export type responseProps = {
//         action:String
//     }

//     export type serviceProps ={
//         onPressButton: () => responseProps;
//     }
  
    

//     export function createChannel(config:channelConfig): Promise;
//     export function startService():Promise;
//     export function stopService():Promise;
    
   
// }