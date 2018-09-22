package de.unistuttgart.ims.creta.sharedtask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
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
	File latexFile = null;

	GenericXmlReader<DocumentMetaData> reader;

	String annotatorId;

	List<String> featureStructureTypes;
	List<String> featureTypes;

	transient JCas jcas = null;

	MutableMap<String, String> fsDeclMap = Maps.mutable.empty();
	MutableMap<String, String> fsMap = Maps.mutable.empty();
	/**
	 * This map contains annotation ids and the seg annotations they refer to
	 */
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
			String typeName = e.selectFirst("fsDescr").text();
			// System.err.println("fsDecl: " + typeName + ": " + e.attr("xml:id"));
			if (featureStructureTypes == null || featureStructureTypes.contains(typeName))
				fsDeclMap.put(e.attr("xml:id"), typeName);
		});

		reader.addGlobalRule("fs", (a, e) -> {
			fsMap.put(e.attr("xml:id"), e.attr("type"));
			// System.err.println("fs: " + e.attr("type") + ": " + e.attr("xml:id"));
			StringBuilder b = new StringBuilder();
			Elements propertyElements = e.select("f");
			for (int i = 0; i < propertyElements.size(); i++) {
				Element pElement = propertyElements.get(i);
				String name = pElement.attr("name");
				if (!name.startsWith("catma")) {
					// System.err.println(name);
					if (featureTypes == null || featureTypes.contains(name)) {
						b.append('+');
						b.append(name);
						if (pElement.hasText()) {
							b.append("=");
							b.append(pElement.text());
						}
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
		process0();
		writeCSV();
		writeMarkdown();
		writeLaTeX();
	}

	public void process0() throws UIMAException, FileNotFoundException, IOException {
		jcas = JCasFactory.createJCas();
		reader.read(jcas, new FileInputStream(file));

		SimplePipeline.runPipeline(jcas, AnalysisEngineFactory.createEngineDescription(BreakIteratorSegmenter.class,
				BreakIteratorSegmenter.PARAM_WRITE_SENTENCE, false));
		int tokenNumber = 0;
		for (Token token : JCasUtil.select(jcas, Token.class)) {
			token.setId(String.valueOf(tokenNumber++));
		}
		Map<Seg, Collection<Token>> tokenIndex = JCasUtil.indexCovered(jcas, Seg.class, Token.class);

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
			try {
				while (begin == -1) {
					try {
						Seg first = annoMap.get(id).toList().get(elementToTry);
						begin = Integer.parseInt(getFirstToken(tokenIndex.get(first)).getId());
					} catch (java.lang.IllegalArgumentException e) {

					} finally {
						elementToTry++;
					}

				}
			} catch (java.lang.IndexOutOfBoundsException e) {

			}
			ca.setTokenBegin(begin);

			// find end
			int end = Integer.MAX_VALUE;
			elementToTry = annoMap.get(id).toList().size() - 1;
			try {
				while (end == Integer.MAX_VALUE) {
					try {
						Seg last = annoMap.get(id).toList().get(elementToTry);
						end = Integer.parseInt(getLastToken(tokenIndex.get(last)).getId());
					} catch (java.lang.IllegalArgumentException e) {

					} finally {
						elementToTry--;
					}
				}
			} catch (java.lang.IndexOutOfBoundsException e) {

			}
			ca.setTokenEnd(end);
		}

	}

	public void writeCSV() throws IOException {
		int counter = 0;
		try (CSVPrinter p = new CSVPrinter(getAppendable(), CSVFormat.DEFAULT)) {
			// iterate over CatmaAnnotations
			for (CatmaAnnotation ca : JCasUtil.select(jcas, CatmaAnnotation.class)) {
				String id = ca.getId();
				String type = fsMap.get(id);
				if (ca.getTokenBegin() != -1 && ca.getTokenEnd() != Integer.MAX_VALUE && fsDeclMap.containsKey(type))
					p.printRecord(getAnnotatorId().substring(0, 1) + counter++, getAnnotatorId(),
							fsDeclMap.get(fsMap.get(id)) + (propertiesMap.containsKey(id) ? propertiesMap.get(id) : ""),
							null, ca.getTokenBegin(), ca.getTokenEnd());
			}
		}

	}

	public void writeMarkdown() throws FileNotFoundException, IOException {
		if (jcas != null && markdownFile != null)
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

	public void writeLaTeX() throws FileNotFoundException, IOException {
		if (jcas != null && getLatexFile() != null)
			try (

					FileOutputStream os = new FileOutputStream(getLatexFile())) {
				GenericInlineWriter<CatmaAnnotation> giw = new GenericInlineWriter<CatmaAnnotation>(
						CatmaAnnotation.class);
				List<CatmaAnnotation> annoList = new LinkedList<CatmaAnnotation>();

				giw.setTagFactory(new InlineTagFactory<CatmaAnnotation>() {

					@Override
					public String getBeginTag(CatmaAnnotation anno) {
						if (!annoList.contains(anno))
							annoList.add(anno);
						int idx = annoList.indexOf(anno);
						return "\\colorbox{yellow}{[\\footnotemark[" + idx + "]}\\footnotetext[" + idx + "]{"
								+ fsDeclMap.get(fsMap.get(anno.getId())) + anno.getProperties() + "}";
					}

					@Override
					public String getEndTag(CatmaAnnotation anno) {
						if (!annoList.contains(anno))
							annoList.add(anno);
						int idx = annoList.indexOf(anno);
						return "\\colorbox{yellow}{]\\footnotemark[" + idx + "]}\\footnotetext[" + idx + "]{"
								+ fsDeclMap.get(fsMap.get(anno.getId())) + anno.getProperties() + "}";
					}

					@Override
					public String getEmptyTag(CatmaAnnotation anno) {
						return "\\colorbox{yellow}{[]}";
					}

				});

				OutputStreamWriter osw = new OutputStreamWriter(os);
				StringWriter sw = new StringWriter();
				giw.write(jcas, sw);
				String s = sw.toString();
				s = s.replaceAll("_", "\\\\_");
				s = s.replaceAll("#", "\\\\#");
				s = s.replaceAll("&", "\\\\&");
				s = s.replaceAll("\n", "\n\n");
				osw.write(s);
				osw.flush();
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

	public File getLatexFile() {
		return latexFile;
	}

	public void setLatexFile(File latexFile) {
		this.latexFile = latexFile;
	}

	public List<String> getFeatureStructureTypes() {
		return featureStructureTypes;
	}

	public void setFeatureStructureTypes(List<String> featureStructureTypes) {
		this.featureStructureTypes = featureStructureTypes;
	}

	public List<String> getFeatureTypes() {
		return featureTypes;
	}

	public void setFeatureTypes(List<String> featureTypes) {
		this.featureTypes = featureTypes;
	}
}
