package ru.kfu.itis.issst.corpus.statistics.cpe;

import com.google.common.collect.Sets;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;
import ru.kfu.itis.issst.corpus.statistics.dao.corpus.XmiFileTreeCorpusDAO;
import ru.kfu.itis.issst.uima.segmentation.SentenceSplitterAPI;
import ru.kfu.itis.issst.uima.tokenizer.TokenizerAPI;

import java.io.IOException;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class UnitAnnotatorTest {
	String corpusPathString = Thread.currentThread().getContextClassLoader()
			.getResource("corpus_example").getPath();
	Set<String> unitTypes = Sets
			.newHashSet("ru.kfu.cll.uima.tokenizer.fstype.W");
	ExternalResourceDescription daoDesc;
	TypeSystemDescription tsd;
	CollectionReaderDescription readerDesc;
	AnalysisEngineDescription tokenizerSentenceSplitterDesc;
	AnalysisEngineDescription unitAnnotatorDesc;

	@Before
	public void setUp() throws Exception {
		daoDesc = ExternalResourceFactory.createExternalResourceDescription(
				XmiFileTreeCorpusDAOResource.class, corpusPathString);
		tsd = CasCreationUtils
				.mergeTypeSystems(Sets.newHashSet(
						XmiFileTreeCorpusDAO.getTypeSystem(corpusPathString),
						TypeSystemDescriptionFactory
								.createTypeSystemDescription(),
						TokenizerAPI.getTypeSystemDescription(),
						SentenceSplitterAPI.getTypeSystemDescription()));
		readerDesc = CollectionReaderFactory.createReaderDescription(
                CorpusDAOCollectionReader.class, tsd,
                CorpusDAOCollectionReader.CORPUS_DAO_KEY, daoDesc);
		CAS aCAS = CasCreationUtils.createCas(tsd, null, null, null);
		tokenizerSentenceSplitterDesc = AnalysisEngineFactory
				.createEngineDescription(Unitizer.createTokenizerSentenceSplitterAED());
		unitAnnotatorDesc = AnalysisEngineFactory.createEngineDescription(
                UnitAnnotator.class, UnitAnnotator.PARAM_UNIT_TYPE_NAMES,
                unitTypes);
	}

	@Test
	public void test() throws UIMAException, SAXException,
			CpeDescriptorException, IOException {
		for (JCas jCas : new JCasIterable(readerDesc,
                tokenizerSentenceSplitterDesc, unitAnnotatorDesc)) {
			CAS aCas = jCas.getCas();
			Type unitSourceType = CasUtil.getType(aCas, unitTypes.iterator()
					.next());
			Type unitType = aCas.getTypeSystem().getType(
					UnitAnnotator.UNIT_TYPE_NAME);

			int sourceNumber = CasUtil.select(aCas, unitSourceType).size();
			int unitNumber = CasUtil.select(aCas, unitType).size();
			assertTrue(sourceNumber > 0);
			assertThat(unitNumber, equalTo(sourceNumber));
		}
	}
}
