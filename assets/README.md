# assets/ — 新作素材 + 素材来源登记

授权基线（decisions.md A1/A7/A21/A22）：

- **私有阶段**可复用原版贴图、音效、模型、GUI，但原版文件**一律放在独立私有仓库 `ia2-legacy-assets`**（本地 `../../ia2-legacy-assets/`），**禁止进入本仓库 git 历史**。
- **本目录只放新作素材**（CC BY-SA 4.0），以及登记表 `SOURCES.md`。
- **转 Public 前**：所有原版素材必须被新作覆盖（贴图、模型、音效、GUI 一项不留），然后删除构建对 `ia2-legacy-assets` 的引用。
- IC2 名称/商标不可用：新作中不得出现 "IndustrialCraft"/"IC2" 字样或 logo。

## 规则

1. 原版素材：从原 jar 解包目录**拷贝**到 `ia2-legacy-assets`（保持 jar 内相对路径，命名空间改为 `ia2`），禁止原地修改解包源。Gradle 以属性 `ia2.legacyAssetsDir` 作为额外资源目录引入；缺省时构建仍能通过（缺失纹理可接受）。
2. 新作素材：放本目录对应路径。与原版同路径时，**新作优先**（打包时覆盖）。
3. `SOURCES.md` 每行一条：资源路径 → 状态（`legacy` / `new`）→ 作者 → 协议 → 日期。转 Public 的门禁是：`legacy` 行数 = 0。
4. 外包或社区投稿的新作，须书面同意以 CC BY-SA 4.0 授权，记录在 `SOURCES.md` 作者列。

## 目录

```
assets/
├── SOURCES.md      登记表（legacy / new 状态，转 Public 门禁依据）
└── ia2/...         新作素材（与资源包路径一致）
```
