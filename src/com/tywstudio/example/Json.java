package com.tywstudio.example;

import java.util.*;

/** 极简 JSON 解析/序列化（仅满足 TML 协议需要）。零依赖。 */
final class Json {
    public static Object parse(String s) {
        Parser p = new Parser(s);
        p.skipWs();
        Object v = p.parseValue();
        p.skipWs();
        return v;
    }

    static final class Parser {
        final String s; int i;
        Parser(String s) { this.s = s; }
        void skipWs() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') i++;
                else break;
            }
        }
        Object parseValue() {
            skipWs();
            char c = s.charAt(i);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == '-' || (c >= '0' && c <= '9')) return parseNumber();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            throw new RuntimeException("JSON 解析错误 @ " + i);
        }
        Map<String, Object> parseObject() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            skipWs();
            if (s.charAt(i) == '}') { i++; return m; }
            while (true) {
                skipWs();
                String key = parseString();
                skipWs();
                i++;
                Object val = parseValue();
                m.put(key, val);
                skipWs();
                char c = s.charAt(i);
                if (c == ',') { i++; continue; }
                if (c == '}') { i++; break; }
                throw new RuntimeException("JSON 对象错误");
            }
            return m;
        }
        List<Object> parseArray() {
            List<Object> a = new ArrayList<>();
            i++;
            skipWs();
            if (s.charAt(i) == ']') { i++; return a; }
            while (true) {
                a.add(parseValue());
                skipWs();
                char c = s.charAt(i);
                if (c == ',') { i++; continue; }
                if (c == ']') { i++; break; }
                throw new RuntimeException("JSON 数组错误");
            }
            return a;
        }
        String parseString() {
            StringBuilder sb = new StringBuilder();
            i++;
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u': sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
                        default: sb.append(e);
                    }
                } else if (c == '"') break;
                else sb.append(c);
            }
            return sb.toString();
        }
        Object parseNumber() {
            int start = i;
            if (s.charAt(i) == '-') i++;
            while (i < s.length()) {
                char c = s.charAt(i);
                if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') i++;
                else break;
            }
            String num = s.substring(start, i);
            if (num.contains(".") || num.contains("e") || num.contains("E")) return Double.parseDouble(num);
            return Long.parseLong(num);
        }
    }

    public static String stringify(Object o) {
        StringBuilder sb = new StringBuilder();
        write(o, sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    static void write(Object o, StringBuilder sb) {
        if (o == null) sb.append("null");
        else if (o instanceof String) { sb.append('"'); escape((String) o, sb); sb.append('"'); }
        else if (o instanceof Boolean) sb.append(o.toString());
        else if (o instanceof Number) sb.append(o.toString());
        else if (o instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> e : ((Map<String, Object>) o).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"'); escape(e.getKey(), sb); sb.append("\":");
                write(e.getValue(), sb);
            }
            sb.append('}');
        } else if (o instanceof List) {
            sb.append('[');
            boolean first = true;
            for (Object v : (List<Object>) o) {
                if (!first) sb.append(',');
                first = false;
                write(v, sb);
            }
            sb.append(']');
        } else sb.append('"').append(o.toString()).append('"');
    }

    static void escape(String s, StringBuilder sb) {
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
    }
}
