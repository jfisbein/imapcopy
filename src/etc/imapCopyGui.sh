#!/bin/sh
if [ ! -d "$JAVA_HOME" ] ; then
    JAVA=java
  else
    JAVA=${JAVA_HOME}/bin/java
fi

${JAVA} -cp imapcopy-*.jar com.fisbein.joan.gui.ImapCopyGui
