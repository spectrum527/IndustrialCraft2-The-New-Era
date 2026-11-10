# spec/ — 行为规格（实现者唯一的信息源）

## 管线（decisions.md A9）

`docs/mechanisms/` 的 9 份机制文档是**底稿**，按 `_schema/` 模板 schema 化重构后入库：

1. Spec Agent 取一份机制文档 + 对应 data-workspace CSV。
2. 按 `_schema/machine.md`（或 block/item/recipe/gui/energy 同构模板）重写为 spec 页：补齐**输入 / 输出 / 边界 / 失败模式**四要素、去掉源码行号与 SRG 名、数值表带单位。
3. 冲突项以 `decisions.md` B 表处置为准（修复项写"重建行为=修复后"）。
4. 入库到 `spec/<子系统>/`，PR 由 Spec Lead 评审。

## 模板

- [`_schema/machine.md`](_schema/machine.md) —— 机器/设备规格页（含示例：打粉机骨架）
- 其余模板（block / item / recipe / gui / energy / crop / reactor / sound）在 P1-T1 产出，均以本模板为母版裁剪。

## 状态

- [ ] P1-T1 补齐全部模板
- [ ] 9 份机制文档 → spec/（映射表：《净室重建分期计划》§10.4）
