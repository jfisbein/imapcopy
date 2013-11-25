#!/bin/sh
if [ ! -d "$JAVA_HOME" ] ; then
    JAVA=java
  else
    JAVA=${JAVA_HOME}/bin/java
fi

${JAVA} -cp log4j-1.2.15.jar:mail.jar:activation.jar:.:imapCopy.jar:forms-1.1.0.jar com.fisbein.joan.model.ImapCopier $*
