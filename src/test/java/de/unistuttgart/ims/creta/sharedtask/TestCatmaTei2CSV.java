package de.unistuttgart.ims.creta.sharedtask;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.uima.UIMAException;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.Test;

import de.unistuttgart.ims.creta.sharedtask.iaa.type.CatmaAnnotation;

public class TestCatmaTei2CSV {

	@Test
	public void testSANTA2_o_Buechner() throws UIMAException, FileNotFoundException, IOException {
		CatmaTei2CSV conv = null;
		conv = new CatmaTei2CSV(getClass().getResourceAsStream("/SANTA2_o_Buechner.xml"));
		conv.setFeatureStructureTypes(Lists.immutable.of("level").castToList());
		conv.setFeatureTypes(Lists.immutable.of("degree").castToList());

		assertNotNull(conv);
		conv.process0();

		MutableList<CatmaAnnotation> l = conv.getFilteredCatmaAnnotations();
		assertFalse(l.isEmpty());
		CatmaAnnotation ca;

		ca = l.get(0);
		assertNotNull(ca);

		assertEquals("CATMA_DA623E4D-CF3F-432B-A460-5E0CD9C3108D", ca.getId());
		assertEquals(1082, ca.getTokenBegin());
		assertEquals(1091, ca.getTokenEnd());
		assertEquals("his homeland; he sketched its various local costumes,", ca.getCoveredText());
	}

	@Test
	public void testSANTA4_f_Buechner() throws UIMAException, FileNotFoundException, IOException {
		CatmaTei2CSV conv = null;
		conv = new CatmaTei2CSV(getClass().getResourceAsStream("/SANTA4_f_Buechner.xml"));
		assertNotNull(conv);
		conv.process0();

		MutableList<CatmaAnnotation> l = conv.getFilteredCatmaAnnotations();
		assertFalse(l.isEmpty());
		CatmaAnnotation ca;

		ca = l.get(0);
		assertNotNull(ca);

		conv.setFeatureStructureTypes(Lists.immutable.of("level").castToList());
		conv.setFeatureTypes(Lists.immutable.of("degree").castToList());

	}

}
