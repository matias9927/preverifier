#!/bin/sh
# Restore class file from backup
echo Restoring $1...
if [ -f "$1"_backup.class ]
then
	rm $1.class
mv $1_backup.class $1.class
else
	echo $1 backup not found
fi
