# FTC Auto Logger

**KoalaLog** is a logging framework for FTC robots that generates `.wpilog` files fully compatible with [Advantage Scope](https://docs.advantagescope.org). It enables rich telemetry and data capture — just like in FRC.

[![](https://jitpack.io/v/ori-coval/Koala-Log.svg)](https://jitpack.io/#ori-coval/Koala-Log)

---

## 🚀 Getting Started

To get started with KoalaLog, follow the setup instructions in the wiki:

📖 **[KoalaLog Wiki](https://github.com/ori-coval/Koala-Log/wiki)**  
- [How to Add to Your FTC Project](https://github.com/ori-coval/Koala-Log/wiki/1.-How-to-add-to-project)  
- [How to Use in Your Code](https://github.com/ori-coval/Koala-Log/wiki/2.-how-to-use-in-your-code)  
- [How to Get the Log](https://github.com/ori-coval/Koala-Log/wiki/3.-how-to-get-the-log)

---

## 📦 Project Structure

### [`KoalaLogger`](KoalaLogger)
The core runtime library used in your robot code:
- **`AutoLogManager.java`** – Registers and manages all loggable instances.
- **`WpiLog.java`** – Manages `.wpilog` files, handles timestamps, and serializes data.
- **`Logged.java`** – Interface for objects that should be recorded in the log.

### [`KoalaLoggingProcessor`](KoalaLoggingProcessor)
Annotation processor for generating logging boilerplate:
- Automatically processes `@AutoLog` annotations.
- Generates `Logged` interface implementations at compile time.

### [`LogPuller`](LogPuller)
Tools to retrieve logs from the Control Hub over ADB:
- `FTCLogPuller.exe` – Pull logs without deleting.
- `PullAndDeleteLogs.exe` – Pull logs and clean up the hub afterward.

### [`LogPullerDevelopment`](LogPullerDevelopment)
Build system and scripts to generate `.exe` tools:
- PowerShell scripts used for packaging.
- `build_exe.bat` – Converts `.ps1` scripts to `.exe` using PS2EXE.

---

## 🙌 Contributions & Support

Want to improve or contribute? Found a bug?  
Open an issue or a pull request here: [https://github.com/ori-coval/Koala-Log](https://github.com/ori-coval/Koala-Log)
