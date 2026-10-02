package Problems.DeveloperToolsAndInfrastructure;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskScheduler {
    static enum State {
        WAITING, IN_PROGRESS, CANCELLED, FAILED, INITIATED, SUCCEEDED
    }
    static class TaskConfig {
        private final boolean[] runDays = new boolean[7];   // 0 = Mon ... 6 = Sun

        public TaskConfig(boolean[] runDays) {
            if (runDays == null || runDays.length != 7) {
                throw new IllegalArgumentException("runDays must have exactly 7 entries");
            }
            System.arraycopy(runDays, 0, this.runDays, 0, 7);
        }

        public boolean runsOn(java.time.DayOfWeek day) {
            return runDays[day.getValue() - 1];
        }

        public boolean[] getRunDays() {
            return runDays.clone();
        }
    }
    static class Task {
        private final String taskId;
        private final TaskConfig taskConfig;
        private Instant lastExecutionTime;
        private State status;

        public Task(String taskId, TaskConfig taskConfig) {
            this.taskId = taskId;
            this.taskConfig = taskConfig;
            this.status = State.WAITING;
            this.lastExecutionTime = null;
        }

        public String getTaskId() { return taskId; }
        public TaskConfig getTaskConfig() { return taskConfig; }
        public State getStatus() { return status; }
        public Instant getLastExecutionTime() { return lastExecutionTime; }

        public void setLastExecutionTime(Instant t) { this.lastExecutionTime = t; }
        public void setStatus(State s) { this.status = s; }

        public Task run() {
            try {
                this.setStatus(State.IN_PROGRESS);
                Thread.sleep(100000);
                return this;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }


        public boolean ranToday(ZoneId zone) {
            Instant last = lastExecutionTime;
            if (last == null) return false;
            LocalDate lastDay = last.atZone(zone).toLocalDate();
            return LocalDate.now().equals(lastDay);
        }
    }
    public interface Observer {
        void update(Task task);
    }

    public interface Observable {
        void register(Observer observer);
        void unregister(Observer observer);
        void notifyObservers(Task task);
    }
    public class DisplayService implements Observer {
        @Override
        public void update(Task task) {
            System.out.println("Task " + task.getTaskId()
                    + " -> " + task.getStatus()
                    + " (last run: " + task.getLastExecutionTime() + ")");
        }
    }
    static class TaskSchedulerService implements Observable {
        Map<String, Task> map;
        ExecutorService executor;
        volatile boolean running;
        Set<Future<Task>> futureSet;
        Thread consumer;

        public TaskSchedulerService() {
            this.map = new ConcurrentHashMap<>();
            this.executor = Executors.newFixedThreadPool(10, runnable -> {
                Thread th = new Thread(runnable, "Worker Thread");
                th.setDaemon(true);
                return th;
            });
            futureSet = new HashSet<>();
            this.consumer = new Thread(this::schedule, "Consumer Th.");
            running = true;
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        }

        private final Set<Observer> observers = ConcurrentHashMap.newKeySet();

        @Override
        public void register(Observer observer) { observers.add(observer); }

        @Override
        public void unregister(Observer observer) { observers.remove(observer); }

        @Override
        public void notifyObservers(Task task) {
            for (Observer o : observers) o.update(task);
        }

        public boolean offer(String taskId, Task ts) {
            if (!running) return false;
            return map.putIfAbsent(taskId, ts) != null;
        }

        public void schedule() {
            while(running) {
                DayOfWeek today = LocalDate.now(ZoneId.of("UTC")).getDayOfWeek();

                map.values().stream()
                        .filter(task -> task.getStatus() != State.CANCELLED &&
                                task.getLastExecutionTime() != null &&
                                task.getTaskConfig().runsOn(today) &&
                                !task.ranToday(ZoneId.of("UTC")))
                        .forEach(task -> {
                            CompletableFuture<Task> future = CompletableFuture.completedFuture(task);
                            futureSet.add(future);
                            future.thenApplyAsync(this::execute, executor)
                                    .thenAcceptAsync(this::notifyObservers);
                            futureSet.add(future);
                            future.whenComplete((t, ex) -> futureSet.remove(future));
                });
            }
        }

        private Task execute(Task task) {
            try {
                if (!this.running) {
                    throw new RuntimeException("System interrupted");
                }
                task.setStatus(State.INITIATED);
                task.run();
            }
            catch (Exception e) {
                task.setStatus(State.FAILED);
            }
            return task;
        }

        public synchronized void shutdown() {
            if (!running) return;

            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)){
                    executor.shutdownNow();
                    executor.awaitTermination(5, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }

        }

        public boolean cancelTask(String taskId) {
            DayOfWeek today = LocalDate.now(ZoneId.of("UTC")).getDayOfWeek();

            return map.computeIfPresent(taskId, (s, task) -> {
                if (task.getTaskConfig().runsOn(today) && task.getStatus() != State.SUCCEEDED) task.setStatus(State.CANCELLED);
                return task;
            }).getStatus() == State.CANCELLED;
        }
    }

    static class TaskSchedulerFacade {
        private static final TaskSchedulerFacade INSTANCE = new TaskSchedulerFacade();

        private final TaskSchedulerService schedulerService = new TaskSchedulerService();

        private TaskSchedulerFacade() {}

        public static TaskSchedulerFacade getInstance() {
            return INSTANCE;
        }

        public String submitTask(TaskConfig config) {
            Task task = new Task(UUID.randomUUID().toString(), config);
            return task.getTaskId();
        }

        public boolean cancelTask(String taskId) {
            return schedulerService.cancelTask(taskId);
        }

        public void addObserver(Observer o) { schedulerService.register(o); }
        public void submit(String taskId, TaskConfig config) {
            schedulerService.offer(taskId, new Task(taskId, config));
        }
    }

}
