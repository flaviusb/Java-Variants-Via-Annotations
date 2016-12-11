Java-Variants-Via-Annotations.jar: src/net/flaviusb/types/variant_encoding/*.java res
	mkdir -p build
	javac -sourcepath ./src -d build src/net/flaviusb/types/variant_encoding/*.java
	cp -R res/* build
	jar cf Java-Variants-Via-Annotations.jar -C build .

clean:
	rm -rf build
	rm Java-Variants-Via-Annotations.jar

all: Java-Variants-Via-Annotations.jar

.PHONY: clean

.DEFAULT_GOAL := all

