# VideoPlayer

[![Latest Release](https://img.shields.io/github/release/TeamKun/VideoPlayer.svg?label=Latest%20Release&style=flat)](https://github.com/TeamKun/VideoPlayer/releases)
![Minecraft 1.16.5](https://img.shields.io/badge/Minecraft-1.16.5-green.svg?style=flat)
![Minecraft 1.15.2](https://img.shields.io/badge/Minecraft-1.15.2-green.svg?style=flat)

## 概要

このModはマイクラ内で動画を見ることができます。

各クライアントが直接YouTube等のリソースに通信を行うため、鯖に負荷がかかりません。

タイミングの同期をサポートしており、**多人数でで同じ時間の動画**を見ることができます。

## 導入

動作にはクライアント、鯖に導入が必要です。

鯖へはMod、または下記のプラグインどちらかをご利用いただけます。

[VideoPlayerPlugin](https://github.com/TeamKun/VideoPlayerPlugin/)を使えば、Paper/Spigot鯖で動かす事ができます。

※ Forge鯖、Paper鯖どちらの場合もクライアント側にModの導入が必要です。

## 使用方法

### コマンド

- /vdisplay
    - create <name>
    - destroy <name>
    - position <name>
        - pos1
        - pos2
        - (margin)
- /vplayer <name>
    - video [url]
        - url なし (/vplayer video) → 表示
        - url あり (/vplayer video "https://～～") → 変更+シーク0
    - play [url]
        - url なし (/vplayer play) → ポーズ解除+シーク0
        - url あり (/vplayer play "https://～～") → 変更+シーク0+ポーズ解除
    - pause [on|off]
    - stop
        - urlクリア
    - seek <sec>
    - time [sec|min|percent+"%"] [sec]
        - 引数なし → 表示(1:20 / 3:00 (45%))
        - sec → 秒数でセット
        - percent + "%" → 割合でセット
