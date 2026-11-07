# TML 扩展模板 · Java

TYW Launcher（TML）扩展模板，演示如何用 **Java** 编写一个扩展（打包为 `.jar`）。
扩展运行在启动器进程之外，通过 stdin/stdout 的 NDJSON（每行一个 JSON）交换白名单方法。

> 本仓库为 GitHub **模板仓库（template repository）**，点击 “Use this template” 即可派生出你自己的扩展工程。

## 目录结构

```
META-INF/tml-extension.json   扩展清单（id / 名称 / 入口 / 权限 / 订阅事件 …）
src/com/tywstudio/example/
    Main.java                 扩展入口（entry 指向这里）
    Json.java                 极简 JSON 解析/序列化（零依赖）
build.sh / build.bat          编译并打包成 .jar
LICENSE                       CC BY-NC 4.0
```

## 协议要点

- 宿主启动扩展时执行 `java -cp "<jar>" com.tywstudio.example.Main`，
  先发 `initialize` 请求；扩展须回 `{"ready": true}` 完成握手。
- 扩展调用宿主能力（如 `log`、`host.info`）即向 stdout 写一行 JSON-RPC 请求，宿主回结果。
- 宿主通过 `event` 行向扩展广播事件（如 `launcher.started`、`game.launched`）。
- 宿主发送 `shutdown` 时扩展应干净退出。

可用能力（需写在 manifest 的 `permissions` 里）：

| 方法 | 权限 | 说明 |
| --- | --- | --- |
| `log` | `log` | 写启动器日志 |
| `toast` | `toast` | 弹左下角提示 |
| `host.info` | `host.info` | 取宿主平台/版本/语言等 |
| `instances.list` | `instances.list` | 取的实例列表（脱敏） |
| `event.subscribe` | `events` | 订阅事件（还须在 `subscribe` 声明） |
| `storage.get/set` | `storage` | 扩展私有键值存储 |
| `settings.get/set` | `settings` | 读写启动器设置（命名空间 `ext.<id>.*`） |

## 编译与打包

需要 JDK（含 `javac` 与 `jar`）：

```bash
# Linux / macOS
bash build.sh
# Windows
build.bat
```

产物：`com.tywstudio.example.java.jar`。

## 导入与启用

把生成的 `.jar` 在启动器「设置 → 其他 → 拓展 → 导入」里导入，
确认权限后启用即可。首次启用会要求确认申请的权限。

## 注意事项

- `META-INF/tml-extension.json` 的 `platforms` 当前须包含 `windows`（Windows 宿主已实现，其余平台后续补充）。
- 扩展默认全部禁用，且只能访问白名单能力；任何文件/网络/凭据相关的能力都不存在。
- 不要硬编码敏感信息；持久化数据请走 `storage` / `settings`。

## 许可证

[CC BY-NC 4.0](./LICENSE) —— 署名-非商业性使用。可自由学习、修改、二次分发，但不得用于商业目的。
