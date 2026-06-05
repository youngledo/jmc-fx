# JMC FX

中文 | [English](README.md)

---

JMC FX 是一个独立的 JavaFX 桌面应用，目标是重建 JDK Mission Control 的用户界面，同时复用 [JMC](https://github.com/openjdk/jmc) core/headless 库。

## 环境要求

- JDK 26
- JavaFX 26
- 本仓库自带的 Maven Wrapper，它会下载 Maven 4.0.0-rc-5

本仓库包含 `.sdkmanrc`，其中声明了项目所需的 Java 版本。如果你使用 SDKMAN，请先激活项目 JDK，再运行 Maven：

```bash
sdk env
./mvnw -v
```

Maven Wrapper 会固定 Maven 版本，但不会自动选择 JDK。执行构建或运行命令前，`./mvnw -v` 必须显示 Java 26。

## 构建

```bash
sdk env
./mvnw verify
```

架构边界说明见 `docs/hexagonal-boundary-guide.md`。新增工作流、端口、适配器、UI 页面或启动装配时，请参考该文档。

## 平台安装包

使用 `jpackage-classpath-jlink-leyden` profile 构建当前平台的 Leyden 优化安装包：

```bash
sdk env && ./mvnw -pl jmc-fx-launcher -am -Pjpackage-classpath-jlink-leyden package
```

当前只有一个 Leyden 安装包 profile。它会把打包工作流委托给已发布到 Maven Central 的独立插件 `io.github.youngledo:jpackage-maven-plugin`。插件的 `leyden` goal 会检测当前操作系统，并在内部推导平台相关的 `jpackage` 选项、安装包类型、app image 路径和 AOT cache 位置。安装包输出到 `jmc-fx-launcher/target/jpackage-leyden/`。在 macOS 上，默认产物是 `JMC FX-1.0.0.dmg`。

Leyden 打包被有意放在显式 profile 后面，而不是放进默认构建。安装包流程会进入 Maven 的 `package` 阶段，先运行一次应用来训练 AOT cache，再把 jpackage launcher 配置从 `-XX:AOTCacheOutput` 改写为 `-XX:AOTCache`，最后打包训练后的 app image。保持显式启用可以避免普通 `./mvnw verify` 构建安装包。

该安装包使用经过 `jlink` 裁剪的 JDK/JavaFX 运行时，并从 classpath 启动应用。OpenJDK JMC 9.1.2 依赖目前是 automatic modules，因此暂时不能被链接进完整的 JPMS `jlink` 镜像。等 JMC artifacts 成为显式 JPMS modules 后，这条安装包路径可以迁移为 module 启动和完整 application-module `jlink` 镜像。

未来如果引入 GraalVM native-image 打包，应使用单独 profile，不要复用当前 jpackage/Leyden 安装包路径。

`jpackage` 要求平台相关的包版本格式。默认包版本是 `1.0.0`，因为 macOS 会拒绝首位为 `0` 的版本。发布时可以这样覆盖：

```bash
./mvnw -pl jmc-fx-launcher -am -Pjpackage-classpath-jlink-leyden -Djmcfx.package.version=1.2.3 package
```

## 运行

```bash
sdk env && ./mvnw -pl jmc-fx-launcher -am org.openjfx:javafx-maven-plugin:0.0.8:run
```

## 法律声明

JMC FX 是一个独立项目，与 Oracle 或 OpenJDK 项目不存在从属、认可、背书或赞助关系。
