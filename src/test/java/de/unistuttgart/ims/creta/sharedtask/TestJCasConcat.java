package de.unistuttgart.ims.creta.sharedtask;

import static org.junit.Assert.assertEquals;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.AnnotationFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.unistuttgart.ims.creta.sharedtask.iaa.type.Seg;

public class TestJCasConcat {
	@Test
	public void testJCasConcat1() throws UIMAException {
		JCas[] jcas = new JCas[] { JCasFactory.createJCas(), JCasFactory.createJCas() };

		jcas[0].setDocumentText("One Two Three Four Five");
		jcas[1].setDocumentText("Eleven Twelve Thirteen Fourteen");

		AnnotationFactory.createAnnotation(jcas[0], 0, 3, Seg.class).setId("One");
		AnnotationFactory.createAnnotation(jcas[0], 4, 7, Seg.class).setId("Two");
		AnnotationFactory.createAnnotation(jcas[0], 14, 18, Seg.class).setId("bla3");
		AnnotationFactory.createAnnotation(jcas[1], 0, 6, Seg.class).setId("Eleven");

		AnnotationFactory.createAnnotation(jcas[1], 10, 10, Seg.class).setId("Empty");

		JCasConcat concat = new JCasConcat();
		JCas newJCas = concat.concat("", jcas);

		assertEquals(5, JCasUtil.select(newJCas, Seg.class).size());

		Seg s;
		s = JCasUtil.selectByIndex(newJCas, Seg.class, 0);
		assertEquals(0, s.getBegin());
		assertEquals(3, s.getEnd());
		assertEquals("One", s.getCoveredText());

		s = JCasUtil.selectByIndex(newJCas, Seg.class, 1);
		assertEquals(4, s.getBegin());
		assertEquals(7, s.getEnd());
		assertEquals("Two", s.getCoveredText());

		s = JCasUtil.selectByIndex(newJCas, Seg.class, 2);
		assertEquals(14, s.getBegin());
		assertEquals(18, s.getEnd());
		assertEquals("Four", s.getCoveredText());

		s = JCasUtil.selectByIndex(newJCas, Seg.class, 3);
		assertEquals(23, s.getBegin());
		assertEquals(29, s.getEnd());
		assertEquals("Eleven", s.getCoveredText());

		s = JCasUtil.selectByIndex(newJCas, Seg.class, 4);
		assertEquals(33, s.getBegin());
		assertEquals(33, s.getEnd());
		assertEquals("", s.getCoveredText());

	}
}
