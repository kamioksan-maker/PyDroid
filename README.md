# PyDroid - Android Python 运行器

在 Android 手机上运行 Python 脚本的移动应用。

## 功能特性

- ✅ 创建、编辑、保存 Python 脚本
- ✅ 内置代码编辑器
- ✅ 一键运行脚本
- ✅ 查看运行历史和输出
- ✅ 文件浏览器管理脚本
- ✅ 支持常用 Python 包（numpy, requests, pillow, matplotlib）
- ✅ 后台运行支持

## 技术栈

- **语言**: Kotlin + Python
- **Python 引擎**: Chaquopy 15.0.1
- **Python 版本**: 3.8
- **最低 Android**: 7.0 (API 24)
- **编译**: Gradle 8.0 + GitHub Actions

## 编译 APK

### 方法 1: GitHub Actions（推荐）

1. 确保所有文件已上传到此仓库
2. 进入仓库 **Actions** 标签
3. 点击 **Build APK** 工作流
4. 点击 **Run workflow**
5. 等待编译完成（约 5-10 分钟）
6. 在 **Artifacts** 中下载 `app-debug.apk`

### 方法 2: 本地编译

