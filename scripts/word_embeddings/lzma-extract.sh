set -x
#set -e

for lang in ca cs de en es ja zh
do
    xz -d -c $lang\_wiki\_text.tar.lzma | tar -x
done
