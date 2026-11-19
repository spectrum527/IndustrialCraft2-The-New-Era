# Reference 索引（净室规则：仅 Spec 角色可读）

`rebuild/reference/` **不拷贝**反编译产物（decisions.md A8），本索引指向唯一事实源。迁移工作区到其他机器时，先按下表恢复 `../analysis/` 与 `../analysis-1122/` 再工作。

首发以修复版 `1.2.0-port.20261119` 的玩家行为、产出和失败场景为对照；原版 2.9.40 补未覆盖行为，2.8.222 交叉核对。原始观测与重建目标分别保存（A23/A27）。修复目录仅向 Spec 开放；由隔离环境运行源码相似度比较，向实现与 QA 提供行为规格和脱敏结果。

## 基线与完整性

| 对象 | 路径 | SHA-256 |
|---|---|---|
| 1.19.2 修复正式包与同目录发行验收记录 | `../../../IC2 Repair copy/deliverable/release-2026-11-19.2/industrialcraft-2-1.19.2-port.20261119.2.jar` | `dc1b803b24101d6372f3cbf7ffb633fed21f5339efb16e455e5a4ed5cbe0ccab` |
| Forge 原始 jar | `../../industrialcraft-2-2.9.40+ex119-1.19.2-forge.jar` | `5d06ca9146041043132a72bad9b5612e260e77a84a2f3773a3da9b0167ea1d3d` |
| Fabric 原始 jar | `../../industrialcraft-2-2.9.40+ex119-1.19.2-fabric.jar` | `09787119172eda213f51fc5785d8a91a88bbcc18124e66d1b0cb384b5a42b154` |
| Forge 反编译源码 | `../../analysis/src/`（901 .java，Vineflower 1.10.1，SRG 名） | 复现：`java -jar analysis/tools/vineflower.jar -dgs=1 -rsy=1 -ind="    " analysis/extracted analysis/src` |
| Fabric 反编译源码 | `../../analysis/src-fabric/`（895 .java，intermediary 名） | 同上，对 fabric 解包目录 |
| Forge 解包资源 | `../../analysis/extracted/`（assets 2,069 JSON + 1,541 PNG + 125 OGG；data 16 类配方） | unzip 原始 jar |
| Fabric 解包资源 | `../../analysis/extracted-fabric/` | 同上 |
| 1.12.2 原始 jar（2.8.222，A23 交叉验证与 C-MF 缺失功能底稿） | `../../industrialcraft-2-2.8.222-ex112.jar` | `f96b1886b6d060b308fb811029d5c5cc76deb17b835aa368dcb6071f2de290d4` |
| 1.12.2 反编译源码 | `../../analysis-1122/src/`（1,162 .java，Vineflower；SRG `func_`/`field_` 名，对照 MCP 1.12 映射） | 复现：同上 Vineflower 命令，对 `analysis-1122/extracted` 输出到 `analysis-1122/src`；日志 `analysis-1122/decompile.log` |
| 1.12.2 解包资源 | `../../analysis-1122/extracted/` | unzip 原始 jar |
| 1.12.2 缺失功能普查 | `../../missing-features/`（README + 4 份子系统报告 + 类/注册表 diff，含源码位置） | 仅 Spec 角色可读 |

复现全流程见《拆解指南》§17.3；SRG 名→官方名对照查 MCPConfig joined.tsrg + Mojang client.txt（机制文档头部已带常用对照表）。

## Spec 角色的典型查法

| 要查什么 | 去哪 |
|---|---|
| 某机器的行为公式 | `docs/mechanisms/<对应文档>.md` 先看（已核对过源码），再回源码验证行号 |
| 注册 ID/数值表 | `../data-workspace/01-registry/`、`03-machines/` 等 CSV（带 source 列可直接跳源码） |
| 配方 JSON | `../../analysis/extracted/data/ic2/recipes/`（44 CSV 是它的视图） |
| 配置键 | `../data-workspace/12-reference/config_keys_full.csv`（91 键含 consumer 行号） |
| 1.12.2 独有功能 | `../../missing-features/README.md` 先看，再进对应子系统报告与 `analysis-1122/src/` |
| 贴图/音效映射 | `../data-workspace/10-art/inventory.csv`、`texture_mapping.csv` |

## 素材复用（decisions.md A7）

A7 要求所有机器、物品、线缆纹理独立重绘。原样文件留作私有参考，其他素材的私有预览按 A22 执行。复用时**拷贝**到独立私有仓库 `../../ic2-legacy-assets/`（保持 jar 内相对路径，**不得进入 rebuild 主仓库历史**，A22），并在 `../assets/SOURCES.md` 登记为 `legacy`。禁止原地修改 `analysis/extracted/`、`analysis-1122/extracted/` 下任何文件。1.12.2 素材同样适用（A24）。
