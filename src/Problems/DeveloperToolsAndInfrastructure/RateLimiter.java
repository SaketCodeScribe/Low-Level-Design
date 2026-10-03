package Problems.DeveloperToolsAndInfrastructure;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * FRs:
 * Rate limit client request irrespective of devices
 * Api responds 429 too many response when client request is exceeded
 * Same rate limit for all users
 * <p>
 * NFRs:
 * System should follow OOD and have modular components
 * System is extensible and include future features
 * Components should be easier to test
 * System should handle concurrent users
 */
public class RateLimiter {
    interface Response {
        int status();

        String message();

    }

    record User(String id, String name) {
    }

    record ErrorResponse(int status, String message, Instant retryAfter) implements Response {

        public static ErrorResponse rateLimitExceeded(User user, Instant retryAfter) {
            return new ErrorResponse(
                    429,
                    "Rate limit exceeded for user " + user.id(),
                    retryAfter);
        }

        public long retryAfterSeconds() {
            long s = Duration.between(Instant.now(), retryAfter).toSeconds();
            return Math.max(s, 0);
        }

    }

    record SuccessResponse(int status, String message) implements Response {
        public static SuccessResponse allow(User user) {
            return new SuccessResponse(
                    200,
                    "User allowed" + user.id());
        }
    }

    record InternalServerError(int status, String message) implements Response {
        public static InternalServerError error(User user, String message) {
            return new InternalServerError(
                    500,
                    String.format("%s for: %s" + message, user.id())
            );
        }
    }

    static class Token {
        final int MAX_LIMIT;
        private final int MULTIPLIER = 1000;
        int bucket;
        int rps;
        Long lastTokenUsed;

        public Token(int bucket, int rps) {
            this.MAX_LIMIT = bucket * MULTIPLIER;
            this.bucket = bucket * MULTIPLIER;
            this.rps = rps;
        }

        public Response allow(User user) {
            long current = Instant.now().toEpochMilli();
            if (lastTokenUsed == null) {
                lastTokenUsed = current;
            }
            long newToken = (current - lastTokenUsed) * rps;
            this.bucket = (int) Math.min(this.bucket + newToken, this.MAX_LIMIT);
            if (this.bucket / MULTIPLIER > 0) {
                this.bucket--;
                return SuccessResponse.allow(user);
            }
            return ErrorResponse.rateLimitExceeded(user, Instant.ofEpochMilli(lastTokenUsed));
        }
    }

    static class RateLimiterService {
        static volatile boolean running = true;
        final Set<Future<?>> futures = ConcurrentHashMap.newKeySet();
        private final ExecutorService executorService;
        private final int TOKEN_LIMIT;
        private final int rps;
        Map<String, Token> tokens;

        public RateLimiterService(int tokens, int rps) {
            this.TOKEN_LIMIT = tokens;
            this.rps = rps;
            this.tokens = new ConcurrentHashMap<>();
            this.executorService = Executors.newFixedThreadPool(10, runnable -> {
                Thread th = new Thread("Worker thread");
                th.setDaemon(true);
                return th;
            });
        }

        public CompletableFuture<Response> checkLimit(User user) {
            Response[] response = new Response[1];
            CompletableFuture<Response> future = CompletableFuture.completedFuture(null)
                    .thenApplyAsync(u -> {
                        if (!running) throw new RuntimeException("application is interrupted");
                        tokens.compute(user.id(), (k, v) -> {
                            if (v == null) {
                                v = new Token(this.TOKEN_LIMIT, this.rps);
                            }
                            response[0] = v.allow(user);
                            return v;
                        });
                        return response[0];
                    }, this.executorService)
                    .whenComplete((r, ex) -> {
                        if (ex != null) r = InternalServerError.error(user, ex.getMessage());
                    });
            futures.add(future);
            return future.whenComplete((r, ex) -> {
                        if (ex != null) r = InternalServerError.error(user, ex.getMessage());
                        futures.remove(future);
                    }
            );
        }

        public synchronized void shutdown() {
            if (!running) return;
            running = false;
            executorService.shutdown();
            try {
                if (executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    executorService.awaitTermination(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            for (Future<?> future : futures) future.cancel(true);
        }
    }

    static class RateLimiterFacade {
        private static final int DEFAULT_TOKENS = 10;
        private static final int DEFAULT_RPS = 5;
        private final RateLimiterService service;

        RateLimiterFacade(RateLimiterService service) {
            this.service = service;
            Runtime.getRuntime().addShutdownHook(new Thread(service::shutdown));
        }

        public static RateLimiterFacade getInstance() {
            return Holder.INSTANCE;
        }

        public CompletableFuture<Response> handleRequest(User user) {
            if (user == null || user.id() == null) {
                return CompletableFuture.completedFuture(
                        new InternalServerError(400, "user is required"));
            }
            return service.checkLimit(user);
        }

        public void shutdown() {
            service.shutdown();
        }

        private static class Holder {
            static final RateLimiterFacade INSTANCE = new RateLimiterFacade(
                    new RateLimiterService(DEFAULT_TOKENS, DEFAULT_RPS));
        }
    }
}
