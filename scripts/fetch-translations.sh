#!/bin/bash

declare -A android
android=([de]=de [fr]=fr [hu]=hu [nl]=nl)

for lang in ${!android[@]}
do
	target=../app/src/main/res/values-${android[$lang]}
	mkdir -p $target
	curl -n "https://www.transifex.com/api/2/project/icsdroid/resource/icsdroid/translation/$lang?file" >$target/strings.xml
done
