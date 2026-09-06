import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * cache-kit 外部 HTTP 压测驱动（Java 21 单文件，虚拟线程）。
 *
 * 模式：
 *   read   <portsCsv> <token> <durationMs> <threads>  — 读吞吐 + 延迟分位数
 *   probe  <portsCsv> <token> <durationMs>            — 脏读观测：每实例 200ms 采样一次
 *            username，与写入的 epochMs 比较，输出各实例脏读演化
 */
public class LoadDriver {

    static final HttpClient CLIENT = HttpClient.newBuilder()
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static void main(String[] args) throws Exception {
        String mode = args[0];
        String[] ports = args[1].split(",");
        String token = args[2];
        long durationMs = Long.parseLong(args[3]);
        if (mode.equals("read")) {
            int threads = Integer.parseInt(args[4]);
            runRead(ports, token, durationMs, threads);
        } else {
            runProbe(ports, token, durationMs);
        }
    }

    static void runRead(String[] ports, String token, long durationMs, int threads) throws Exception {
        long deadline = System.nanoTime() + durationMs * 1_000_000L;
        AtomicBoolean stop = new AtomicBoolean(false);
        AtomicLong[] perInstanceOk = new AtomicLong[ports.length];
        for (int i = 0; i < ports.length; i++) perInstanceOk[i] = new AtomicLong();
        AtomicLong errors = new AtomicLong();
        List<List<Long>> buffers = new ArrayList<>();
        for (int i = 0; i < threads; i++) buffers.add(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final int id = i;
            final List<Long> buf = buffers.get(i);
            Thread.ofVirtual().start(() -> {
                int k = id;
                while (!stop.get()) {
                    int inst = k++ % ports.length;
                    HttpRequest req = HttpRequest.newBuilder(URI.create(
                                    "http://localhost:" + ports[inst] + "/paperwise/user/getuser"))
                            .header("Authorization", token)
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build();
                    long t0 = System.nanoTime();
                    try {
                        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
                        if (resp.statusCode() == 200) {
                            buf.add(System.nanoTime() - t0);
                            perInstanceOk[inst].incrementAndGet();
                        } else {
                            errors.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                    if (System.nanoTime() >= deadline) {
                        break;
                    }
                }
                done.countDown();
            });
        }
        done.await();
        stop.set(true);

        long[] all = buffers.stream().flatMapToLong(b -> b.stream().mapToLong(Long::longValue)).sorted().toArray();
        long total = all.length;
        System.out.println("===== 外部读压测结果（" + threads + " 并发, " + durationMs / 1000 + "s, "
                + ports.length + " 实例） =====");
        System.out.printf("总请求 %d | 吞吐 %.0f req/s | 非200 %d%n", total,
                total * 1000.0 / durationMs, errors.get());
        for (int i = 0; i < ports.length; i++) {
            System.out.printf("实例 :%s 占比 %.1f%%%n", ports[i], 100.0 * perInstanceOk[i].get() / Math.max(total, 1));
        }
        if (total > 0) {
            System.out.printf("延迟 ms: p50=%.1f p90=%.1f p99=%.1f p99.9=%.1f max=%.1f%n",
                    pct(all, 50), pct(all, 90), pct(all, 99), pct(all, 99.9),
                    all[all.length - 1] / 1e6);
        }
    }

    static double pct(long[] sorted, double p) {
        int idx = (int) Math.min(sorted.length - 1, sorted.length * p / 100.0);
        return sorted[idx] / 1e6;
    }

    static final Pattern USERNAME_MS = Pattern.compile("\"username\":\"cachekit-(\\d+)\"");

    /** 每实例 1 个探测线程，200ms 间隔采样，观察绕过应用的直接写库后脏读如何演化 */
    static void runProbe(String[] ports, String token, long durationMs) throws Exception {
        long deadline = System.nanoTime() + durationMs * 1_000_000L;
        CountDownLatch done = new CountDownLatch(ports.length);
        for (int i = 0; i < ports.length; i++) {
            final int inst = i;
            Thread.ofVirtual().start(() -> {
                long maxStale = 0;
                long lastStaleMs = 0;
                try {
                    while (System.nanoTime() < deadline) {
                        HttpRequest req = HttpRequest.newBuilder(URI.create(
                                        "http://localhost:" + ports[inst] + "/paperwise/user/getuser"))
                                .header("Authorization", token)
                                .POST(HttpRequest.BodyPublishers.noBody())
                                .build();
                        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
                        Matcher m = USERNAME_MS.matcher(resp.body());
                        if (m.find()) {
                            long stale = System.currentTimeMillis() - Long.parseLong(m.group(1));
                            if (stale > maxStale) {
                                maxStale = stale;
                            }
                            lastStaleMs = stale;
                        }
                        Thread.sleep(200);
                    }
                } catch (Exception ignored) {
                } finally {
                    System.out.printf("实例 :%s | 末次观测脏读 %d ms | 窗口内最大脏读 %d ms%n",
                            ports[inst], lastStaleMs, maxStale);
                    done.countDown();
                }
            });
        }
        done.await();
    }
}
