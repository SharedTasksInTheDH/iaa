# README

The code in this package can be used to convert the annotations done in CATMA tei into a CSV format. 

The CSV format looks like this:
```
a0,ANNOTATORNAME,narrative,,861,986
a1,ANNOTATORNAME,narrative,,602,676
a2,ANNOTATORNAME,narrative,,677,766
...
```

The columns represent:

- an id for the annotation unit
- the name/id of the annotator
- the category
- unused
- begin offset
- end offset

The offsets are currently character offsets, but they will likely be converted to token offsets at some point.

## Compilation

Easiest done using maven:

```
mvn package
```

## Execute

We provide a small shell script to facilitate execution of the program. 

```
bash catma2csv.sh FILENAME ANNOTATORNAME
```