package com.kurirpaket;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class main {

    static class LogConfig {
        String logId;
        boolean enabled = true;
        String logFile;
        String syslogHost;
        int syslogPort;
        String protocol;
        long intervalMs;
    }

    public static void main(String[] args) throws Exception {
        String configPath = "kurirpaket.conf"; // default
        if (args.length > 0) {
            configPath = args[0]; // user-specified config
        }

        List<LogConfig> configs = loadConfig(configPath);

        ExecutorService executor = Executors.newFixedThreadPool(configs.size());
        for (LogConfig cfg : configs) {
            if (!cfg.enabled) {
                System.out.println("Skipping " + cfg.logId + " (disabled in config)");
                continue;
            }
            executor.submit(() -> runLogSender(cfg));
        }
    }

    private static List<LogConfig> loadConfig(String filePath) throws IOException {
        List<LogConfig> configs = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(filePath));

        LogConfig cfg = null;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("- logid:")) {
                if (cfg != null) configs.add(cfg);
                cfg = new LogConfig();
                cfg.logId = line.split(":", 2)[1].trim();
            } else if (cfg != null) {
                String[] parts = line.split(":", 2);
                if (parts.length < 2) continue;
                String key = parts[0].trim();
                String value = parts[1].trim();
                switch (key) {
                    case "enabled": cfg.enabled = Boolean.parseBoolean(value); break;
                    case "log_dir": cfg.logFile = value; break;
                    case "syslog_host": cfg.syslogHost = value; break;
                    case "syslog_port": cfg.syslogPort = Integer.parseInt(value); break;
                    case "syslog_protocol": cfg.protocol = value.toLowerCase(); break;
                    case "interval": cfg.intervalMs = parseInterval(value); break;
                }
            }
        }
        if (cfg != null) configs.add(cfg);
        return configs;
    }

    private static long parseInterval(String interval) {
        if (interval.endsWith("ms")) return Long.parseLong(interval.replace("ms", ""));
        if (interval.endsWith("s")) return Long.parseLong(interval.replace("s", "")) * 1000;
        if (interval.endsWith("m")) return Long.parseLong(interval.replace("m", "")) * 60 * 1000;
        return Long.parseLong(interval); // default ms
    }

    private static void runLogSender(LogConfig cfg) {
        System.out.println("Starting sender for " + cfg.logId +
                " -> " + cfg.syslogHost + ":" + cfg.syslogPort + "/" + cfg.protocol);

        while (true) {
            try (BufferedReader reader = new BufferedReader(new FileReader(cfg.logFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sendLog(line, cfg);
                    Thread.sleep(cfg.intervalMs);
                }
            } catch (Exception e) {
                System.err.println("Error reading " + cfg.logFile + ": " + e.getMessage());
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        }
    }

    private static void sendLog(String log, LogConfig cfg) {
        try {
            if ("udp".equalsIgnoreCase(cfg.protocol)) {
                DatagramSocket socket = new DatagramSocket();
                byte[] data = log.getBytes();
                InetAddress address = InetAddress.getByName(cfg.syslogHost);
                DatagramPacket packet = new DatagramPacket(data, data.length, address, cfg.syslogPort);
                socket.send(packet);
                socket.close();
            } else if ("tcp".equalsIgnoreCase(cfg.protocol)) {
                try (Socket socket = new Socket(cfg.syslogHost, cfg.syslogPort)) {
                    OutputStream out = socket.getOutputStream();
                    out.write(log.getBytes());
                    out.write("\n".getBytes());
                    out.flush();
                }
            }
//            System.out.println("[" + cfg.logId + "] Sent: " + log);
        } catch (Exception e) {
            System.err.println("[" + cfg.logId + "] Failed to send log: " + e.getMessage());
        }
    }
}
