# Changelog

## 1.0.0-mc1.12.2 - 2026-08-26

首个 Minecraft 1.12.2 独立版本。

### 新增

- 五级 EMC Fluid、通用桶、世界流体方块和管道 capability。
- EMC Liquefier、EMC Convert Liquefier、EMC Converter 和 EMC Crafter。
- ProjectE 1.12.2 EMC 容器、物品估值、在线/离线知识和 Tome 全知识支持。
- 可绑定/解绑的 Knowledge Pattern，以及知识变化后的自动图样刷新。
- JEI 4.16 Converter 双向 tier 配方和 catalyst。
- Refined Storage 1.6.16 单层、递归、批量流体合成，支持取消、恢复、掉电和网络背压。
- AE2 rv6 + AE2 Fluid Crafting 1.0.11 Fluid Drop/Packet 自动合成，支持真实 Crafting CPU 批量任务和 ME 输出背压。
- 英文与简体中文语言资源。

### 稳定性

- 所有机器库存、流体、模式、进度、知识图样和输出缓存持久化到 NBT。
- 输出采用完整容量模拟后提交，避免部分插入导致复制或丢失。
- RS/AE2 执行和任务恢复时重新验证玩家知识、物品 EMC 与 tier 配置 hash。
- 对异常 NBT 中的模式、tier、转换进度、流体容量、流体类型和物品堆叠进行 fail-closed 清洗；非法的水、岩浆或非 EMC 流体不能兑换 EMC。
- 网络包只允许当前打开的服务端容器修改对应方块；tier delta 被收敛为单步变化。
- 可选模组通过隔离入口延迟加载，AE2FC、RS 或 JEI 缺失时核心模组仍可启动。
- AE2 节点在旧版全模组事件顺序下若尚未 ready，会幂等恢复，避免首次加载时偶发 `node=null`。

### 发布

- 固定 Forge 14.23.5.2860、ForgeGradle 3.0.197、Gradle 4.10.3、Java 8 和 stable_39 mappings。
- 发布 jar 自动 reobf，并通过第三方类、开发探针和开发 Access Transformer 隔离检查。
- 本版本与 Minecraft 1.20.1 分支独立维护，不保证跨版本存档兼容。
