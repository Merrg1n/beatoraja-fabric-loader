#!/bin/bash
export _JAVA_OPTIONS='-Dsun.java2d.opengl=true -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true -Dswing.defaultlaf=com.sun.java.swing.plaf.gtk.GTKLookAndFeel'
cd "$(dirname "$0")"
exec java -Xms1g -Xmx4g -cp libraries/*;beatoraja-fabric-loader.jar io.github.merrg1n.beatorajafabric.Main
