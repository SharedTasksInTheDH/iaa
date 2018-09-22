package de.unistuttgart.ims.creta.sharedtask;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.CasCopier;

public class JCasConcat {
	public JCas concat(String sep, JCas... jcass) throws UIMAException {
		System.err.println("Concatenating " + jcass.length + " JCas objects.");
		JCas jcas = JCasFactory.createJCas();

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < jcass.length; i++) {
			if (i != 0)
				b.append(sep);
			b.append(jcass[i].getDocumentText());
		}

		jcas.setDocumentText(b.toString());

		int offset = 0;
		for (int i = 0; i < jcass.length; i++) {
			if (i != 0)
				offset += sep.length();
			System.err.println("   copying feature structures");
			copy(jcass[i], jcas, offset);
			offset += jcass[i].getDocumentText().length();
		}

		return jcas;
	}

	protected void copy(JCas src, JCas tgt, int offset) {
		CasCopier cc = new CasCopier(src.getCas(), tgt.getCas());

		for (Annotation srcAnno : JCasUtil.select(src, Annotation.class)) {
			FeatureStructure tgtFS = cc.copyFs(srcAnno);
			if (tgtFS instanceof Annotation) {
				Annotation tgtAnno = (Annotation) tgtFS;
				tgtAnno.setBegin(tgtAnno.getBegin() + offset);
				tgtAnno.setEnd(tgtAnno.getEnd() + offset);

			}
			tgt.addFsToIndexes(tgtFS);
		}
	}
}
