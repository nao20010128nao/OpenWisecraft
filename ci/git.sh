git config --global user.email "nao20010128@gmail.com"
git config --global user.name "nao20010128nao"
git submodule update --init --recursive
cd wisecraft-i18n
git pull origin master
git checkout master
cd ../statusesLayout
git pull origin master
git checkout master
cd ../MaterialIcons
git pull origin master
git checkout master
cd ../calligraphy
git pull origin master
git checkout master
cd ../psts
git pull origin master
git checkout master
cd ../colorPicker
git pull origin master
git checkout master
cd ../