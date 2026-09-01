# プライバシーポリシー / Privacy Policy

最終更新日 / Last updated: 2026-09-01

## 日本語

NetSpeed（以下「本アプリ」）は、akagiryohei（以下「開発者」）が提供するネットワーク速度計測アプリです。

### 収集する情報

本アプリは、氏名・メールアドレス・位置情報などの**個人を特定できる情報を一切収集しません**。アカウント登録やログインも不要です。

### 通信について

本アプリは速度計測のため、以下の外部サービスに対して匿名の通信を行います。

- **Cloudflare Speed Test エンドポイント** (`speed.cloudflare.com`): ダウンロード/アップロード速度・レイテンシ計測用のデータ送受信

これらの通信には、計測に必要なデータ（ダミーのバイト列）以外の個人情報は含まれません。本アプリ自身が計測結果をどこかのサーバーに送信・保存することはなく、計測結果は端末内（アプリのメモリ・設定ストレージ）にのみ保持されます。

### 権限について

- **INTERNET / ACCESS_NETWORK_STATE**: 速度計測および接続種別（Wi-Fi・モバイル回線・VPN）の判定に使用します。
- **POST_NOTIFICATIONS**: 常時計測(ON)中にバックグラウンドでも計測を継続するための通知表示に使用します。
- **FOREGROUND_SERVICE / FOREGROUND_SERVICE_DATA_SYNC**: 上記の通知と連動し、計測ループをバックグラウンドで維持するために使用します。

### 第三者への提供

開発者が取得した情報を第三者に提供することはありません（そもそも個人情報を取得していません）。

### お問い合わせ

本ポリシーに関するお問い合わせは、本リポジトリの GitHub Issues までお願いします。
https://github.com/akagiryohei/netSpeed/issues

---

## English

NetSpeed ("the App") is a network speed measurement app provided by akagiryohei ("the Developer").

### Information We Collect

The App does **not** collect any personally identifiable information (name, email address, location, etc.). No account or sign-in is required.

### Network Communication

To measure speed, the App makes anonymous requests to the following third-party service:

- **Cloudflare Speed Test endpoints** (`speed.cloudflare.com`): used to measure download/upload throughput and latency.

These requests contain only the data needed for the measurement itself (dummy byte payloads) — no personal information. The App does not send or store your measurement results on any server; results live only on your device (in app memory and local settings storage).

### Permissions

- **INTERNET / ACCESS_NETWORK_STATE**: used to run speed tests and detect the current connection type (Wi-Fi, cellular, VPN).
- **POST_NOTIFICATIONS**: used to show a notification so monitoring can continue in the background while turned ON.
- **FOREGROUND_SERVICE / FOREGROUND_SERVICE_DATA_SYNC**: used together with that notification to keep the measurement loop running in the background.

### Sharing

The Developer does not share any data with third parties (none is collected in the first place).

### Contact

For questions about this policy, please open an issue on this repository:
https://github.com/akagiryohei/netSpeed/issues
