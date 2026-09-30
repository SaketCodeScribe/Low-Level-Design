package Problems.DeveloperToolsAndInfrastructure;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/*
FRs:
1. logging level
2. log can be sent to multiple desitanations -  console & file output
3. custom log formatter
4. async logging & ensure no interleaving of logs in multi-threaded env

NFRs:
1. design should follow OOD + modular and easier to test components
2. design should be extensible to handle future features
3. logging should add minimal overhead on the application performance
 */
public class DesignLoggingFramework {
    enum Level {
        INFO, DEBUG, WARN, ERROR, FATAL
    }

    interface Formatter {
        String format(Record record);
    }

    interface Appender {
        void append(List<Record> batch);
    }

    record Record(Instant timestamp, Class<?> logger, String mssg, Level level) {
    }

    static class SimpleTextFormatter implements Formatter {
        @Override
        public String format(Record r) {
            return String.format("%s [%s] %s - %s",
                    r.timestamp(), r.level(), r.logger().getSimpleName(), r.mssg());
        }
    }

    static class JSONFormatter implements Formatter {
        private static String escape(String s) {
            if (s == null) return "";
            StringBuilder sb = new StringBuilder(s.length() + 8);
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> {
                        if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                        else sb.append(c);
                    }
                }
            }
            return sb.toString();
        }

        @Override
        public String format(Record r) {
            return "{"
                    + "\"timestamp\":\"" + r.timestamp() + "\","
                    + "\"level\":\"" + r.level() + "\","
                    + "\"logger\":\"" + escape(r.logger().getName()) + "\","
                    + "\"message\":\"" + escape(r.mssg()) + "\""
                    + "}";
        }
    }

    static class ConsoleAppender implements Appender {
        private final Formatter formatter;

        public ConsoleAppender(Formatter formatter) {
            this.formatter = formatter;
        }

        @Override
        public void append(List<Record> batch) {
            for (Record r : batch) System.out.println(formatter.format(r));
        }
    }

    static class FileAppender implements Appender, Closeable {

        private final Formatter formatter;
        private BufferedWriter writer;

        public FileAppender(Formatter formatter) {
            this.formatter = formatter;
            try {
                Path dir = Paths.get(System.getProperty("user.dir"), "output");
                Files.createDirectories(dir);
                String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                this.writer = Files.newBufferedWriter(dir.resolve("log-" + ts + ".txt"),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                System.err.println("file appender disabled: " + e);
            }
        }

        @Override
        public void append(List<Record> batch) {
            if (writer == null) return;
            try {
                for (Record r : batch) {
                    writer.write(formatter.format(r));
                    writer.newLine();
                }
                writer.flush();
            } catch (IOException e) {
                System.err.println("file write failed: " + e);
            }
        }

        @Override
        public void close() {
            if (writer == null) return;
            try {
                writer.close();
            } catch (IOException ignored) {
            }
        }
    }
    static class LogDispatcher {
        private static final int MAX_BATCH = 500;

        private final List<Appender> appenders;
        private final BlockingQueue<Record> queue = new LinkedBlockingQueue<>(10_000);
        private final ExecutorService executor;
        private final AtomicLong failedRecords = new AtomicLong();
        private volatile boolean running = true;

        public LogDispatcher(List<Appender> appenders) {
            this.appenders = appenders;
            this.executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "dispatch-thread");
                t.setDaemon(true);
                return t;
            });
            this.executor.submit(this::run);
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        }

        public void submit(Record record) {
            if (!queue.offer(record)) failedRecords.incrementAndGet();   // queue full
        }

        public long failedRecords() { return failedRecords.get(); }

        private void run() {
            List<Record> batch = new ArrayList<>(MAX_BATCH);
            try {
                while (running || !queue.isEmpty()) {
                    Record first;
                    try {
                        first = queue.poll(100, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        break;
                    }
                    if (first == null) continue;
                    batch.add(first);
                    queue.drainTo(batch, MAX_BATCH - 1);
                    writeBatch(batch);
                    batch.clear();
                }
            } finally {
                Thread.interrupted();
                queue.drainTo(batch);
                if (!batch.isEmpty()) writeBatch(batch);
                closeAppenders();
                long f = failedRecords.get();
                if (f > 0) System.err.println("logger shutdown: " + f + " records failed/dropped");
            }
        }

        private void writeBatch(List<Record> batch) {
            for (Appender ap : appenders) {
                try {
                    ap.append(batch);
                } catch (Throwable t) {
                    failedRecords.addAndGet(batch.size());
                    System.err.println("appender failed: " + ap.getClass().getSimpleName() + " -> " + t);
                }
            }
        }

        private void closeAppenders() {
            for (Appender ap : appenders) {
                if (ap instanceof Closeable c) {
                    try { c.close(); } catch (IOException ignored) {}
                }
            }
        }

        public synchronized void shutdown() {
            if (!running) return;
            running = false;
            executor.shutdown();

            boolean interrupted = false;
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    executor.awaitTermination(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                interrupted = true;
                executor.shutdownNow();
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
    }
    static class LoggerFactory {
        private static final LogDispatcher DISPATCHER = new LogDispatcher(List.of(
                new ConsoleAppender(new SimpleTextFormatter()),
                new FileAppender(new JSONFormatter())));

        private static final Map<Class<?>, Logger> CACHE = new ConcurrentHashMap<>();

        private LoggerFactory() {
        }

        public static Logger getLogger(Class<?> c) {
            return getLogger(c, Level.INFO);
        }

        public static Logger getLogger(Class<?> c, Level minLevel) {
            return CACHE.computeIfAbsent(c, k -> new Logger(k, minLevel, DISPATCHER));
        }
    }

    static class Logger {
        private final Class<?> clazz;
        private final Level minLevel;
        private final LogDispatcher dispatcher;

        Logger(Class<?> clazz, Level minLevel, LogDispatcher dispatcher) {
            this.clazz = clazz;
            this.minLevel = minLevel;
            this.dispatcher = dispatcher;
        }

        public void debug(String msg) {
            log(Level.DEBUG, msg);
        }

        public void info(String msg) {
            log(Level.INFO, msg);
        }

        public void warn(String msg) {
            log(Level.WARN, msg);
        }

        public void error(String msg) {
            log(Level.ERROR, msg);
        }

        public void fatal(String msg) {
            log(Level.FATAL, msg);
        }

        private void log(Level level, String msg) {
            if (level.ordinal() < minLevel.ordinal()) return;
            dispatcher.submit(new Record(Instant.now(), clazz, msg, level));
        }
    }
}
