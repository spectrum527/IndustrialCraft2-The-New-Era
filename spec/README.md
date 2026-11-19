# spec/ — 行为规格（实现者唯一的信息源）

## 管线（decisions.md A9）

`docs/mechanisms/` 的 9 份机制文档是**底稿**，按 `_schema/` 模板 schema 化重构后入库：

1. Spec Agent 取一份机制文档 + 对应 data-workspace CSV。
2. 按 `_schema/machine.md`（或 block/item/recipe/gui/energy 同构模板）重写为 spec 页：补齐**输入 / 输出 / 边界 / 失败模式**四要素、去掉源码行号与 SRG 名、数值表带单位。
3. 冲突项以 `decisions.md` B 表处置为准（修复项写"重建行为=修复后"）。
4. 入库到 `spec/<子系统>/`，PR 由 Spec Lead 评审。

## 模板

- [`_schema/machine.md`](_schema/machine.md) —— 母版：机器行为、升级与自动化
- [`_schema/block.md`](_schema/block.md) / [`_schema/item.md`](_schema/item.md) —— 方块与物品
- [`_schema/recipe.md`](_schema/recipe.md) / [`_schema/gui.md`](_schema/gui.md) / [`_schema/energy.md`](_schema/energy.md) —— 配方、界面与能量
- 打粉机规格与 M01/M08 参考观测见[打粉机样板](machines/macerator.md)；候选机器需运行 M09。
- crop / reactor / sound 模板留待对应任务，P1-T1 不扩展可选模板；这些规格不是 MVP/G0 范围批准。

首发物流的管段容量和配方见[物品管、流体管与泵盖](logistics/pipes.md)。直管每刻到货量、出口分配、满仓重试及跨区块重连见[路由与恢复样本](logistics/routing.md)。

## 状态

- [x] P1-T1 六类模板和打粉机样板已交付
- [ ] 9 份机制文档 → spec/（映射表：《净室重建分期计划》§10.4）
