package de.unistuttgart.ims.creta.sharedtask;

import static org.junit.Assert.assertEquals;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.AnnotationFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.unistuttgart.ims.creta.sharedtask.iaa.type.CatmaAnnotation;

public class TestAnnotationMerger {
	CatmaAnnotation[] ca;
	JCas jcas;

	@Before
	public void setUp() throws UIMAException {
		jcas = JCasFactory.createText("The dog bar.");

		SimplePipeline.runPipeline(jcas, AnalysisEngineFactory.createEngineDescription(BreakIteratorSegmenter.class,
				BreakIteratorSegmenter.PARAM_WRITE_SENTENCE, false));

		ca = new CatmaAnnotation[] { AnnotationFactory.createAnnotation(jcas, 0, 3, CatmaAnnotation.class),
				AnnotationFactory.createAnnotation(jcas, 4, 6, CatmaAnnotation.class),
				AnnotationFactory.createAnnotation(jcas, 7, 9, CatmaAnnotation.class) };
	}

	@Test
	public void testMerger1() throws UIMAException {
		ca[0].setProperties("");
		ca[1].setProperties("");
		ca[2].setProperties("");

		ca[0].setCatmaType("c1");
		ca[1].setCatmaType("c1");
		ca[2].setCatmaType("c2");

		SimplePipeline.runPipeline(jcas, AnalysisEngineFactory.createEngineDescription(AnnotationMerger.class));

		assertEquals(2, JCasUtil.select(jcas, CatmaAnnotation.class).size());

		assertEquals(ca[0], JCasUtil.selectByIndex(jcas, CatmaAnnotation.class, 0));
		assertEquals(ca[2], JCasUtil.selectByIndex(jcas, CatmaAnnotation.class, 1));

	}

	@Test
	public void testMerger2() throws UIMAException {
		ca[0].setProperties("");
		ca[1].setProperties("");
		ca[2].setProperties("");

		ca[0].setCatmaType("c1");
		ca[1].setCatmaType("c2");
		ca[2].setCatmaType("c2");

		SimplePipeline.runPipeline(jcas, AnalysisEngineFactory.createEngineDescription(AnnotationMerger.class));

		assertEquals(2, JCasUtil.select(jcas, CatmaAnnotation.class).size());

		assertEquals(ca[0], JCasUtil.selectByIndex(jcas, CatmaAnnotation.class, 0));
		assertEquals(ca[1], JCasUtil.selectByIndex(jcas, CatmaAnnotation.class, 1));

	}

	@Test
	public void testMerger3() throws UIMAException {
		ca[0].setProperties("x");
		ca[1].setProperties("y");
		ca[2].setProperties("z");

		ca[0].setCatmaType("c1");
		ca[1].setCatmaType("c1");
		ca[2].setCatmaType("c1");

		SimplePipeline.runPipeline(jcas, AnalysisEngineFactory.createEngineDescription(AnnotationMerger.class));

		assertEquals(3, JCasUtil.select(jcas, CatmaAnnotation.class).size());

		// assertEquals(ca[0], JCasUtil.selectByIndex(jcas, CatmaAnnotation.class, 0));
		// assertEquals(ca[1], JCasUtil.selectByIndex(jcas, CatmaAnnotation.class, 1));

	}

	@Test
	public void testMerger4() throws UIMAException {
		ca[0].setProperties("x");
		ca[1].setProperties("y");
		ca[2].setProperties("x");

		ca[0].setCatmaType("c1");
		ca[1].setCatmaType("c1");
		ca[2].setCatmaType("c1");

		SimplePipeline.runPipeline(jcas, AnalysisEngineFactory.createEngineDescription(AnnotationMerger.class));

		assertEquals(3, JCasUtil.select(jcas, CatmaAnnotation.class).size());

		// assertEquals(ca[0], JCasUtil.selectByIndex(jcas, CatmaAnnotation.class, 0));
		// assertEquals(ca[1], JCasUtil.selectByIndex(jcas, CatmaAnnotation.class, 1));

	}

	@Test
	public void testMerger5() throws UIMAException {
		ca[0].setProperties("x");
		ca[1].setProperties("x");
		ca[2].setProperties("y");

		ca[0].setCatmaType("c1");
		ca[1].setCatmaType("c1");
		ca[2].setCatmaType("c1");

		SimplePipeline.runPipeline(jcas, AnalysisEngineFactory.createEngineDescription(AnnotationMerger.class));

		assertEquals(2, JCasUtil.select(jcas, CatmaAnnotation.class).size());

		assertEquals(ca[0], JCasUtil.selectByIndex(jcas, CatmaAnnotation.class, 0));
		assertEquals(ca[2], JCasUtil.selectByIndex(jcas, CatmaAnnotation.class, 1));

	}
}
