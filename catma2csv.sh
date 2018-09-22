#!/bin/bash

MAINCLASS="de.unistuttgart.ims.creta.sharedtask.Convert"

mvn -q exec:java -Dexec.mainClass="$MAINCLASS" -Dexec.args="--input $1 --annotatorId $2 --output $3 --categories $4 --features $5"