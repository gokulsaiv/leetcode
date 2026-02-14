# Async Logger System — Low Level Design (LLD)

## Problem Statement

Design a flexible and extensible logging system that supports:
- Multiple log levels (INFO, ERROR, WARN, PANIC)
- Structured metadata
- Pluggable output handlers (stdout, file, network, etc.)
- Pluggable parsers/formatters
- Asynchronous logging
- Ordering guarantees
- Back pressure handling
- Thread-safe producer model

This design is suitable for LLD interviews and extensible toward production evolution.

---

# High Level Architecture

Application Threads (Producers)
        |
        v
    BlockingQueue<LogEvent>
        |
        v
  Logger Worker Thread (Consumer)
        |
        v
     Parser → Handler → Output Sink

Core idea: Separate log production from slow I/O using an async queue and a worker thread.

---

# Design Principles

- Separation of concerns (creation vs formatting vs delivery)
- Composition over inheritance
- Strategy pattern for Parser and Handler
- Producer–Consumer concurrency model
- Bounded queue for back pressure
- Single consumer thread for global ordering
- Easy handler/parser extensibility

Patterns Used:
- Strategy Pattern
- Producer–Consumer Pattern

---

# Data Model

## MetaData

Carries contextual information about the service instance.

```java
class MetaData {
    private final int id;
    private final String replicaName;
    private final String serviceName;
    private final String region;

    public MetaData(int id, String replicaName, String serviceName, String region) {
        this.id = id;
        this.replicaName = replicaName;
        this.serviceName = serviceName;
        this.region = region;
    }

    public int getId() { return id; }
    public String getReplicaName() { return replicaName; }
    public String getServiceName() { return serviceName; }
    public String getRegion() { return region; }
}
```

---

## Log Level Enum

```java
enum Type {
    ERROR,
    INFO,
    WARN,
    PANIC
}
```

---

## LogEvent Wrapper

Queue payload object.

```java
import java.time.LocalDateTime;

class LogEvent {
    final MetaData meta;
    final String message;
    final LocalDateTime time;
    final Type level;

    LogEvent(MetaData m, String msg, Type level) {
        this.meta = m;
        this.message = msg;
        this.level = level;
        this.time = LocalDateTime.now();
    }
}
```

---

# Parser (Formatting Strategy)

Responsible for converting LogEvent → formatted string.

```java
interface Parser {
    String parse(LogEvent e);
}
```

## StandardParser Implementation

```java
class StandardParser implements Parser {
    public String parse(LogEvent e) {
        return e.time + " | " +
               e.level + " | " +
               e.meta.getServiceName() + " | " +
               e.meta.getReplicaName() + " | " +
               e.message;
    }
}
```

Possible extensions:
- JsonParser
- CompactParser
- KeyValueParser

---

# Handler (Output Strategy)

Responsible for writing formatted logs to destination.

```java
interface Handlers {
    void handleEvent(String formattedLog);
}
```

## StdOutHandler

```java
class StdOutHandler implements Handlers {
    public void handleEvent(String formattedLog) {
        System.out.println(formattedLog);
    }
}
```

## FileHandler (Example Extension)

```java
import java.io.*;

class FileHandler implements Handlers {
    private final BufferedWriter writer;

    public FileHandler(String path) throws IOException {
        this.writer = new BufferedWriter(new FileWriter(path, true));
    }

    public synchronized void handleEvent(String formattedLog) {
        try {
            writer.write(formattedLog);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("File write failed");
        }
    }
}
```

---

# Async Logger with BlockingQueue

## Design Goals

- Producers return quickly
- I/O handled by background thread
- Preserve global ordering
- Provide back pressure

## Implementation

```java
import java.util.concurrent.*;

class Logger {
    private final BlockingQueue<LogEvent> queue;
    private final Handlers handler;
    private final Parser parser;
    private final Thread worker;

    public Logger(Handlers handler, Parser parser) {
        this.queue = new ArrayBlockingQueue<>(1000);
        this.handler = handler;
        this.parser = parser;
        this.worker = startWorker();
    }

    private Thread startWorker() {
        Thread t = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    LogEvent e = queue.take();
                    String formatted = parser.parse(e);
                    handler.handleEvent(formatted);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    // handler/parser failure isolation point
                    System.err.println("Logger worker error: " + ex.getMessage());
                }
            }
        });

        t.setDaemon(true);
        t.start();
        return t;
    }

    public void shutdown() {
        worker.interrupt();
    }

    public void info(MetaData m, String msg) {
        enqueue(m, msg, Type.INFO);
    }

    public void error(MetaData m, String msg) {
        enqueue(m, msg, Type.ERROR);
    }

    public void warn(MetaData m, String msg) {
        enqueue(m, msg, Type.WARN);
    }

    public void panic(MetaData m, String msg) {
        enqueue(m, msg, Type.PANIC);
    }

    private void enqueue(MetaData m, String msg, Type level) {
        LogEvent e = new LogEvent(m, msg, level);
        try {
            queue.put(e);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
```

---

# Concurrency Model Explanation

- Application threads act as producers
- Worker thread acts as consumer
- BlockingQueue is thread-safe buffer
- `queue.put()` blocks when full
- `queue.take()` blocks when empty
- No busy waiting
- No manual locking needed in logger core

---

# Ordering Guarantees

## Single Worker Thread

- Preserves global ordering
- Deterministic output
- Simplest correctness model

## Alternative: Per-Level Queues

- One queue per level
- Ordering preserved per level
- Higher throughput
- No cross-level ordering guarantee

---

# Back Pressure Strategy

Using bounded queue:

```java
new ArrayBlockingQueue<>(1000);
```

When queue is full:
- Producer threads block
- Prevents memory blowup

Alternative strategies:
- Drop INFO logs first
- Use `offer()` with timeout instead of `put()`
- Ring buffer overwrite
- Spill to disk buffer

---

# Failure Handling Options

- Handler throws exception → worker catches and isolates failure
- Add retry wrapper handler
- Add fallback handler
- Add dead-letter log sink
- Add metrics counter for failures

---

# Extensibility Points

Add new handlers:
- FileHandler
- NetworkHandler
- KafkaHandler
- DatabaseHandler

Add new parsers:
- JSON
- Structured KV
- Compact format

Add new features:
- Level filtering
- Sampling
- Multi-handler fanout
- Distributed sequence IDs
- Correlation IDs / trace IDs

---

# Interview Talking Points

- Logging is I/O bound → async improves latency
- BlockingQueue gives safe producer-consumer model
- Single worker preserves ordering
- Bounded queue provides back pressure
- Parser and Handler are strategy interfaces
- Open for extension, closed for modification
- Easy to add sinks without touching logger core

---

# Minimal Usage Example

```java
MetaData meta = new MetaData(
    1,
    "replica-A",
    "payment-service",
    "us-east"
);

Logger logger = new Logger(
    new StdOutHandler(),
    new StandardParser()
);

logger.info(meta, "payment started");
logger.error(meta, "payment failed");

// optional graceful shutdown
logger.shutdown();
```
