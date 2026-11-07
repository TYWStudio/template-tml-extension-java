package com.tywstudio.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** TML 拓展示例（Java）。入口由 manifest 的 entry 指定。 */
public class Main {

    /** 宿主调用失败时抛出。 */
    public static class TmlError extends RuntimeException {
        public final int code;
        public TmlError(int code, String msg) { super("[" + code + "] " + msg); this.code = code; }
    }

    /** 极简 TML 客户端：握手、调用白名单方法、接收事件、优雅退出。零依赖。 */
    public static class TmlClient {
        private int nextId = 2;
        private final Map<Integer, CompletableFuture<Map<String, Object>>> pending = new ConcurrentHashMap<>();
        private final List<EventCallback> handlers = new CopyOnWriteArrayList<>();
        public List<String> permissions = new ArrayList<>();
        public String privateDir = "";
        public String extId = "";
        public volatile boolean ready = false;
        public volatile boolean stop = false;
        private final BufferedReader in;
        private final PrintStream out;

        public TmlClient() {
            this.in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            this.out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        }

        private void send(Map<String, Object> obj) {
            out.println(Json.stringify(obj));
        }

        @SuppressWarnings("unchecked")
        public CompletableFuture<Map<String, Object>> call(String method, Map<String, Object> params) {
            int id = nextId++;
            CompletableFuture<Map<String, Object>> f = new CompletableFuture<>();
            pending.put(id, f);
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("id", id);
            req.put("method", method);
            req.put("params", params == null ? new LinkedHashMap<>() : params);
            send(req);
            return f;
        }

        @SuppressWarnings("unchecked")
        public void loop() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    Object parsed;
                    try { parsed = Json.parse(line); } catch (Exception e) { continue; }
                    if (!(parsed instanceof Map)) continue;
                    Map<String, Object> msg = (Map<String, Object>) parsed;
                    if (msg.containsKey("method")) {
                        String method = (String) msg.get("method");
                        if ("initialize".equals(method)) {
                            Map<String, Object> p = (Map<String, Object>) msg.get("params");
                            if (p != null) {
                                extId = str(p.get("extId"));
                                privateDir = str(p.get("privateDir"));
                                if (p.get("permissions") instanceof List) {
                                    for (Object x : (List<?>) p.get("permissions")) permissions.add(String.valueOf(x));
                                }
                            }
                            Map<String, Object> r = new LinkedHashMap<>();
                            r.put("id", msg.get("id"));
                            Map<String, Object> res = new LinkedHashMap<>();
                            res.put("ready", true);
                            r.put("result", res);
                            send(r);
                            ready = true;
                            continue;
                        }
                        if ("shutdown".equals(method)) { stop = true; break; }
                        if ("event".equals(method)) {
                            Map<String, Object> p = (Map<String, Object>) msg.get("params");
                            if (p != null) {
                                String type = str(p.get("type"));
                                Object data = p.get("data");
                                for (EventCallback cb : handlers) cb.on(type, data);
                            }
                            continue;
                        }
                        continue;
                    }
                    if (msg.containsKey("id")) {
                        Object idv = msg.get("id");
                        int id = (idv instanceof Number) ? ((Number) idv).intValue() : Integer.parseInt(String.valueOf(idv));
                        CompletableFuture<Map<String, Object>> f = pending.remove(id);
                        if (f != null) {
                            if (msg.containsKey("error")) {
                                Map<String, Object> err = (Map<String, Object>) msg.get("error");
                                f.completeExceptionally(new TmlError(num(err.get("code")), str(err.get("message"))));
                            } else {
                                Object res = msg.get("result");
                                f.complete(res instanceof Map ? (Map<String, Object>) res : new LinkedHashMap<>());
                            }
                        }
                    }
                }
            } catch (IOException e) { /* 退出 */ }
        }

        // ---------- 便捷方法 ----------
        public void log(String message) { log(message, "info"); }
        public void log(String message, String level) { call("log", m("level", level, "message", message)); }
        public void toast(String message) { call("toast", m("message", message)); }
        public Map<String, Object> hostInfo() throws Exception { return call("host.info", null).get(); }
        @SuppressWarnings("unchecked")
        public List<Object> listInstances() throws Exception {
            Map<String, Object> r = call("instances.list", null).get();
            return (List<Object>) r.getOrDefault("instances", new ArrayList<>());
        }
        @SuppressWarnings("unchecked")
        public List<Object> subscribe(List<String> types) throws Exception {
            Map<String, Object> r = call("event.subscribe", m("types", types)).get();
            return (List<Object>) r.getOrDefault("accepted", new ArrayList<>());
        }
        public Object storageGet(String key) throws Exception {
            return call("storage.get", m("key", key)).get().get("value");
        }
        public void storageSet(String key, Object value) throws Exception {
            call("storage.set", m("key", key, "value", value)).get();
        }
        public Object settingsGet(String key) throws Exception {
            return call("settings.get", m("key", key)).get().get("value");
        }
        public void settingsSet(String key, Object value) throws Exception {
            call("settings.set", m("key", key, "value", value)).get();
        }
        public void onEvent(EventCallback cb) { handlers.add(cb); }

        // ---------- 小工具 ----------
        private static Map<String, Object> m(Object... kv) {
            Map<String, Object> r = new LinkedHashMap<>();
            for (int i = 0; i + 1 < kv.length; i += 2) r.put((String) kv[i], kv[i + 1]);
            return r;
        }
        private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
        private static int num(Object o) {
            if (o instanceof Number) return ((Number) o).intValue();
            try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return -32000; }
        }
    }

    /** 事件回调。 */
    public interface EventCallback {
        void on(String type, Object data);
    }

    public static void main(String[] args) throws Exception {
        TmlClient tml = new TmlClient();
        new Thread(tml::loop, "tml-loop").start();
        while (!tml.ready && !tml.stop) Thread.sleep(50);
        if (!tml.ready) return;

        tml.log("拓展示例已启动 (Java)", "info");
        try {
            Map<String, Object> info = tml.hostInfo();
            tml.log("宿主信息: " + Json.stringify(info));
        } catch (Exception e) {
            tml.log("获取宿主信息失败: " + e.getMessage(), "warn");
        }

        List<Object> accepted = tml.subscribe(Arrays.asList("launcher.started", "game.launched", "settings.changed"));
        Map<String, Object> sub = new LinkedHashMap<>();
        sub.put("accepted", accepted);
        tml.log("已订阅事件: " + Json.stringify(sub));
        tml.toast("你好，我是 TML 拓展示例！");

        tml.onEvent((type, data) -> {
            Map<String, Object> wrap = new LinkedHashMap<>();
            wrap.put("data", data);
            tml.log("收到事件 " + type + ": " + Json.stringify(wrap));
        });

        Object count = tml.storageGet("launch_count");
        int n = 0;
        try { n = Integer.parseInt(String.valueOf(count)); } catch (Exception ignore) { }
        n += 1;
        tml.storageSet("launch_count", n);
        tml.log("历史触发次数: " + n);

        tml.settingsSet("ext." + tml.extId + ".greeting", "hello");
        Object greeting = tml.settingsGet("ext." + tml.extId + ".greeting");
        tml.log("读取设置: " + greeting);

        while (!tml.stop) Thread.sleep(200);
    }
}
