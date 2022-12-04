#!/usr/bin/env fish

set PW_testy ~/PW_testy
set zip_name ja438241.zip
set src ~/pw-sharing-economy/src/

if not test -e $PW_testy
    git clone https://github.com/MIMUW-Inf-2002/PW_testy $PW_testy
end

cd $PW_testy
rm -f $zip_name
cd $src
zip -r $PW_testy/$zip_name .
cd -
bash ./zip_test.sh $zip_name

