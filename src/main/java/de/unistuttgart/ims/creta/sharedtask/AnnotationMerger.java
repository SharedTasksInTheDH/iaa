package de.unistuttgart.ims.creta.sharedtask;

import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unistuttgart.ims.creta.sharedtask.iaa.type.CatmaAnnotation;

public class AnnotationMerger extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		Annotation beginAnno = new Annotation(jcas);
		beginAnno.setBegin(0);
		beginAnno.setEnd(0);
		beginAnno.addToIndexes();
		try {
			CatmaAnnotation ca = JCasUtil.select(jcas, CatmaAnnotation.class).iterator().next();
			CatmaAnnotation next;
			while (ca != null) {
				List<CatmaAnnotation> followers = JCasUtil.selectFollowing(CatmaAnnotation.class, ca, 1);
				if (!followers.isEmpty()) {
					next = followers.get(0);
					List<Token> tokens = JCasUtil.selectBetween(Token.class, ca, next);
					if (tokens.isEmpty() && ca.getProperties().equals(next.getProperties())
							&& ca.getCatmaType().equals(next.getCatmaType())) {
						ca.setEnd(next.getEnd());
						next.removeFromIndexes();
					} else {
						ca = next;
					}

				} else {
					ca = null;
				}
			}
		} catch (

		IndexOutOfBoundsException e) {

		}
	}

}
