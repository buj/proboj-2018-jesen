JC = javac
JFLAGS = -cp ..

.SUFFIXES: .java .class

.java.class:
	$(JC) $(JFLAGS) $*.java

SOURCES = *.java
SOURCES_LIST = $(wildcard $(SOURCES))

MAIN_PRODUCTS = $(SOURCES_LIST:.java=.class)
ALL_PRODUCTS = $(SOURCES:.java=.class)


all: compile pack

compile: $(MAIN_PRODUCTS)

pack:
	cd .. && jar -cvfm mapedit/mapedit.jar mapedit/Manifest.txt $(addprefix mapedit/, $(ALL_PRODUCTS)) server/game/map/*.class server/game/units/*.class server/game/Constants.class

clean:
	rm -f $(ALL_PRODUCTS) mapedit.jar
