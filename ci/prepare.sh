#!/bin/bash
if [ "$CI_BUILD_STAGE" == "main" ]
then
    apt-get install tree zip unzip > /dev/null
	./ci/git.sh 2> /dev/null > /dev/null
	export POW_OF_2=false
	java -version
fi
