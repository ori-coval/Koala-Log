# FTC Auto Logger

**FTC Auto Logger** is a logging framework for FTC robots that generates `.wpilog` files fully compatible with [Advantage Scope](https://docs.advantagescope.org). It enables rich telemetry, data capture, and visual analysis — just like in FRC.

[![](https://jitpack.io/v/ori-coval/ftc-auto-logger.svg)](https://jitpack.io/#ori-coval/ftc-auto-logger)

---

## 🚀 How to Add to Your FTC Project

To start logging FTC data in the `.wpilog` format:

1. **Add the JitPack repository** to your `TeamCode/build.gradle` below the android block:

   ```groovy
   android {
       // your existing config...
   }

   repositories {
       maven { url 'https://jitpack.io' }
   }
   ```

2. **Add the dependencies** inside the same `build.gradle`:

   ```groovy
   dependencies {
       implementation 'com.github.ori-coval.Koala-Log:FtcWpiLogger:v1.0.0'
       annotationProcessor 'com.github.ori-coval.Koala-Log:logging-processor:v1.0.0'
   }
   ```

   Replace `<version>` with the latest version shown below:  
   [![](https://jitpack.io/v/ori-coval/ftc-auto-logger.svg)](https://jitpack.io/#ori-coval/ftc-auto-logger)

---

## 📦 Project Structure

### [`FtcWpiLogger`](FtcWpiLogger)
The core runtime library used in your robot code:
- **`AutoLogManager.java`** – Registers and manages all loggable instances.
- **`WpiLog.java`** – Manages `.wpilog` files, handles timestamps, and serializes data.
- **`Logged.java`** – Interface for objects that should be recorded in the log.

### [`Logging-Processor`](Logging-processor)
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
Open an issue or a pull request here: [https://github.com/ori-coval/ftc-auto-logger](https://github.com/ori-coval/ftc-auto-logger)
