/**
 * 
 */
package ru.ksu.niimm.cll.uima.morph.ruscorpora;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.bindExternalResource;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createDependency;
import static ru.ksu.niimm.cll.uima.morph.ruscorpora.DictionaryAligningTagMapper2.RESOURCE_KEY_MORPH_DICTIONARY;

import java.io.File;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;

import ru.kfu.itis.cll.uima.annotator.AnnotationRemover;
import ru.kfu.itis.cll.uima.consumer.XmiWriter;
import ru.kfu.itis.cll.uima.util.Slf4jLoggerImpl;
import ru.kfu.itis.issst.uima.morph.dictionary.MorphDictionaryAPI;
import ru.kfu.itis.issst.uima.morph.dictionary.MorphDictionaryAPIFactory;
import ru.kfu.itis.issst.uima.morph.dictionary.resource.MorphDictionaryHolder;
import ru.kfu.itis.issst.uima.postagger.PosTaggerAPI;
import ru.kfu.itis.issst.uima.segmentation.SentenceSplitterAPI;
import ru.kfu.itis.issst.uima.tokenizer.InitialTokenizer;
import ru.kfu.itis.issst.uima.tokenizer.TokenizerAPI;
import ru.ksu.niimm.cll.uima.morph.opencorpora.OpencorporaMorphDictionaryAPI;
import ru.ksu.niimm.cll.uima.morph.util.NonTokenizedSpan;
import ru.ksu.niimm.cll.uima.morph.util.NonTokenizedSpanAnnotator;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * @author Rinat Gareev (Kazan Federal University)
 * 
 */
public class RusCorporaParserBootstrap {

	public static void main(String[] args) throws Exception {
		// setup logging
		Slf4jLoggerImpl.forceUsingThisImplementation();
		//
		RusCorporaParserBootstrap launcher = new RusCorporaParserBootstrap();
		new JCommander(launcher, args);
		launcher.run();
	}

	@Parameter(names = "--ruscorpora-text-dir", required = true)
	private File ruscorporaTextDir;
	@Parameter(names = { "-o", "--output-dir" }, required = true)
	private File xmiOutputDir;
	@Parameter(names = "--enable-dictionary-aligning")
	private boolean enableDictionaryAligning;

	private RusCorporaParserBootstrap() {
	}

	private void run() throws Exception {
		CollectionReaderDescription colReaderDesc;
		{
			TypeSystemDescription tsDesc = TypeSystemDescriptionFactory
					.createTypeSystemDescription(
							"ru.kfu.itis.cll.uima.commons.Commons-TypeSystem",
							TokenizerAPI.TYPESYSTEM_TOKENIZER,
							SentenceSplitterAPI.TYPESYSTEM_SENTENCES,
							PosTaggerAPI.TYPESYSTEM_POSTAGGER);
			//
			if (!enableDictionaryAligning) {
				colReaderDesc = CollectionReaderFactory.createReaderDescription(
						RusCorporaCollectionReader.class,
						tsDesc,
						RusCorporaCollectionReader.PARAM_INPUT_DIR, ruscorporaTextDir.getPath());
			} else {
				File daLogFile = new File(xmiOutputDir, "dict-aligning2.log");
				colReaderDesc = CollectionReaderFactory.createReaderDescription(
						RusCorporaCollectionReader.class,
						tsDesc,
						RusCorporaCollectionReader.PARAM_INPUT_DIR, ruscorporaTextDir.getPath(),
						RusCorporaCollectionReader.PARAM_TAG_MAPPER_CLASS,
						DictionaryAligningTagMapper2.class,
						DictionaryAligningTagMapper2.PARAM_OUT_FILE, daLogFile.getPath());
				MorphDictionaryAPI dictAPI = MorphDictionaryAPIFactory.getMorphDictionaryAPI();
				if (!(dictAPI instanceof OpencorporaMorphDictionaryAPI)) {
					throw new UnsupportedOperationException(String.format(
							"Doesn't work with " + dictAPI.getClass().getName()));
				}
				ExternalResourceDescription morphDictDesc = dictAPI
						.getResourceDescriptionForCachedInstance();
				createDependency(colReaderDesc,
						RESOURCE_KEY_MORPH_DICTIONARY,
						MorphDictionaryHolder.class);
				bindExternalResource(colReaderDesc, RESOURCE_KEY_MORPH_DICTIONARY, morphDictDesc);
			}
		}
		// 
		AnalysisEngineDescription xmiWriterDesc = XmiWriter.createDescription(xmiOutputDir, true);
		// make NonTokenizedSpanAnnotator
		AnalysisEngineDescription ntsAnnotatorDesc;
		{
			TypeSystemDescription tsDesc = TypeSystemDescriptionFactory
					.createTypeSystemDescription("ru.ksu.niimm.cll.uima.morph.util.ts-util");
			ntsAnnotatorDesc = createEngineDescription(NonTokenizedSpanAnnotator.class, tsDesc);
		}
		// TODO can we use tokenizer through TokenizerAPI ?
		// make InitialTokenizer for NonTokenizedSpans
		AnalysisEngineDescription tokenizerDesc = createEngineDescription(
				InitialTokenizer.class,
				TokenizerAPI.PARAM_SPAN_TYPE, NonTokenizedSpan.class.getName());
		// make AnnotationRemovers
		AnalysisEngineDescription scaffoldRemover = createEngineDescription(
				AnnotationRemover.class,
				AnnotationRemover.PARAM_NAMESPACES_TO_REMOVE,
				new String[] { "ru.ksu.niimm.cll.uima.morph.util" });
		AnalysisEngineDescription specialWTokenRemover = SpecialWTokenRemover.createDescription();
		//
		SimplePipeline.runPipeline(colReaderDesc,
				ntsAnnotatorDesc, tokenizerDesc,
				scaffoldRemover, specialWTokenRemover,
				xmiWriterDesc);
	}
}