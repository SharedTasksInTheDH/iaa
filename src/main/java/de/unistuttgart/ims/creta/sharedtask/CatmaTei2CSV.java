package de.unistuttgart.ims.creta.sharedtask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Multimaps;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.unistuttgart.ims.creta.sharedtask.iaa.type.CatmaAnnotation;
import de.unistuttgart.ims.creta.sharedtask.iaa.type.Seg;
import de.unistuttgart.ims.uima.io.xml.GenericInlineWriter;
import de.unistuttgart.ims.uima.io.xml.GenericXmlReader;
import de.unistuttgart.ims.uima.io.xml.InlineTagFactory;

public class CatmaTei2CSV {
	File file;

	Appendable appendable;
	File markdownFile = null;

	GenericXmlReader<DocumentMetaData> reader;

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
		this.reader = new GenericXmlReader<DocumentMetaData>(DocumentMetaData.class);
		reader.setPreserveWhitespace(false);
		reader.setTextRootSelector("TEI > text > body");
		reader.addGlobalRule("fsDecl", (a, e) -> {
			fsDeclMap.put(e.attr("xml:id"), e.selectFirst("fsDescr").text());
		});

		reader.addGlobalRule("fs", (a, e) -> {
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
				CatmaAnnotation ca = new CatmaAnnotation(jcas);
				ca.setBegin(annoMap.get(id).toList().getFirst().getBegin());
				ca.setEnd(annoMap.get(id).toList().getLast().getEnd());
				ca.setId(id);
				if (propertiesMap.containsKey(id))
					ca.setProperties(propertiesMap.get(id));

				ca.addToIndexes();

				// find begin
				int begin = -1;
				int elementToTry = 0;
				while (begin == -1) {
					Seg first = annoMap.get(id).toList().get(elementToTry++);
					try {
						begin = Integer.parseInt(getFirstToken(tokenIndex.get(first)).getId());
					} catch (java.lang.IllegalArgumentException e) {

					}
				}

				// find end
				int end = Integer.MAX_VALUE;
				elementToTry = annoMap.get(id).toList().size() - 1;
				while (end == Integer.MAX_VALUE) {
					Seg last = annoMap.get(id).toList().get(elementToTry--);
					try {
						end = Integer.parseInt(getLastToken(tokenIndex.get(last)).getId());
					} catch (java.lang.IllegalArgumentException e) {

					}
				}

				p.printRecord(getAnnotatorId().substring(0, 1) + counter++, getAnnotatorId(),
						fsDeclMap.get(fsMap.get(id)) + (propertiesMap.containsKey(id) ? propertiesMap.get(id) : ""),
						null, begin, end);
			}
		}
		if (markdownFile != null)
			try (

					FileOutputStream os = new FileOutputStream(markdownFile)) {
				GenericInlineWriter<CatmaAnnotation> giw = new GenericInlineWriter<CatmaAnnotation>(
						CatmaAnnotation.class);
				giw.setTagFactory(new InlineTagFactory<CatmaAnnotation>() {

					@Override
					public String getBeginTag(CatmaAnnotation anno) {
						return "[";
					}

					@Override
					public String getEndTag(CatmaAnnotation anno) {
						return "]*" + fsDeclMap.get(fsMap.get(anno.getId())) + anno.getProperties() + "*";
					}

					@Override
					public String getEmptyTag(CatmaAnnotation anno) {
						return "[]";
					}

				});
				giw.write(jcas, os);
			}
	}

	private Token getFirstToken(Collection<Token> coll) {
		if (coll.isEmpty())
			throw new IllegalArgumentException();

		Token firstToken = null;
		for (Token token : coll) {
			if (firstToken == null || token.getBegin() < firstToken.getBegin())
				firstToken = token;
		}
		return firstToken;
	}

	private Token getLastToken(Collection<Token> coll) {
		if (coll.isEmpty())
			throw new IllegalArgumentException();

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

	public File getMarkdownFile() {
		return markdownFile;
	}

	public void setMarkdownFile(File markdownFile) {
		this.markdownFile = markdownFile;
	}
}
