#!/bin/bash
SOURCE_FILE="source.txt"
PARAM_FILE="param.txt"

OUTPUT="sizes.txt"
SCALE_FACTORS="scale_factors.txt"
RESULT="mosaic"
PATH_TO_PIC="img"

if [ -f $OUTPUT ] 
then 
    rm $OUTPUT 
fi

if [ ! -d "$PATH_TO_PIC" ]
then
    mkdir $PATH_TO_PIC
fi
    
javac Mosaic.java
java Mosaic

MOSAIC_WIDTH=$(awk 'NR==1' $PARAM_FILE)
BORDER=$(awk 'NR==2' $PARAM_FILE)

while read line
    do
    for IMAGE in $line
        do
        IMG_CHARS=$(identify "$IMAGE")
        SIZE=$(echo "$IMG_CHARS" | awk '{print $3}')
        echo $SIZE >> $OUTPUT
        done
    echo "*" >> $OUTPUT
    done <$SOURCE_FILE    

HALF_BORDER=`expr $BORDER / 2`
BORDER=`expr $HALF_BORDER + $HALF_BORDER`

python mosaic.py $MOSAIC_WIDTH $BORDER

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
    for IMAGE in $line
        do
        SIZE=$(echo "$FACTOR_LINE" | awk '{print $'$INDEX'}')
        convert $IMAGE -resize "$SIZE"% "$PATH_TO_PIC"/pic$INDEX.jpg

        INDEX=`expr $INDEX + 1`
        done
 
    montage "$PATH_TO_PIC"/pic[1-`expr $INDEX - 1`].jpg -mode Concatenate -gravity center -border $HALF_BORDER -bordercolor White -tile x1 "$PATH_TO_PIC"/row$ROW_NUMBER.jpg    
    ROW_NUMBER=`expr $ROW_NUMBER + 1`   
    done <$SOURCE_FILE

montage "$PATH_TO_PIC"/row[1-`expr $ROW_NUMBER - 1`].jpg -tile 1x -geometry +0+0 "$PATH_TO_PIC"/$RESULT.jpg
convert "$PATH_TO_PIC"/$RESULT.jpg -bordercolor White -border $HALF_BORDER "$PATH_TO_PIC"/$RESULT.jpg

rm $SOURCE_FILE
rm $PARAM_FILE 
rm $OUTPUT
rm $SCALE_FACTORS
