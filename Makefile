JAVAFILES=$(shell find src/client -name "*.java") $(shell find src/util -name "*.java") $(shell find src/imd -name "*.java")
GROOVYFILES=$(shell find src/server -name "*.groovy")
LIBS=lib/mail.jar:lib/jmf.jar:lib/jnaerator-0.10-shaded.jar:lib/jna.jar:lib/platform.jar:lib/groovy-2.4.6.jar
LIBS2=lib/mail.jar:lib/jmf.jar:lib/jna.jar:lib/platform.jar
OPTS=-Dorg.lwjgl.opengl.Display.allowSoftwareOpenGL=false
SERVERCMDWIN=java -Xmx500m -Djava.library.path=lib -cp build;lib/groovy-2.4.6.jar;lib/jnaerator-0.10-shaded.jar;lib/jna.jar;lib/platform.jar;lib/mail.jar server.Server

JAVAC=javac -source 7 -target 7 

SERVERCMDLIN=clear&&java -Xmx500m -Djava.library.path=lib -cp build:$(LIBS) server.Server

KEYALIAS=mykey
KEYSTORE=keystore.jks

ENABLEMAIL=enableMail boxs@jeronim.de
SMTPSERVER=canis.upperspace.de
SMTPPORT=587
EMAILADDRESS=boxs@jeronim.de
EMAILPASSWORD=boxs

all: offlineserver dropboxshare compileofficialserver

compileofficialserver:
	@echo "\nCompile official server"
	rm -f src/server/SmtpSend.java
	cp src/SmtpSend.java src/server/SmtpSend.java
	@echo "\nCompile BoXS"
	rm -rf build
	mkdir build
	$(JAVAC) -cp $(LIBS) -d build $(JAVAFILES) src/server/SmtpSend.java
	groovyc -cp $(LIBS2):build -d build $(GROOVYFILES)
	cp src/client/*.png build/client/
	rm -f src/server/SmtpSend.java

compileofflineserver: 
	@echo "\nCompile offline server"
	rm -f src/server/SmtpSend.java
	cp src/SmtpSend.java src/server/SmtpSend.java
	@echo "\nCompile BoXS"
	rm -rf build
	mkdir build
	$(JAVAC) -cp $(LIBS) -d build $(JAVAFILES) src/server/SmtpSend.java
	groovyc -cp $(LIBS2):build -d build $(GROOVYFILES)
	cp src/client/*.png build/client/
	rm -f src/server/SmtpSend.java

parsertest: compileofflineserver
	clear
	java -Xmx500m -Djava.library.path=lib -cp build:$(LIBS) server.ParserTest 

runserver: compileofflineserver
	clear
	$(SERVERCMDLIN) $(ENABLEMAIL)

runclient: createjars
	clear
	#firefox webroot/expsys/index.html?email=0\&realm=default\&host=localhost\&port=58000
	firefox -new-window webroot/expsys/es_1e2s.html?email=0\&realm=default\&host=localhost\&port=58000

.PHONY: pstables
pstables:
	java -Djava.library.path=lib -cp build:$(LIBS) -Xmx512m server.PerfectStrangerMatcher 30 30 3

createkey:
	keytool -genkey -alias $(KEYALIAS) -keyalg RSA -keystore $(KEYSTORE)
	keytool -selfcert 


createjars: compileofflineserver
	@echo "\nCreate jars"
	rm -f webroot/expsys/es.jar
	#// TODO hier wird alles in das Client Package gepackt
	cd build && jar cfm ../webroot/expsys/es.jar ../manifest.txt client util imd 
	jarsigner -keystore $(KEYSTORE) webroot/expsys/es.jar $(KEYALIAS)

offlineserver: createjars 
	@echo "\nBuilding offlineserver"
	rm -rf offlineserver

	mkdir offlineserver
	mkdir offlineserver/server
	mkdir offlineserver/server/export
	mkdir offlineserver/client

	rm -f offlineserver/server/server.jar
	jar -cf offlineserver/server/server.jar -C build .

	cp lib/mail.jar offlineserver/server/mail.jar
	echo "$(SERVERCMDWIN)" > offlineserver/server/server.bat
	echo "$(SERVERCMDLIN)" > offlineserver/server/server.sh

	cp webroot/indexoffline.html offlineserver/client/index.html
	cp -r webroot/expsys offlineserver/client/expsys

	zip -r webroot/general/offlineserver.zip offlineserver
	rm -rf offlineserver

dropboxshare: offlineserver createjars
	@echo "\nSharing via Dropbox"
	mkdir -p export
	cp -r src export
	cp -r build export
	cp -r webroot export
	cp -r lib export
	cp -r pstables export
	echo "$(SERVERCMDWIN)" > export/server.bat
	echo "$(SERVERCMDLIN)" > export/server.sh

clean: 
	rm -rf build 

