#!/bin/bash
TEXT_FILE=$1
COLLAGE_WIDTH=$2
BORDER=$3

OUTPUT="sizes.txt"
PATH_TO_PIC="img/"
SCALE_FACTORS="scale_factors.txt"
RESULT="mosaic"

if [ -f $OUTPUT ] 
then 
    rm $OUTPUT 
fi

while read line
    do
    for WORD in $line
        do
        IMAGE=$PATH_TO_PIC$WORD
        IMG_CHARS=$(identify "$IMAGE")
        SIZE=$(echo "$IMG_CHARS" | awk '{print $3}')
        echo $SIZE >> $OUTPUT
        done
    echo "*" >> $OUTPUT
    done <$TEXT_FILE    

HALF_BORDER=`expr $BORDER / 2`
BORDER=`expr $HALF_BORDER + $HALF_BORDER`
python Mosaic $OUTPUT $COLLAGE_WIDTH $BORDER

SCALE_FACTOR_LIST=( )
INDEX=1
while read line
    do
    SCALE_FACTOR_LIST[$INDEX]=$line
    echo ${SCALE_FACTOR_LIST[$INDEX]}
    INDEX=`expr $INDEX + 1`
    done <$SCALE_FACTORS 

ROW_NUMBER=1
while read line
    do
    FACTOR_LINE=${SCALE_FACTOR_LIST[$ROW_NUMBER]}
    INDEX=1
    for WORD in $line
        do
        SIZE=$(echo "$FACTOR_LINE" | awk '{print $'$INDEX'}')
        convert $PATH_TO_PIC$WORD -resize "$SIZE"% "$PATH_TO_PIC"pic$INDEX.jpg

        INDEX=`expr $INDEX + 1`
        done
 
    montage "$PATH_TO_PIC"pic[1-`expr $INDEX - 1`].jpg -mode Concatenate -gravity center -border $HALF_BORDER -bordercolor White -tile x1 "$PATH_TO_PIC"row$ROW_NUMBER.jpg    
    ROW_NUMBER=`expr $ROW_NUMBER + 1`   
    done <$TEXT_FILE

montage "$PATH_TO_PIC"row[1-`expr $ROW_NUMBER - 1`].jpg -tile 1x -geometry +0+0 "$PATH_TO_PIC"$RESULT.jpg
convert "$PATH_TO_PIC"$RESULT.jpg -bordercolor White -border $HALF_BORDER "$PATH_TO_PIC"$RESULT.jpg
