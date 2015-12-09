COMPILER = javac
SOURCE = RUBTClient.java
ARGS = Phase2.torrent downloadedFile.mov
all:
        $(COMPILER) $(SOURCE)
run:
        $(COMPILER) $(SOURCE)
        java RUBTClient $(ARGS)

gui:
        javac *.java
        java BitTorrentView

cleanClass:
        rm *.class
cleanDownload:
        rm *.mov
        rm *.txt

cleanAll:
        rm *.class
        rm *.mov
        rm *.txt
