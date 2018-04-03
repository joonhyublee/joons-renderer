#!/bin/sh
mem=1G
java -Xmx$mem -server -jar sunflow.jar $*
