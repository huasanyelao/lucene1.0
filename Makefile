all:
	$(MAKE) -C com/lucene -w all

jar:
	$(MAKE) -C com/lucene -w jar

release:
	$(MAKE) -C com/lucene -w release

clean:
	$(MAKE) -C com/lucene -w real_clean
