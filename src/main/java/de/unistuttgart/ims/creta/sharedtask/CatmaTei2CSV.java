package de.unistuttgart.ims.creta.sharedtask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Comparator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Multimaps;

import de.unistuttgart.ims.creta.sharedtask.iaa.type.Seg;
import de.unistuttgart.ims.uima.io.xml.GenericXmlReader;

public class CatmaTei2CSV {
	File file;

	Appendable appendable;

	GenericXmlReader<TOP> reader;

	String annotatorId;

	MutableMap<String, String> fsDeclMap = Maps.mutable.empty();
	MutableMap<String, String> fsMap = Maps.mutable.empty();
	MutableMultimap<String, Seg> annoMap = Multimaps.mutable.sortedSet.with(new Comparator<Seg>() {

		@Override
		public int compare(Seg o1, Seg o2) {
			return Integer.compare(o1.getBegin(), o2.getBegin());
		}
	});

	public CatmaTei2CSV(File file) {
		this.file = file;
		this.reader = new GenericXmlReader<TOP>(TOP.class);
		reader.setPreserveWhitespace(false);
		reader.setTextRootSelector(null);
		reader.addRule("fsDecl", Annotation.class, (a, e) -> {
			fsDeclMap.put(e.attr("xml:id"), e.selectFirst("fsDescr").text());
		});

		reader.addRule("fs", Annotation.class, (a, e) -> {
			fsMap.put(e.attr("xml:id"), e.attr("type"));
		});

		reader.addRule("seg", Seg.class, (seg, element) -> {
			String anaAttribute = element.attr("ana");
			for (String ana : anaAttribute.split(" ")) {
				annoMap.put(ana.substring(1), seg);
			}
		});
	}

	public void process() throws UIMAException, FileNotFoundException, IOException {
		JCas jcas = JCasFactory.createJCas();
		reader.read(jcas, new FileInputStream(file));

		int counter = 0;
		try (CSVPrinter p = new CSVPrinter(getAppendable(), CSVFormat.DEFAULT)) {
			for (String id : annoMap.keySet()) {
				Seg first = annoMap.get(id).getFirst();
				Seg last = annoMap.get(id).getLast();
				p.printRecord("a" + counter++, getAnnotatorId(), fsDeclMap.get(fsMap.get(id)), null, first.getBegin(),
						last.getEnd());
			}
		}
	}

	public Appendable getAppendable() {
		return appendable;
	}

	public void setAppendable(Appendable appendable) {
		this.appendable = appendable;
	}

	public String getAnnotatorId() {
		return annotatorId;
	}

	public void setAnnotatorId(String annotatorId) {
		this.annotatorId = annotatorId;
	}
}
