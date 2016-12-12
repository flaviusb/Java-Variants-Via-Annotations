Java-Variants-Via-Annotations.jar: src/net/flaviusb/types/variant_encoding/*.java res
	mkdir -p build
	javac -sourcepath ./src -d build src/net/flaviusb/types/variant_encoding/*.java
	cp -R res/* build
	jar cf Java-Variants-Via-Annotations.jar -C build .

clean:
	rm -rf build buildt
	rm Java-Variants-Via-Annotations.jar

all: Java-Variants-Via-Annotations.jar

test: Java-Variants-Via-Annotations.jar t/*.java t/test/meteorology/*.java
	mkdir -p buildt
	javac -sourcepath ./t -classpath ./Java-Variants-Via-Annotations.jar -d buildt t/*.java t/test/meteorology/*.java

.PHONY: clean test

.DEFAULT_GOAL := all

