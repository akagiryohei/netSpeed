# NetSpeed

Windows / Linux / Android で動くネットワーク速度モニターアプリです。
Wi-Fi・キャリア回線・VPN接続時など、今つながっている回線の速度を **ワンタップのON/OFFボタン**で常時計測し続け、数値と色でひと目でわかるようにします。

VPNはハンドシェイクに成功していても回線自体は遅くなっていることがあります。ONにしている間は自動で再計測を続けるので、VPNの接続/切断を切り替えながら実際の体感速度の変化を確認する、という使い方を想定しています。

## 主な機能

- **ON/OFFボタン**: ONの間は一定間隔で計測をループし続けます。OFFにするといつでも停止できます。Androidではフォアグラウンドサービス通知と連動し、アプリをバックグラウンドに回しても（VPNアプリ側を操作している間も）計測を継続します。
- **ダウンロード / アップロード速度 (Mbps)・Ping (ms)**: 3つとも常時表示。
- **色分け表示**: 数値に応じて 緑(快適) / 黄(普通) / 赤(低速) に自動で変化します。
- **接続種別バッジ**: Wi-Fi / モバイル回線 / VPN経由 / 有線LAN を自動判定して表示します。
- **モバイル通信のデータ節約**: モバイル回線と判定した場合、自動でテストサイズを縮小・間隔を延長し、無制限プランでない回線での通信量を抑えます（設定でオフに可能）。
- **直近の推移グラフ**: 直近の計測値をミニグラフで表示します。
- **設定画面**: 計測間隔・しきい値・データ節約の可否を変更できます（端末に保存されます）。
- **ダークモード対応 / 日英自動切り替え**: OSの設定に追従します。
- **オフライン時の自動リトライ抑制**: 接続不可が続くと再試行間隔を自動的に延ばし、無駄な通信を防ぎます。

| 色 | ダウンロード速度 | レイテンシ |
|---|---|---|
| 緑 (快適) | 25 Mbps 以上 (設定で変更可) | 60 ms 以下 |
| 黄 (普通) | 5〜25 Mbps (設定で変更可) | 60〜150 ms |
| 赤 (低速) | 5 Mbps 未満 | 150 ms 超 |

速度とレイテンシのうち悪い方の評価が採用されます（速いのにレイテンシが酷いVPNを「快適」と誤表示しないため）。

計測には Cloudflare の公開スピードテストエンドポイント (`speed.cloudflare.com`) を利用しています。APIキーは不要です。

## 技術構成

Kotlin Multiplatform + Compose Multiplatform を採用し、Windows / Linux（デスクトップ）と Android を単一コードベースでカバーしています。

```
composeApp/
  src/
    commonMain/   … UI (App.kt) と計測ロジック (core/) 本体
    desktopMain/  … デスクトップ用エントリポイント & Ktor CIO エンジン、java.util.prefs による設定保存
    androidMain/  … Android用エントリポイント、フォアグラウンドサービス通知、SharedPreferencesによる設定保存
```

- UI: Compose Multiplatform (Material3)
- 通信: Ktor Client（デスクトップ: CIO エンジン / Android: OkHttp エンジン）
- 非同期処理: Kotlin Coroutines
- 設定・状態管理: `AppGraph`（プロセス全体で1つの `SpeedMonitorController` を共有し、UIとAndroidのフォアグラウンドサービスが同じ計測ループを見る設計）

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

Android Studio でこのフォルダを開き、`composeApp` の実行構成を実機/エミュレータに対して実行してください。CLIの場合:

```bash
./gradlew :composeApp:installDebug
```

初回起動時、Android 13以降では通知の許可を求められます（バックグラウンド計測中の通知表示に使用）。許可しなくてもアプリ自体は動作します。

## リリース署名について

`composeApp/build.gradle.kts` はリポジトリ直下の `keystore.properties`（**Gitには含まれません**）を読み込んで署名します。まだ用意していない場合は次のように作成してください。

```properties
storeFile=keystore/netspeed-release.jks
storePassword=xxxxxxxx
keyAlias=netspeed
keyPassword=xxxxxxxx
```

keystoreファイル自体を初めて用意する場合:

```bash
keytool -genkeypair -v -keystore keystore/netspeed-release.jks \
  -alias netspeed -keyalg RSA -keysize 2048 -validity 10950
```

**keystoreファイルとパスワードは紛失すると同じアプリとしてPlay Storeを更新できなくなります。** パスワードマネージャーなどに安全に保管し、`keystore.properties` と `keystore/` フォルダは絶対にコミットしないでください（`.gitignore` 済みです）。

リリースビルド:

```bash
./gradlew :composeApp:bundleRelease   # Google Play提出用 AAB
./gradlew :composeApp:assembleRelease # 単体テスト用 APK
```

## Google Play 公開チェックリスト

- [ ] Google Play Console 開発者アカウント登録（$25、個人の場合は本人確認あり）
- [ ] 上記の keystore を使って署名済み AAB をビルド (`bundleRelease`)
- [ ] ストア掲載情報（アイコン512x512・フィーチャーグラフィック1024x500・スクリーンショット）を用意
- [ ] プライバシーポリシーのURLとして [`PRIVACY_POLICY.md`](./PRIVACY_POLICY.md) のGitHub上のURLを登録
  (`https://github.com/akagiryohei/netSpeed/blob/main/PRIVACY_POLICY.md`)
- [ ] データセーフティフォームに記入（個人情報は収集していません）
- [ ] コンテンツレーティング質問票に回答
- [ ] Foreground Service (dataSync) の利用目的をPlay Consoleの申告フォームで説明
  （例: 「ユーザーがONにした間、VPN接続などのネットワーク速度変化をバックグラウンドでも計測し続けるため」）
- [ ] 新規個人開発者アカウントの場合、closed testingを12人以上のテスターで14日間実施してから本番公開
- [ ] 申請時点でのPlay必須ターゲットAPIレベルを確認し、`gradle/libs.versions.toml` の `android-targetSdk` を必要に応じて更新

## 開発メモ

- 計測ロジック(`core/`)はプラットフォームに依存しないので、しきい値や計測間隔、計測先URLの変更は `commonMain` 側だけで完結します。
- `HttpClientFactory` / `SettingsStore` / `NetworkType` / `Strings` (言語判定) はいずれも `expect/actual` でプラットフォームごとの実装を差し込んでいます。
- `AppGraph.controller` はプロセス全体で単一インスタンスです。Android側の `MonitorService`（フォアグラウンドサービス）はこの同じインスタンスを購読して通知を更新するだけで、計測ループそのものは持ちません。
