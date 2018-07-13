package de.unistuttgart.ims.creta.sharedtask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Multimaps;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
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
	MutableMap<String, String> propertiesMap = Maps.mutable.empty();

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

			StringBuilder b = new StringBuilder();
			Elements propertyElements = e.select("f");
			for (int i = 0; i < propertyElements.size(); i++) {
				Element pElement = propertyElements.get(i);
				String name = pElement.attr("name");
				if (!name.startsWith("catma")) {
					b.append('+');
					b.append(name);
					if (pElement.hasText()) {
						b.append("=");
						b.append(pElement.text());
					}
				}
			}
			propertiesMap.put(e.attr("xml:id"), b.toString());
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

		SimplePipeline.runPipeline(jcas, AnalysisEngineFactory.createEngineDescription(BreakIteratorSegmenter.class,
				BreakIteratorSegmenter.PARAM_WRITE_SENTENCE, false));
		int tokenNumber = 0;
		for (Token token : JCasUtil.select(jcas, Token.class)) {
			token.setId(String.valueOf(tokenNumber++));
		}

		Map<Seg, Collection<Token>> tokenIndex = JCasUtil.indexCovered(jcas, Seg.class, Token.class);
		int counter = 0;
		try (CSVPrinter p = new CSVPrinter(getAppendable(), CSVFormat.DEFAULT)) {
			for (String id : annoMap.keySet()) {
				Seg first = annoMap.get(id).getFirst();
				Seg last = annoMap.get(id).getLast();
				int begin = Integer.parseInt(getFirstToken(tokenIndex.get(first)).getId());
				int end = Integer.parseInt(getLastToken(tokenIndex.get(last)).getId());
				p.printRecord(getAnnotatorId().substring(0, 1) + counter++, getAnnotatorId(),
						fsDeclMap.get(fsMap.get(id)) + (propertiesMap.containsKey(id) ? propertiesMap.get(id) : ""),
						null, begin, end);
			}
		}
	}

	private Token getFirstToken(Collection<Token> coll) {
		Token firstToken = null;
		for (Token token : coll) {
			if (firstToken == null || token.getBegin() < firstToken.getBegin())
				firstToken = token;
		}
		return firstToken;
	}

	private Token getLastToken(Collection<Token> coll) {
		Token lastToken = null;
		for (Token token : coll) {
			if (lastToken == null || token.getEnd() > lastToken.getEnd())
				lastToken = token;
		}
		return lastToken;
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
