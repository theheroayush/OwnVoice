# Google Play Console Data Safety Questionnaire Answers

Use these exact answers when filling out the **Data safety** form in Google Play Console:

---

### 1. Data Collection and Security
* **Does your app collect or share any of the required user data types?**  
  👉 **Yes** (Audio recordings for transcription functionality)
* **Is all of the user data collected by your app encrypted in transit?**  
  👉 **Yes** (All audio and API traffic uses TLS 1.3 / HTTPS)
* **Do you provide a way for users to request that their data be deleted?**  
  👉 **Yes** (Users can delete all local notes, transcripts, and API keys via Settings → Clear Data, or by uninstalling the application)

---

### 2. Data Types

#### Category: Audio Files (Voice or Sound Recordings)
* **Is this data collected, shared, or both?**  
  👉 **Collected** (Transiently processed for transcription)
* **Is this data processed ephemerally?**  
  👉 **Yes** (Audio buffers exist in volatile RAM strictly during dictation and are immediately purged after transcription)
* **Is this data required for your app, or can users choose whether it's collected?**  
  👉 **Data collection is required** (The primary function of the app is voice typing and transcription)
* **Why is this user data collected?**  
  👉 Check: **App functionality** (Voice dictation, transcription, and meeting mode)

#### All Other Categories (Personal info, Financial info, Health, Messages, Photos, Location, Contacts, Device IDs):
* 👉 **No data collected or shared**

---

### 3. Target Audience & Content Rating
* **Target age group:** 18 and older
* **Contains ads:** **No**
* **In-app purchases:** No (Software licenses and BYOK API keys managed independently)
* **Content Rating (IARC):** Everyone / PEGI 3 (Utility / Productivity keyboard tool)
