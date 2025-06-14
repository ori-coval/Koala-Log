# FTC Auto Logger

**FTC Auto Logger** is a powerful logging system for FTC robots that produces `.wpilog` files fully compatible with [Advantage Scope](https://github.com/Mechanical-Advantage/AdvantageScope), enabling detailed log analysis and visualization.

---

## 📦 Components

### [FtcWpiLogger](FtcWpiLogger)
A FTC‑compatible logger. Major parts include:
- **AutoLogManager.java** – automatically registers and manages loggable objects.
- **WpiLog.java** – creates and manages `.wpilog` file sessions, handles data streams, and ensures proper formatting for Advantage Scope.
- **Logged.java** – interface marking classes whose data should be serialized and logged.

### [Logging‑Processor](Logging-processor)
Annotation processor that scans for `@AutoLog` annotations and generates corresponding `Logged` implementations, enabling automatic data capture.

### [LogPuller](LogPuller)
ADB-based tools for retrieving logs from the Control Hub:
- `FTCLogPuller.exe` – downloads logs without removing them.
- `PullAndDeleteLogs.exe` – downloads logs and deletes them from the hub.

### [LogPullerDevelopment](LogPullerDevelopment)
Build scripts and tooling for executable generation:
- PowerShell scripts for each EXE.
- `build_exe.bat` – compiles EXEs via PS2EXE.

