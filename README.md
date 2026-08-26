# EMC Fluid 1.12.2

EMC Fluid 将 ProjectE EMC 转换为五级可储存、可输送的流体，并允许 Refined Storage 或 Applied Energistics 2 使用玩家的 ProjectE 知识进行流体自动合成。

当前发布版本：`1.0.0-mc1.12.2`。

## 安装

1. 使用 Minecraft 1.12.2 和 Forge 14.23.5.2860。
2. 客户端与服务端都安装 `ProjectE-1.12.2-PE1.4.1.jar`。
3. 将 `emcfluid-1.0.0-mc1.12.2.jar` 放入 `mods` 目录。
4. 按需安装下表中的可选集成模组；多人游戏中，除 JEI 外，客户端和服务端应保持相同的内容模组组合。

不要把本项目 `reference/` 目录中的开发依赖整体复制到发布包。EMC Fluid 的发布 jar 不包含任何第三方模组。

## 固定依赖版本

| 模组 | 版本 | 必需性 | 用途 |
| --- | --- | --- | --- |
| ProjectE | 1.12.2-PE1.4.1 | 必需 | EMC 存取、物品估值和玩家知识 |
| Refined Storage | 1.6.16 | 可选 | RS 流体自动合成 |
| Applied Energistics 2 | rv6-stable-7 | 可选 | ME 网络基础 |
| AE2 Fluid Crafting | 1.0.11 | 可选 | AE2 流体图样和 Fluid Packet；使用 AE2 自动合成时必需 |
| JEI | 4.16.5.1029 | 可选、仅客户端 | Converter 配方展示 |

只保证表中固定版本。尤其不要将 AE2 Fluid Crafting 1.0.11 与其他 AE2 大版本混用。

开发与测试所用 jar 的 SHA-256 记录在 [开发计划](docs/1.12.2-development-plan.md) 中。

## 基本使用

- EMC Liquefier：在 Klein Star 等 EMC 容器与 T1 EMC Fluid 之间双向转换。
- EMC Convert Liquefier：在 EMC 容器与选定等级的 EMC Fluid 之间双向转换。
- EMC Converter：在相邻 EMC Fluid 等级之间进行严格守恒的升降级转换。
- Knowledge Pattern：右键绑定玩家；EMC Crafter 只发布该玩家当前已学习且拥有正 EMC 值的目标。
- EMC Crafter：将 Knowledge Pattern 转换为 RS/AE2 自动合成图样，产物在网络堵塞时保存在九格持久缓存中。

使用 AE2 集成时，ME 网络必须安装 AE2 Fluid Crafting 的 Fluid Discretizer。没有 Discretizer 时图样仍会显示，但合成计算会报告缺少对应 Fluid Drop。

## 配置与存档

配置文件为 `config/emcfluid.cfg`：

- `enabled_tiers`：启用的流体等级数，范围 1～5。
- `tier_values`：每 mB 对应的 EMC 值；必须为正数、严格递增，且 T1 必须为 1。
- `converter_ticks_per_batch`：Converter 每批处理 tick，范围 1～200。

世界会保存 tier 配置快照。修改已有世界的 tier 配置前请先备份；禁用等级中已经存在的流体不会被静默删除，但机器会停止处理该等级。

Minecraft 1.20.1 与 1.12.2 版本之间不提供世界存档迁移保证。

## 已知限制

- AE2 自动合成只支持 `AE2 rv6-stable-7 + AE2 Fluid Crafting 1.0.11`。
- 单个图样输入使用 32 位 mB 数量；无法安全表示的超大 EMC 成本不会发布图样。
- 机器加载异常 NBT 时只接受对应的 EMC 流体；水、岩浆及其他非法流体会被清空，避免反向兑换 EMC。
- JEI 是客户端辅助模组，不是服务端运行依赖。
- 旧版 Forge 开发环境可能输出有关 ASM 6 `module-info.class`、未签名 CoreMod 或版本检查的警告；它们不属于 EMC Fluid 运行错误。

## 从源码构建

构建需要 JDK 8、Gradle 4.10.3 和 `reference/` 中的固定依赖：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-8.jdk/Contents/Home \
  arch -x86_64 ./gradlew clean build
```

构建会执行 `reobfJar` 和 `verifyReleaseJar`。产物位于：

```text
build/libs/emcfluid-1.0.0-mc1.12.2.jar
```

`verifyReleaseJar` 会拒绝包含开发探针、开发 Access Transformer 或 ProjectE/RS/AE2/AE2FC/JEI 第三方类的发布包。

完整测试证据见 [1.12.2 发布测试报告](docs/1.12.2-release-test-report.md)。
