#!/bin/sh

# Important! Use the new client: https://github.com/transifex/cli/
# The old one [https://github.com/transifex/transifex-client/] which is still packaged with Ubuntu 22.10 doesn't work anymore since Nov 2022

MYDIR=`dirname $0`/..
cd $MYDIR
tx pull --use-git-timestamps -a --minimum-perc 10
