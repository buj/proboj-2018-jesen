JC = javac
JFLAGS = -cp ..

.SUFFIXES: .java .class

.java.class:
	$(JC) $(JFLAGS) $*.java

SOURCES = *.java 				\
	communication/*.java 	\
	game/*.java 					\
	game/logic/*.java 		\
	game/map/*.java 			\
	game/units/*.java

SOURCES_LIST = $(wildcard $(SOURCES))

MAIN_PRODUCTS = $(SOURCES_LIST:.java=.class)
ALL_PRODUCTS = $(SOURCES:.java=.class)


all: compile pack

compile: $(MAIN_PRODUCTS)

pack:
	cd .. && jar -cvfm server/server.jar server/Manifest.txt $(addprefix server/, $(ALL_PRODUCTS))

clean:
	rm -f $(ALL_PRODUCTS) server.jar
