# NetSpeed

Windows / Linux / Android で動くネットワーク速度モニターアプリです。
Wi-Fi・キャリア回線・VPN接続時など、今つながっている回線の速度を **ワンタップのON/OFFボタン**で常時計測し続け、数値と色でひと目でわかるようにします。

VPNはハンドシェイクに成功していても回線自体は遅くなっていることがあります。ONにしている間は数秒おきに自動で再計測を続けるので、VPNの接続/切断を切り替えながら実際の体感速度の変化を確認する、という使い方を想定しています。

## 主な機能

- **ON/OFFボタン**: ONの間は一定間隔（デフォルト4秒）で計測をループし続けます。OFFにするといつでも停止できます。
- **ダウンロード速度 (Mbps)**: 大きめの数字で常時表示。
- **Ping (レイテンシ, ms)**: VPNなどでの応答遅延も合わせて確認できます。
- **色分け表示**: 数値に応じて 緑(快適) / 黄(普通) / 赤(低速) に自動で変化します。

| 色 | ダウンロード速度 | レイテンシ |
|---|---|---|
| 緑 (快適) | 25 Mbps 以上 | 60 ms 以下 |
| 黄 (普通) | 5〜25 Mbps | 60〜150 ms |
| 赤 (低速) | 5 Mbps 未満 | 150 ms 超 |

速度とレイテンシのうち悪い方の評価が採用されます（速いのにレイテンシが酷いVPNを「快適」と誤表示しないため）。しきい値は `composeApp/src/commonMain/kotlin/dev/akagiryohei/netspeed/core/SpeedMonitorController.kt` の `SpeedThresholds` で調整できます。

計測には Cloudflare の公開スピードテストエンドポイント (`speed.cloudflare.com`) を利用しています。APIキーは不要です。

## 技術構成

Kotlin Multiplatform + Compose Multiplatform を採用し、Windows / Linux（デスクトップ）と Android を単一コードベースでカバーしています。

```
composeApp/
  src/
    commonMain/   … UI (App.kt) と計測ロジック (core/) 本体
    desktopMain/  … デスクトップ用エントリポイント & Ktor CIO エンジン
    androidMain/  … Android用エントリポイント & Ktor OkHttp エンジン
```

- UI: Compose Multiplatform (Material3)
- 通信: Ktor Client（デスクトップ: CIO エンジン / Android: OkHttp エンジン）
- 非同期処理: Kotlin Coroutines

## 動かし方

### Windows / Linux (デスクトップ)

```bash
./gradlew :composeApp:run
```

配布用パッケージ（インストーラ）を作る場合:

```bash
# Windows (.msi)
./gradlew :composeApp:packageMsi

# Linux (.deb)
./gradlew :composeApp:packageDeb
```

生成物は `composeApp/build/compose/binaries/` 以下に出力されます。

### Android

Android Studio でこのフォルダを開き、`composeApp` の実行構成（アプリアイコン付き）を実機/エミュレータに対して実行してください。CLIの場合:

```bash
./gradlew :composeApp:installDebug
```

※ インターネット接続の常時計測を行うため `AndroidManifest.xml` で `INTERNET` / `ACCESS_NETWORK_STATE` パーミッションを付与しています。

## 開発メモ

- 計測ロジック(`core/`)はプラットフォームに依存しないので、しきい値や計測間隔、計測先URLの変更は `commonMain` 側だけで完結します。
- `HttpClientFactory` は `expect/actual` でプラットフォームごとの Ktor エンジンを差し込んでいます。
