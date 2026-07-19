# 🚫 请不要在任何公开平台宣传本软件

本软件不接受任何形式的公开宣传。若出现公开宣传、搬运或引流，仓库维护者可能随时归档或隐藏仓库，并删除已编译的发行版。

# 🌸 Han1meViewer+

本项目[原仓库](https://github.com/misaka10032w/Han1meViewer)已归档。因为对项目弃坑感到惋惜，同时也是原项目的使用者，所以现由我进行接下来的维护。

以下是原项目维护者的停更声明：

```
鉴于很多用户的意见，对部分功能存在较大意见，开发目标产生分歧，但是又说不出个所以然来，我又猜不到你想说什么，故：

本项目将不再提供更新，仓库将归档，并已删除所有编译产物，如您需要更丰富的功能，请自行下载修改编译。

有缘再见
```

还请大家好好看片，不要去打击用爱发电的开发者们的积极性

# 🌸 目前做了什么

- 移除了 GMS 和 Firebase 追踪统计模块

- 移除了 CI 更新频道

- ...


# 以下是原仓库 README

# 🌸 Han1meViewer

🔞 R18 警告：未满 18 岁禁止下载和使用。

Han1meViewer 是一个使用 Kotlin 开发的 Android 客户端，用于浏览、搜索、播放和管理 hanime 相关公开视频页面内容。当前项目以 Jetpack Compose、Navigation Compose、ViewModel、StateFlow、Retrofit、Jsoup、Room、WorkManager、Media3/JZVD/MPV 为主要技术栈，围绕视频浏览、详情播放、搜索、用户列表、下载管理、评论、订阅、设置和隐私保护等功能组织。

本应用没有任何官方网站。GitHub Release 是唯一下载及更新渠道。

## 📷 截图预览

![readme0](readme_01.png) ![readme1](readme_02.png)

![readme2](readme_03.png) ![readme3](readme_04.png)

![readme4](readme_05.png) ![readme5](readme_06.png)

![readme6](readme_07.png)

## 🧱 当前架构

📦 项目采用多模块 Gradle 结构：

- `:app`：主应用模块，包含界面、导航、业务 ViewModel、网络解析、数据库、下载 Worker、播放器和资源文件。
- `:yenaly_libs`：项目内公共基础库，封装通用 Activity、Fragment、ViewModel、偏好设置、工具类和基础组件。
- `buildSrc`：构建配置辅助代码，用于版本号、构建类型和提交信息等 Gradle 逻辑。

🗂️ 主应用采用分层组织：

- `ui.activity`：入口 Activity、登录、Cloudflare 处理和手动 Cookie 输入页面。
- `ui.navigation`：Navigation Compose 路由定义、主导航、设置导航和安全跳转封装。
- `ui.screen`：Compose 页面实现，按首页、搜索、视频、设置、账号、下载等业务域划分。
- `ui.component`：可复用 Compose 组件，如视频卡片、弹窗、空/错/加载状态、AppBar、评论卡片等。
- `ui.viewmodel`：页面状态和业务动作入口，主要通过 `StateFlow` / `SharedFlow` 对 UI 暴露状态。
- `logic.network`：Retrofit Service、OkHttp 拦截器、DNS、CookieJar、更新服务和网络入口。
- `logic.model`：页面和网络解析后的领域模型，如 `HanimeVideo`、`HanimeInfo`、`HomePage`、`Playlists` 等。
- `logic.dao` / `logic.entity`：Room 数据库、DAO 和实体，用于历史、下载、搜索历史、关键帧、打卡等本地数据。
- `worker`：WorkManager 下载任务和更新任务。
- `util`：主题、网络、文件、权限、Cookie、Toast、视频和通用工具方法。

🔁 整体数据流：

```text
Compose Screen -> ViewModel -> NetworkRepo / DatabaseRepo -> Retrofit + Jsoup / Room -> StateFlow -> Compose Screen
```

🎞️ 视频页数据流：

```text
VideoRoute -> VideoViewModel -> NetworkRepo.getHanimeVideo -> Parser -> HanimeVideo -> VideoScreen / Player / CommentScreen
```

📥 下载数据流：

```text
DownloadScreen -> DownloadViewModel -> HanimeDownloadManagerV2 -> WorkManager Worker -> Room -> Flow -> UI
```

## 🛠️ 技术栈

- Kotlin 2.3.x
- Java 21 toolchain
- Android Gradle Plugin 9.2.x
- Jetpack Compose + Material 3
- Navigation Compose + Kotlin Serialization typed routes
- ViewModel + StateFlow + SharedFlow
- Retrofit 3 + kotlinx.serialization converter
- Jsoup HTML 解析
- Room + KSP
- WorkManager 后台任务
- Coil 3 Compose 图片加载
- Media3 ExoPlayer、JZVD、MPV Android
- AndroidX Window、Biometric、SplashScreen、Preference
- AboutLibraries 开源许可展示

## 🧰 构建环境

✅ 推荐环境：

- Android Studio Panda 或更新版本。
- JDK 21。
- Gradle Wrapper 使用仓库内置版本。
- 编译 SDK：`37`。
- 最低支持：Android 8.1，API `27`。
- 目标 SDK：`37`。

📌 关键版本以仓库文件为准：

- `gradle.properties`
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `buildSrc/src/main/java/Config.kt`

## 🚀 本地运行

1. 克隆项目：

   ```bash
   git clone https://github.com/misaka10032w/Han1meViewer.git
   ```

2. 使用 Android Studio 打开项目根目录。

3. 等待 Gradle Sync 完成。

4. 选择 `debug` 变体运行到设备或模拟器。

💻 常用命令：

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```

🪟 Windows PowerShell：

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:assembleDebug
```

🔏 Release 构建需要本地提供签名相关环境变量：

- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`

## ✅ 代码约定

- UI 优先使用 Jetpack Compose，少量历史页面和组件仍保留 XML / ViewBinding / DataBinding。
- 页面状态通过 `StateFlow` 暴露，事件通过 `SharedFlow` 或回调传递。
- 网络页面列表追加时应按稳定业务键去重，避免 Compose `Lazy*` 列表 key 冲突。
- `LazyColumn`、`LazyVerticalGrid`、`LazyRow` 使用稳定 key 时应确保数据源唯一。

## 🗺️ 目录速查

```text
Han1meViewer/
├── app/                         主 Android 应用
│   ├── src/main/java/.../logic   网络、解析、模型、数据库、状态
│   ├── src/main/java/.../ui      Activity、Compose 页面、导航、组件、ViewModel
│   ├── src/main/java/.../util    通用工具
│   ├── src/main/java/.../worker  WorkManager 任务
│   └── src/main/res              资源文件、主题、布局、图标、小组件
├── yenaly_libs/                  项目内公共库
├── buildSrc/                     Gradle 构建辅助代码
├── gradle/libs.versions.toml     依赖版本目录
├── README.md                     项目说明
└── README_TECH.md                历史技术说明与部分实现笔记
```

## 🤝 贡献说明

- 提交代码前请先确认可以通过 `:app:compileDebugKotlin`。
- 修改网络列表、分页或 Compose `Lazy*` 列表时，请检查重复 key 风险。
- 修改播放、下载、账号、Cookie、Cloudflare、更新逻辑时，请尽量说明验证方式。
- 提交共享关键 H 帧可参考 `.github/PULL_REQUEST_TEMPLATE/submit_h_keyframe.md`。

## 🧩 TODO

- 随时有想法随时写。

## 📄 许可证

本项目继承原始项目的 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)。主要条款包括：

- 允许商用、修改和分发。
- 要求保留版权声明和许可证文件。
- 要求提供修改说明。
- 不提供质量担保。
- 不承担用户使用风险。

完整条款请参阅项目根目录下的 [LICENSE](LICENSE) 文件。
