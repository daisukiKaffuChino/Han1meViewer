# 🚫 请不要在任何公开平台宣传本软件

本软件不接受任何形式的公开宣传。若出现公开宣传、搬运或引流，仓库维护者可能随时归档或隐藏仓库，并删除已编译的发行版。

# 🌸 Han1meViewer+

🔞 **R18 警告：未满 18 岁禁止下载和使用。**

Han1meViewer 是一个使用 Kotlin 开发的 Android 客户端，用于浏览、搜索、播放和管理 hanime 相关公开视频页面内容。当前项目以 Jetpack Compose、Navigation Compose、ViewModel、StateFlow、Retrofit、Jsoup、Room、WorkManager、Media3/JZVD/MPV 为主要技术栈，围绕视频浏览、详情播放、搜索、用户列表、下载管理、评论、订阅、设置和隐私保护等功能组织。

本应用没有任何官方网站。GitHub Release 是唯一下载及更新渠道。

本项目[原仓库](https://github.com/misaka10032w/Han1meViewer)已归档。因为对项目弃坑感到惋惜，同时也是原项目的使用者，所以现由我进行接下来的维护。

以下是原项目维护者的停更声明：

```
鉴于很多用户的意见，对部分功能存在较大意见，开发目标产生分歧，但是又说不出个所以然来，我又猜不到你想说什么，故：

本项目将不再提供更新，仓库将归档，并已删除所有编译产物，如您需要更丰富的功能，请自行下载修改编译。

有缘再见
```

还请大家好好看片，不要去打击用爱发电的开发者们的积极性

# 📜 目前做了什么

### 移除了

- GMS 和 Firebase 追踪统计模块
- CI 更新频道
- 旧外部存储读写权限与 Android 9 以下兼容代码
- 创作中心、日本语翻译（日本网友无法访问H站，保留日语无意义）
- 旧的主题、多语言和依赖传统 View 的工具类

### 新增和重构了

- 恢复了上版本移除的 冲了么 功能，其小组件使用 Glance Compose 重构
- 迁移遗留的弹窗和列表到 Compose 并清理了未引用的 xml 残留
- 基于 MomoQR 的优秀架构，使用 Material 3 Expressive 风格完全重构了用户界面，包含全新的视觉风格和底层逻辑
- ...

### 修复和完善了

- 视频卡片解析适配网站结构变化，补全作者与时长显示
- 视频播放页优化沉浸式系统栏
- 修复退出登录后仍可浏览在线观看历史等问题
- 强化 Cloudflare 验证后的 Cookie 主机隔离、并发等待、取消与超时处理
- ...

## 🤝 贡献说明

- 提交代码前请先确认可以通过 `:app:compileDebugKotlin`。
- 修改网络列表、分页或 Compose `Lazy*` 列表时，请检查重复 key 风险。
- 修改播放、下载、账号、Cookie、Cloudflare、更新逻辑时，请尽量说明验证方式。
- 提交共享关键 H 帧可参考 `.github/PULL_REQUEST_TEMPLATE/submit_h_keyframe.md`。

## 🧩 TODO

- 最终移除 JZVD

# 📄 许可证

- 本项目作为包含 GPLv3 派生代码的整体，按 GNU GPLv3 发布。
- 项目包含来自 [MomoQR](https://github.com/daisukiKaffuChino/MomoQR) 的代码，归属作者 daisukiKaffuChino，并遵循 GPLv3。
- 原项目 Yenaly 的遗留归属和 MomoQR 归属见 [NOTICE](NOTICE)，Apache-2.0 许可证文本见 [LICENSE-APACHE](LICENSE-APACHE)；完整许可证说明见 [LICENSE](LICENSE)。
