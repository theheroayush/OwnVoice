
Set oWS = WScript.CreateObject("WScript.Shell")
sLinkFile = "C:\Users\ayush\OneDrive\Desktop\VoiceTranscriber.lnk"
Set oLink = oWS.CreateShortcut(sLinkFile)
oLink.TargetPath = "C:\Users\ayush\AppData\Local\Programs\Python\Python311-arm64\pythonw.exe"
oLink.Arguments = ""C:\Users\ayush\AppData\Local\VoiceTranscriber\app.py""
oLink.WorkingDirectory = "C:\Users\ayush\AppData\Local\VoiceTranscriber"
oLink.Description = "Wispr Flow — Voice Dictation & Auto-Typing"
oLink.IconLocation = "C:\Users\ayush\AppData\Local\VoiceTranscriber\assets\icon.ico, 0"
oLink.Save
