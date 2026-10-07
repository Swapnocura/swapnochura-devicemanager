# Swapnochura Device Manager

Package: `com.swapnochura.devicemanager`

## এই project কী করে

- Firebase Authentication-এর Anonymous sign-in ব্যবহার করে device-side session তৈরি করে।
- Firestore-এর `devices` collection-এ device registration রাখে।
- `lockCommands` collection-এ `PENDING` command শোনে।
- `LOCK` command এ Android `DevicePolicyManager.lockNow()` ব্যবহার করে।
- Device Owner/DPC status রিপোর্ট করে।

Android-এর `DevicePolicyManager.lockNow()` বাস্তবে শক্তিশালী lock operation; এটি Device Owner/Profile Owner/অনুমোদিত policy context ছাড়া নির্বিচারে ব্যবহার করা যায় না। Android Enterprise provisioning-এ DPC component-কে Device Owner করা হয়। 

## Firebase আগে প্রস্তুত করুন

Firebase Console:
1. Project: `swapnochura-lock-management`
2. Authentication → Sign-in method → Anonymous → Enable
3. Project settings → Your apps → Add app → Android
4. Android package name দিন:
   `com.swapnochura.devicemanager`
5. SHA-1/SHA-256 প্রয়োজন হলে আপনার debug/release keystore fingerprint যোগ করুন।
6. `google-services.json` download করে `app/google-services.json`-এ রাখুন।

### গুরুত্বপূর্ণ
বর্তমান source manual Firebase initialization ব্যবহার করছে না; Firebase Android app registration সম্পূর্ণ করার পর Google Services plugin যোগ করার পরামর্শ দেওয়া হচ্ছে। এই starter project-এর Firebase dependency version Firebase BoM `34.19.0`, যা বর্তমান Firebase Android documentation-এর latest listed BoM। 

## Firebase Auth

এই app Anonymous Authentication ব্যবহার করে। Firebase Console-এ Anonymous provider অবশ্যই enabled থাকতে হবে।

## Firestore collections

### devices/{deviceId}

Example fields:
- deviceId
- enrollmentId
- customerId
- name
- manufacturer
- model
- androidVersion
- sdkInt
- status
- managementStatus
- lockStatus
- lastSeen
- authUid

### lockCommands/{commandId}

Admin panel থেকে:
- deviceId
- command: `LOCK`, `UNLOCK`, বা `PING`
- status: `PENDING`
- createdAt
- createdBy

App processing-এর পরে:
- status: `EXECUTED` বা `FAILED`
- processedAt
- deviceMessage

## Android Enterprise provisioning

এই app-এর `SwapnochuraDeviceAdminReceiver` হল DPC/device-admin receiver।

Provisioning QR-এ পরবর্তীতে Android-এর:
`android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME`

এর value হবে:

`com.swapnochura.devicemanager/.SwapnochuraDeviceAdminReceiver`

এবং APK download URL ও checksum যোগ করতে হবে।

**শুধু এই QR JSON বানালেই সাধারণ already-setup ফোন Device Owner হবে না।** Device Owner provisioning সাধারণত device setup/provisioning flow-এ করতে হয়।

## Current limitation

`UNLOCK` command ইচ্ছাকৃতভাবে remote keyguard bypass করে না। Generic Android DPC-এর জন্য নিরাপদ/সঠিক implementation নির্ভর করে আপনার managed-device policy ও OEM capabilities-এর ওপর।

## Next step

এই project Firebase Android app registration-এর পরে build করুন। তারপর APK-এর HTTPS download URL বের হলে Admin Panel-এর QR generator-কে Android Enterprise provisioning QR format-এ বদলানো হবে।
