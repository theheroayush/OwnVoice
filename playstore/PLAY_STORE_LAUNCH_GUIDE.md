# Google Play Store Complete Launch Guide for OwnVoice

**Package Name:** `com.ownvoice.app`  
**Version:** `2.5.6` (Version Code `17`)  
**Domain:** `https://ownvoice.in`  
**GitHub Repository:** `https://github.com/theheroayush/OwnVoice`  
**Privacy Policy URL:** `https://github.com/theheroayush/OwnVoice/blob/main/PRIVACY_POLICY.md`  
**Support Email:** `support@ownvoice.in`

---

## 📁 Directory Structure & Deliverables

Everything you need to upload to Google Play Console is organized in this `playstore/` directory:

```
playstore/
├── bundle/
│   └── app-release.aab              <-- Upload this Android App Bundle to Play Console
├── apk/
│   └── app-release.apk              <-- Standalone signed APK for direct testing
├── graphics/
│   ├── icon_512x512.png             <-- 512x512 App Icon (32-bit PNG, meets all Play specs)
│   ├── feature_graphic_1024x500.png <-- 1024x500 Banner Graphic (No transparency)
│   └── screenshots/
│       ├── screenshot_1_voice_keyboard.png  <-- 1080x2400 Phone Screenshot 1
│       ├── screenshot_2_air_typing_pc.png   <-- 1080x2400 Phone Screenshot 2
│       ├── screenshot_3_meeting_mode.png    <-- 1080x2400 Phone Screenshot 3
│       └── screenshot_4_context_tone.png    <-- 1080x2400 Phone Screenshot 4
├── store_listing/
│   ├── app_title.txt                <-- "OwnVoice: AI Voice Keyboard" (27 chars)
│   ├── short_description.txt        <-- 75 chars (Under 80 char limit)
│   ├── full_description.txt         <-- Formatted conversion copy with feature breakdowns
│   ├── release_notes.txt            <-- What's new in version 2.5.6 (Build 17)
│   ├── listing_metadata.json        <-- Full metadata spec
│   ├── data_safety_declaration.md   <-- Exact answers for the Data Safety form
│   └── target_audience_and_content_rating.md <-- Exact answers for IARC rating
└── PLAY_STORE_LAUNCH_GUIDE.md        <-- This launch checklist
```

---

## 🚀 Step-by-Step Google Play Console Submission

### Step 1: Create Application in Google Play Console
1. Log in to [Google Play Console](https://play.google.com/console).
2. Click **Create app** (top-right).
3. Fill in the initial modal:
   - **App name**: `OwnVoice: AI Voice Keyboard` (from `store_listing/app_title.txt`)
   - **Default language**: `English (United States) - en-US`
   - **App or game**: Select **App**
   - **Free or paid**: Select **Free**
   - Accept the Developer Program Policies and US export laws checkboxes.
4. Click **Create app**.

---

### Step 2: Set Up Store Listing & Visual Assets
Navigate to **Grow → Store presence → Main store listing** in the left sidebar:

1. **Listing Details**:
   - **App name**: Copy from `store_listing/app_title.txt`
   - **Short description**: Copy from `store_listing/short_description.txt`
   - **Full description**: Copy from `store_listing/full_description.txt`
2. **Graphics**:
   - **App icon**: Upload `playstore/graphics/icon_512x512.png`
   - **Feature graphic**: Upload `playstore/graphics/feature_graphic_1024x500.png`
   - **Phone screenshots**: Upload all 4 screenshots from `playstore/graphics/screenshots/`:
     - `screenshot_1_voice_keyboard.png`
     - `screenshot_2_air_typing_pc.png`
     - `screenshot_3_meeting_mode.png`
     - `screenshot_4_context_tone.png`
3. Click **Save**.

---

### Step 3: Complete App Content Declarations
Navigate to **Policy and programs → App content** in the left sidebar and complete the required tasks:

1. **Privacy policy**:
   - Paste URL: `https://github.com/theheroayush/OwnVoice/blob/main/PRIVACY_POLICY.md`  
     *(Once your custom domain is live, you can also update to `https://ownvoice.in/privacy`)*.
2. **App access**:
   - Select: **All functionality is available without special access**.
3. **Ads**:
   - Select: **No, my app does not contain ads**.
4. **Content rating (IARC)**:
   - Email address: `support@ownvoice.in`
   - Category: **Utility, Productivity, Communication, or other**
   - Violence, sexual content, offensive language, controlled substances: Select **No** to all.
   - Click Save and calculate rating (will yield **Everyone / PEGI 3**).
5. **Target audience and content**:
   - Target age: Check **18 and over**.
   - Could your app unintentionally appeal to children: Select **No**.
6. **Data safety**:
   - Refer to `store_listing/data_safety_declaration.md` for exact answers:
     - Data collected: **Yes** (Audio recordings for voice transcription functionality).
     - Ephemeral processing: **Yes** (audio is processed in-memory and immediately purged).
     - Data encrypted in transit: **Yes** (TLS 1.3).
     - Account deletion / data deletion: **Yes** (Settings → Clear Data).
7. **Accessibility Service Declaration**:
   - In the Accessibility tool declaration form: Check that `OverlayAccessibilityService` is used as an accessibility tool to paste transcribed voice text directly into user-focused text fields when using the floating bubble overlay hands-free.

---

### Step 4: Create Production Release & Upload .aab
1. In the left sidebar, navigate to **Release → Production** (or **Testing → Internal testing** for initial device smoke tests).
2. Click **Create new release**.
3. Under **App bundles**:
   - Click **Upload** and select `playstore/bundle/app-release.aab`.
4. Under **Release name**:
   - Enter: `2.5.6 (17)`
5. Under **Release notes**:
   - Copy and paste the text from `store_listing/release_notes.txt`.
6. Click **Next** → **Save and review release**.
7. If there are no blocking errors, click **Start rollout to Production**!

---

## 🔒 Signing Keystore Information
The release App Bundle is signed with the embedded production keystore:
- **Keystore File:** `android/app/release.keystore`
- **Keystore Password:** `ownvoice123`
- **Key Alias:** `ownvoice`
- **Key Password:** `ownvoice123`
- **Google Play App Signing:** When prompted in Google Play Console, accept Google Play App Signing (Play Console will automatically enroll this key and manage production distribution).
