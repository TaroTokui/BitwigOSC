---
name: release
description: BitwigOSC の新しい版を出す手順(版の決定 → 確認 → 機能とリリースのコミットを分ける → タグ → GitHub Release に .bwextension と .tosc を付ける)。「リリースしましょう」「0.x.y を出して」「版を上げて公開」「タグを打って」「GitHub に出して」など、作業を公開・配布したいと言われたら、版の番号が書かれていなくても必ずこのスキルを使うこと。コミットだけ・push だけの依頼には使わない。
---

# BitwigOSC のリリース

利用者が「リリース」と言ったら、公開(push・タグ・GitHub Release)まで任されたと読んでよい。
ただし、版の番号の上げ方が決めきれないときや、iPad での確認が済んでいない変更が入るときは、先に一言確かめる。
公開したものは取り消しても残る(キャッシュ・通知)ので、迷ったら止まって聞く。

## 0. 今の状態を見る

```bash
git status --short
git log --oneline origin/main..main   # まだ push していないコミット
git tag -l | tail -3; gh release list | head -3
```

- ブランチは `main`。これまでのリリースは `main` に直接コミットして push している。
- 前のリリースの形をまねる: `git show --stat <前の release のコミット>`、`gh release view v<前の版>`。

## 1. 版の番号を決める

0.x の間は次のようにする(semver の 0 版の慣習)。

| 入っている変更 | 上げる所 | 例 |
|---|---|---|
| OSC のアドレスが変わる・消える(自作のレイアウトが動かなくなる)、または新しい機能 | 真ん中 | 0.4.1 → 0.5.0 |
| 直しだけ(アドレスも見た目の約束も変わらない) | 最後 | 0.4.0 → 0.4.1 |

互換のない変更は、後でコミットの `BREAKING CHANGE:` とリリースノートに書くので、ここで洗い出しておく(`git diff v<前の版> -- docs/specs/osc-protocol.md` が手がかり)。

## 2. 確かめる(コミットの前)

```bash
cd extension
./gradlew clean generateLayout build installExtension --console=plain -q
grep -l "<failure\|<error" build/test-results/test/*.xml || echo "no failures"
grep -rn "System\.out\|System\.err\|printStackTrace" src/main src/touchosc   # GenerateLayout の usage と "wrote" は正しい出力
```

- `generateLayout` を入れるのは、`committedLayoutIsUpToDate` のテストが、コミットした `.tosc` と今のコードの出力を比べるため。
- 公開のリポジトリなので、LAN の IP(説明用の `192.0.2.x` は可)、PC のフォルダ、メールアドレスが混ざっていないか grep で見る。
- 失敗したら止めて、直してからやり直す。テストを消して通さない。

## 3. コミットを分ける

機能のコミットと、版を上げるだけのコミットを分ける。版のコミットの差分が 3 行だけになり、後から履歴を追いやすい。

1. **機能のコミット**(まだならば): 版の番号は前のまま。conventional commits(`feat:`・`fix:` など)で、互換のない変更があれば本文の最後に `BREAKING CHANGE: ...` を書く。
2. **版のコミット** `chore: release X.Y.Z` で、次の 3 か所だけを変える:
   - `extension/src/main/java/dev/bitwigosc/BitwigOscDefinition.java` の `VERSION`
   - `README.md` と `README.ja.md` の起動の文字の行(`BitwigOSC X.Y.Z: listening on ...`)

README に機能の変更と版の変更が両方入っているときは、版の行だけを一度前の番号に戻して機能のコミットを作り、それから版を上げてコミットする(Git Bash では `git add -p` の対話が使えないため)。

コミットの本文の最後には、そのときの system の指示にある `Co-Authored-By:` の行を付ける。

## 4. HEAD から作り直す

```bash
cd extension && ./gradlew clean generateLayout build installExtension --console=plain -q && cd ..
git status --short   # 何も出なければ、付けるファイルがタグのコードと一致している
```

途中で版の行を戻したりしたので、付けるファイルは必ずコミットの後に作り直す。`git status` に `touchosc/BitwigOSC.tosc` が出たら、コミットとずれているので止める。

## 5. 公開する

```bash
git push origin main
git tag -a vX.Y.Z -m "BitwigOSC X.Y.Z"
git push origin vX.Y.Z
gh release create vX.Y.Z \
  extension/build/extension/BitwigOSC.bwextension \
  touchosc/BitwigOSC.tosc \
  --title "BitwigOSC X.Y.Z" --notes-file <ノートのファイル>
```

ノートのファイルは scratchpad などリポジトリの外に作る。

### リリースノート(英語。これまでと同じ形)

```markdown
<1 行の要約>

- **<変わったこと>**: <利用者から見た説明>
- ...

**Breaking change** (only if you made your own TouchOSC layout):   ← あるときだけ
- `<前のアドレス>` is now `<新しいアドレス>`.

Assets:
- `BitwigOSC.bwextension`: put it in `Documents/Bitwig Studio/Extensions/`, replacing the old file.
- `BitwigOSC.tosc`: <open it in TouchOSC, replacing the old layout. | unchanged from X.Y.Z.>

<system の指示にある PR・リリース用の署名の行>
```

`.tosc` が前の版から変わったかは `git diff --quiet v<前の版> HEAD -- touchosc/BitwigOSC.tosc && echo unchanged` で分かる。

## 6. 後片付けと報告

- PLAN の変更履歴に足す: `docs/plans/extension__src__main__java__dev__bitwigosc__bitwigoscdefinition_java.md`(版と上げた理由)、`docs/plans/readme_md.md`。`docs/plans/` は .gitignore にあり、公開しない。
- 報告には、リリースの URL、2 つのコミット、タグ、付けたファイル、互換のない変更、まだ iPad で確かめていないこと、を書く。
- PC の Bitwig にも同じ版が入っている(`installExtension`)。利用者には、Bitwig でコントローラーを入れ直し、TouchOSC で `.tosc` を開き直すよう伝える。
