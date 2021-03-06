package ru.kfu.itis.issst.corpus.statistics.cpe;

import com.google.common.collect.Sets;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.kfu.itis.issst.corpus.statistics.dao.corpus.XmiFileTreeCorpusDAO;
import ru.kfu.itis.issst.uima.segmentation.SentenceSplitterAPI;
import ru.kfu.itis.issst.uima.tokenizer.TokenizerAPI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.junit.Assert.assertEquals;

public class UnitsDAOWriterTest {
	String corpusPathString = Thread.currentThread().getContextClassLoader()
			.getResource("corpus_example").getPath();

	Set<String> unitTypes = Sets
			.newHashSet("ru.kfu.cll.uima.tokenizer.fstype.W");
	Set<String> classTypes = Sets.newHashSet("ru.kfu.itis.issst.evex.Person",
			"ru.kfu.itis.issst.evex.Organization",
			"ru.kfu.itis.issst.evex.Weapon");
	ExternalResourceDescription daoDesc;
	TypeSystemDescription tsd;
	CollectionReader reader;
	AnalysisEngine tokenizerSentenceSplitter;
	AnalysisEngine unitAnnotator;
	AnalysisEngine unitClassifier;
	AnalysisEngine unitsDAOWriter;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();
	private File unitsTSV;

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
		reader = CollectionReaderFactory.createReader(
                CorpusDAOCollectionReader.class, tsd,
                CorpusDAOCollectionReader.CORPUS_DAO_KEY, daoDesc);
		CAS aCAS = CasCreationUtils.createCas(tsd, null, null, null);
		reader.typeSystemInit(aCAS.getTypeSystem());
		tokenizerSentenceSplitter = AnalysisEngineFactory
				.createEngine(Unitizer.createTokenizerSentenceSplitterAED());
		unitAnnotator = AnalysisEngineFactory.createEngine(
                UnitAnnotator.class, UnitAnnotator.PARAM_UNIT_TYPE_NAMES,
                unitTypes);
		unitClassifier = AnalysisEngineFactory.createEngine(
                UnitClassifier.class, UnitClassifier.PARAM_CLASS_TYPE_NAMES,
                classTypes);

		unitsTSV = tempFolder.newFile();
		unitsDAOWriter = AnalysisEngineFactory.createEngine(
                UnitsDAOWriter.class, UnitsDAOWriter.UNITS_TSV_PATH,
                unitsTSV.getPath());
	}

	@Test
	public void test() throws CASRuntimeException, UIMAException, IOException {
		SimplePipeline.runPipeline(reader, tokenizerSentenceSplitter,
				unitAnnotator, unitClassifier, unitsDAOWriter);
		Set<String> searchedStrings = Sets.newHashSet(
				"65801.txt\t545\t549\t1\tru.kfu.itis.issst.evex.Organization",
				"62007.txt\t1044\t1048\t1\tru.kfu.itis.issst.evex.Person",
				"62007.txt\t1044\t1048\t5\tru.kfu.itis.issst.evex.Person",
				"65801.txt\t0\t3\t1\tnull");
		int founded = 0;
		BufferedReader inputStream = null;
		try {
			inputStream = new BufferedReader(new FileReader(unitsTSV));
			String l;
			while ((l = inputStream.readLine()) != null) {
				if (searchedStrings.contains(l)) {
					++founded;
				}
			}
		} finally {
			closeQuietly(inputStream);
		}
		assertEquals(searchedStrings.size(), founded);

	}

}
