# 📦 kurirpaket-java

A helpful tool to simulate a log traffic to your syslog server using your provided dummy log

kurirpaket is a lightweight Java application for replaying device/application logs to a Syslog server.
It reads from local log files, sends them line-by-line over UDP or TCP, and loops when the end of the file is reached.

Easily configurable via a simple kurirpaket.conf file — no rebuild required when changing configuration or logs.

## ✨ Features

Send logs from multiple sources to different Syslog servers.

Supports UDP and TCP syslog protocols.

Configurable interval between log messages.

Enable/disable each log source via config file.

External configuration and logs (not bundled in the JAR).

Simple to deploy, runs as a standalone .jar.

```
📂 Project Structure
kurirpaket/
├── jdk/                      # Bundled JDK 24
├── kurirpaket.conf           # App configuration (external, editable)
├── kurirpaket.bat            # Bat file. sh for Linux
├── 1.0.0-SNAPSHOT.jar        # Java archive file
├── source-logs/              # Example log files (external, editable)
│   ├── fortinet.log
│   └── kaspersky.log
└── src/
    └── main/java/com/kurirpaket/
        └── Main.java         # Application source code
```

## ⚙️ Configuration (kurirpaket.conf)

- Define one or more log sources. Each block supports the following fields:

- logid: Unique identifier for the log source.

- enabled: true or false — control whether the source runs.

- log_dir: Path to the log file to replay.

- syslog_host: Destination Syslog server IP/hostname.

- syslog_port: Destination port.

- syslog_protocol: udp or tcp.

- interval: Time between sending log lines (e.g., 1s, 500ms, 1m).

### Example

```
- logid: fortinet
  enabled: true
  log_dir: source-logs/fortinet.log
  syslog_host: 192.168.254.204
  syslog_port: 12050
  syslog_protocol: tcp
  interval: 1s

- logid: kaspersky
  enabled: false
  log_dir: source-logs/kaspersky.log
  syslog_host: 192.168.254.204
  syslog_port: 12051
  syslog_protocol: tcp
  interval: 1s
```

## 🏗️ Build Instructions
### Prerequisites

- Java 24

## ▶️ Running the App

Run the app with the configuration file as argument:

`java -jar kurirpaket-1.0.0.jar kurirpaket.conf`


If no argument is provided, it defaults to kurirpaket.conf in the current directory.

Logs (fortinet.log, kaspersky.log, etc.) are read from the locations defined in kurirpaket.conf.

## 📜 Example Log File

Example fortinet.log (each line is sent separately):

```
Feb 20 04:38:02 192.168.202.101 id=firewall sn=E0E69K5C4LL .......
Feb 20 04:38:02 192.168.202.101 id=firewall sn=C0EAE45CA55 .......
```

The app will:

Send the first line → wait interval (e.g., 1s) → send next line.

When reaching the end, restart from the beginning.

## 📝 Notes

You can freely edit kurirpaket.conf and source-logs/*.log without rebuilding the app.

To disable a log source, set enabled: false.

For new sources, simply append a new block to kurirpaket.conf.
