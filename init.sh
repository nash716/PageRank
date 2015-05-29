#!/bin/sh

mkdir data

cd data

curl -O http://dumps.wikimedia.org/jawiki/20150512/jawiki-20150512-page.sql.gz
curl -O http://dumps.wikimedia.org/jawiki/20150512/jawiki-20150512-pagelinks.sql.gz
curl -O http://dumps.wikimedia.org/jawiki/20150512/jawiki-20150512-categorylinks.sql.gz

gunzip jawiki-20150512-page.sql.gz
gunzip jawiki-20150512-pagelinks.sql.gz
gunzip jawiki-20150512-categorylinks.sql.gz

cd ../
