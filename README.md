# ARP Request–Reply Simulator

A Java-based graphical simulator that demonstrates the Address Resolution Protocol (ARP) process in a local network. It simulates ARP requests, replies, ARP cache lookups, and cache expiration through an interactive GUI.

## Features

- Simulates ARP request broadcasts and unicast replies.
- Maintains ARP caches for individual network devices.
- Supports adding, editing, and removing network devices.
- Supports 2 to 5 devices.
- Validates IPv4 and MAC addresses and prevents duplicates.
- Tracks cache hits, cache misses, requests, replies, and failed resolutions.
- Supports configurable cache time-to-live (TTL).
- Displays a timestamped event log.
- Provides controls to clear caches and reset statistics.

## Technologies Used

- Java
- Java Swing
- Object-Oriented Programming
- Data Structures: HashMap, ArrayList, and List

## Requirements

- JDK 17 or later
- Visual Studio Code or another Java-compatible IDE

## How to Run

Open a terminal in the project root directory.

Compile the Java source files:

```bash
javac -d out src/main/java/*.java
```

Run the simulator:

```bash
java -cp out ARPSimulator
```

## Project Structure

```text
ARP-Request-Reply-Simulator/
├── src/
│   └── main/
│       └── java/
│           ├── NetworkDevice.java
│           ├── ARPEntry.java
│           ├── ARPCache.java
│           ├── NetworkStatistics.java
│           ├── ARPPacket.java
│           ├── ARPManager.java
│           └── ARPSimulator.java
├── .gitignore
└── README.md
```

## Scope and Limitation

This project is a logical simulation of ARP behavior. It does not transmit actual ARP packets over a physical or virtual network interface.

## Authors

Sanjay Thandeeswaran, Monish Kumar T O and Jayanth Priyatharsan P T
