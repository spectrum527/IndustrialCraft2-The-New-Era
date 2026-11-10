# 素材来源登记

每项生产素材登记一行。`resource_path` 是相对于游戏资源根目录的包内路径；同一路径存在新作时，新作优先。例如，`assets/ia2/textures/...` 对应本仓库的 `assets/ia2/textures/...` 和外部 legacy 资源根下的 `assets/ia2/textures/...`。新作打包到 `assets` 前缀，无须 `assets/assets` 双层目录。

- `status` 只能为 `legacy` 或 `new`；`date` 填写登记日期。
- `legacy` 素材只能存放于独立私有素材仓库，永不进入主仓库 Git 历史；新作素材协议不适用于它们。
- `new` 素材采用 CC BY-SA 4.0，须记录作者和来源；外包及社区投稿须同意该协议。
- 构建测试自造的临时资源不是生产素材，不得列入本表或混入成品。
- 转为公开项目之前，必须确认 `legacy` 为零，并核验完整生产资源清单。空表本身不能证明替换完成或通过发布门禁。

| resource_path | status | author | license | source | date |
|---|---|---|---|---|---|
