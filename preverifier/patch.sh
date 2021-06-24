#!/bin/sh
while getopts r flag; do
	case $flag in
		r) 
		  shift $(( OPTIND - 1 ))
		  ./restore.sh $1;;
	esac
done
FILE=$1

if [ -f "$FILE"_backup.class ] 
then
	while true; do
		read -e -p ""$FILE"_backup already exists, will you overwrite this backup? (y/n) " yn
		case $yn in
			y)	cp $FILE.class $1_backup.class;
				echo "$FILE"_backup.class overwritten;break;;
			n) echo Backup unchanged;break;;
			*) echo "Please respond with y or n";;
		esac
	done
else 
	cp $FILE.class $1_backup.class;
fi

#javac $1.java
echo **Original Output**
java $FILE
#java --add-modules java.base --add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED --add-exports java.base/jdk.internal.org.objectweb.asm.util=ALL-UNNAMED --add-exports java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED HelloWorldPatcher.java $FILE
java --add-modules java.base --add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED --add-exports java.base/jdk.internal.org.objectweb.asm.util=ALL-UNNAMED --add-exports java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED Preverifier.java $FILE
echo **Patched Output**
java $FILE
