/**
 * 
 */
package ru.kfu.itis.cll.uima.eval.matching;

import org.apache.uima.cas.text.AnnotationFS;

/**
 * @author Rinat Gareev
 * 
 */
public class BoundaryMatcher implements Matcher<AnnotationFS> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean match(AnnotationFS ref, AnnotationFS cand) {
		return ref.getBegin() == cand.getBegin() && ref.getEnd() == cand.getEnd();
	}

}