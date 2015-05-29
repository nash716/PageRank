# PageRank

Wikipedia のダンプファイルから、Wikipedia 内の各ページの PageRank を求めるツール。  
また、同時に存命人物のうち上位の PageRank をもつ 10 名を表示します。

## 準備

Wikipedia のダンプファイルのダウンロード、解凍を行います。

```
$ ./init.sh
```

## 実行

プログラムは Java で記述されています。コンパイルして実行してください。

```
$ javac Main.java
$ java Main
```

実行結果は `data/result.tsv` に保存され、形式は `PageID \t PageRank \t PageTitle` です。

## 注意

処理には非常に時間がかかります。また、非常にたくさんのメモリが必要です。  
ほぼ例外処理をしていません。例外で落ちた場合は、ファイルが壊れていないか、パーミッションは問題ないかなどを確認してください。

## 実行例

正しく実行できた場合の出力例です。

```
Step 1: Parsing `page` table
Step 1: Parsed 2527687 objects.
Step 1: DONE, 8983ms
Step 2: Parsing `pagelinks` table
Step 2: Parsed 99012818 objects.
Step 2: DONE, 139946ms
Step 3: Calculating PageRank
Step 3: Loop 1/100
Step 3: Loop 1 DONE, 16451ms
Step 3: Loop 2/100
Step 3: Loop 2 DONE, 17404ms
Step 3: Loop 3/100
Step 3: Loop 3 DONE, 15487ms
Step 3: Loop 4/100
Step 3: Loop 4 DONE, 15761ms
Step 3: Loop 5/100
Step 3: Loop 5 DONE, 16713ms
Step 3: Loop 6/100
Step 3: Loop 6 DONE, 15823ms
Step 3: Loop 7/100
Step 3: Loop 7 Skipped, change = 4.994938065226885E-5
Step 3: Loop 8/100
Step 3: Loop 8 Skipped, change = 4.994938065226885E-5
Step 3: Loop 9/100
Step 3: Loop 9 Skipped, change = 4.994938065226885E-5
(snip)
Step 3: Loop 98/100
Step 3: Loop 98 Skipped, change = 4.994938065226885E-5
Step 3: Loop 99/100
Step 3: Loop 99 Skipped, change = 4.994938065226885E-5
Step 3: Loop 100/100
Step 3: Loop 100 Skipped, change = 4.994938065226885E-5
Step 3: DONE, 97663ms
Step 4: Parsing `categorylinks` table
Step 4: Parsed 5904181 objects. Found 152037 people.
Step 4: DONE, 18285ms
Step 5: Sorting
 1: (1位の人の PageRank), (1位の人の名前)
(snip)
10: (10位の人の PageRank), (10位の人の名前)
Step 5: DONE, 10005ms
```