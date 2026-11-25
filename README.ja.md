# BitwigOSC

[English](README.md)

[TouchOSC](https://hexler.net/touchosc) から Bitwig Studio を、OSC で直接操作するための Bitwig の拡張です。

- 8 本のトラックの音量フェーダー、mute / solo / arm、再生、ループを操作できます。
- Bitwig 側で変えた値とトラックの色は、TouchOSC に送り返します。
- レイアウトを開くと、Bitwig の今の状態に揃います。

Bitwig Studio 6.1(Controller API 25)、Windows 11、iPad の TouchOSC で動作を確かめています。

## 入れ方

1. [Releases](../../releases) から `BitwigOSC.bwextension` と `BitwigOSC.tosc` を取ってきます。
2. `BitwigOSC.bwextension` を `ドキュメント/Bitwig Studio/Extensions/` に置きます。
3. Bitwig の **Settings > Controllers > + Add Controller** で、**BitwigOSC > TouchOSC** を追加します。
4. 追加したコントローラーの設定で、**TouchOSC host** に TouchOSC を動かす機器の IP アドレスを入れます。
5. TouchOSC で `BitwigOSC.tosc` を開き、**Connections > OSC** を次のように設定します。
   - **Host**: Bitwig を動かす PC の IP アドレス
   - **Send port**: `10001`
   - **Receive port**: `10002`

ポートや送り先を変えると、拡張は自動で起動し直します。

## 設定

| 設定 | 初期値 | 意味 |
|---|---|---|
| Receive port | 10001 | TouchOSC → Bitwig |
| TouchOSC host | 127.0.0.1 | 拡張から送る先 |
| Send port | 10002 | Bitwig → TouchOSC |
| Log incoming messages | オン | 届いたメッセージを Control Script Console に出す |

## OSC のアドレス

`/{番号}/...` の番号は、ミキサーの左から数えたトラックの順番です(1〜8)。`/0/...` はトランスポート(再生やループ)です。
詳しい決まりは [docs/specs/osc-protocol.md](docs/specs/osc-protocol.md) にあります。

| アドレス | 値 | 向き・意味 |
|---|---|---|
| `/{1-8}/volume`、`/{1-8}/pan` | 0.0〜1.0 | 両方向 |
| `/{1-8}/mute`・`solo`・`arm`・`active` | 1.0 / 0.0 | 両方向 |
| `/0/playState`、`/0/loopEnabled` | 1.0 / 0.0 | 両方向 |
| `/{1-8}/colorR`・`colorG`・`colorB` | 0.0〜1.0 | Bitwig → TouchOSC。トラックがない位置は黒 |
| `/sync` | なし | TouchOSC → Bitwig。今の状態を全部送り返す |
| `/ping` | なし | TouchOSC → Bitwig。`/pong` を返す |

このアドレスを送るレイアウトなら、自作のレイアウトでも使えます。

## うまく動かないとき

Bitwig で **Ctrl + Enter** を押して `console` と入れ、**Show Control Script Console** を開いて BitwigOSC を選ぶと、拡張のログが見えます。

| ログ | 意味 |
|---|---|
| `BitwigOSC 0.4.0: listening on 10001, sending to <送り先>:10002` | 拡張が起動した |
| `OSC ignored: <アドレス> (unknown address)` | 上の表にないアドレスが届いた |
| `OSC out: /pong` は出るのに TouchOSC に届かない | **TouchOSC host** が機器の IP アドレスになっているかを確かめる。UDP は届かなくてもエラーにならない |

## ソースからビルドする

JDK 21 以上が要ります。Gradle Wrapper は同梱しています。

```
cd extension
./gradlew test              # テスト
./gradlew installExtension  # ビルドして ドキュメント/Bitwig Studio/Extensions/ に置く
./gradlew generateLayout    # touchosc/BitwigOSC.tosc を作り直す
```

`touchosc/BitwigOSC.tosc` は手で直さないでください。`extension/src/touchosc/` から作るファイルで、コミットしたものが古いと `./gradlew test` が失敗します。

## ライセンス

[MIT](LICENSE)

これは非公式のプロジェクトで、Bitwig GmbH や Hexler とは関係がなく、両社の承認も受けていません。Bitwig と Bitwig Studio は Bitwig GmbH の商標です。TouchOSC は Hexler の商標です。
