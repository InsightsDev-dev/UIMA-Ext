/**
 * 
 */
package ru.kfu.itis.issst.uima.morph.hunpos;

import static com.google.common.collect.Sets.newHashSet;
import static de.tudarmstadt.ukp.dkpro.lab.storage.StorageService.AccessMode.READONLY;
import static de.tudarmstadt.ukp.dkpro.lab.storage.StorageService.AccessMode.READWRITE;
import static org.uimafit.factory.AnalysisEngineFactory.createAggregateDescription;
import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.uimafit.factory.ExternalResourceFactory.bindResource;
import static org.uimafit.factory.ExternalResourceFactory.createExternalResourceDescription;
import static org.uimafit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static ru.kfu.itis.issst.uima.morph.hunpos.DefaultHunposExecutableResolver.trainerResolver;
import static ru.ksu.niimm.cll.uima.morph.lab.LabConstants.DISCRIMINATOR_FOLD;
import static ru.ksu.niimm.cll.uima.morph.lab.LabConstants.DISCRIMINATOR_POS_CATEGORIES;
import static ru.ksu.niimm.cll.uima.morph.lab.LabConstants.DISCRIMINATOR_SOURCE_CORPUS_DIR;
import static ru.ksu.niimm.cll.uima.morph.lab.LabConstants.KEY_CORPUS;
import static ru.ksu.niimm.cll.uima.morph.lab.LabConstants.KEY_MODEL_DIR;
import static ru.ksu.niimm.cll.uima.morph.lab.LabConstants.KEY_OUTPUT_DIR;
import static ru.ksu.niimm.cll.uima.morph.lab.LabConstants.KEY_TRAINING_DIR;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.annolab.tt4j.ExecutableResolver;
import org.annolab.tt4j.PlatformDetector;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.uimafit.factory.ExternalResourceFactory;

import ru.kfu.itis.cll.uima.io.ProcessIOUtils;
import ru.kfu.itis.cll.uima.io.StreamGobblerBase;
import ru.kfu.itis.cll.uima.util.Slf4jLoggerImpl;
import ru.kfu.itis.issst.uima.morph.commons.DictionaryBasedTagMapper;
import ru.ksu.niimm.cll.uima.morph.lab.AnalysisTaskBase;
import ru.ksu.niimm.cll.uima.morph.lab.CorpusPartitioningTask;
import ru.ksu.niimm.cll.uima.morph.lab.CorpusPreprocessingTask;
import ru.ksu.niimm.cll.uima.morph.lab.EvaluationTask;
import ru.ksu.niimm.cll.uima.morph.lab.FeatureExtractionTaskBase;
import ru.ksu.niimm.cll.uima.morph.lab.LabConstants;
import ru.ksu.niimm.cll.uima.morph.lab.LabLauncherBase;
import ru.ksu.niimm.cll.uima.morph.opencorpora.resource.CachedSerializedDictionaryResource;
import ru.ksu.niimm.cll.uima.morph.opencorpora.resource.MorphDictionaryHolder;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

import de.tudarmstadt.ukp.dkpro.lab.Lab;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.storage.StorageService.AccessMode;
import de.tudarmstadt.ukp.dkpro.lab.task.Dimension;
import de.tudarmstadt.ukp.dkpro.lab.task.ParameterSpace;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask.ExecutionPolicy;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.ExecutableTaskBase;
import de.tudarmstadt.ukp.dkpro.lab.uima.task.UimaTask;

/**
 * @author Rinat Gareev (Kazan Federal University)
 * 
 */
public class HunposLab extends LabLauncherBase {

	public static void main(String[] args) throws Exception {
		System.setProperty("DKPRO_HOME", "wrk/hunpos-lab");
		Slf4jLoggerImpl.forceUsingThisImplementation();
		HunposLab lab = new HunposLab();
		new JCommander(lab).parse(args);
		lab.run();
	}

	// the leading '_' is added to avoid confusion in Task classes
	@Parameter(names = { "-p", "--pos-categories" }, required = true)
	private List<String> _posCategoriesList;
	private Set<String> _posCategories;

	private HunposLab() {
	}

	private void run() throws Exception {
		_posCategories = newHashSet(_posCategoriesList);
		// prepare input TypeSystem
		final TypeSystemDescription inputTS = createTypeSystemDescription(
				"ru.kfu.itis.cll.uima.commons.Commons-TypeSystem",
				"ru.kfu.cll.uima.tokenizer.tokenizer-TypeSystem",
				"ru.kfu.cll.uima.segmentation.segmentation-TypeSystem",
				"org.opencorpora.morphology-ts");
		// prepare morph dictionary resource
		final ExternalResourceDescription morphDictDesc = createExternalResourceDescription(
				CachedSerializedDictionaryResource.class,
				LabConstants.URL_RELATIVE_MORPH_DICTIONARY);
		//
		UimaTask preprocessingTask = new CorpusPreprocessingTask(inputTS, morphDictDesc);
		//
		Task corpusPartitioningTask = new CorpusPartitioningTask(foldsNum);
		//
		UimaTask prepareTrainingDataTask = new FeatureExtractionTaskBase(
				"PrepareTrainingData", inputTS) {
			@Override
			public AnalysisEngineDescription getAnalysisEngineDescription(TaskContext taskCtx)
					throws ResourceInitializationException, IOException {
				File trainDataDir = taskCtx.getStorageLocation(KEY_TRAINING_DIR, READWRITE);
				AnalysisEngineDescription hunposTrainDataWriterDesc = createPrimitiveDescription(
						HunposTrainingDataWriter.class,
						HunposTrainingDataWriter.PARAM_OUTPUT_DIR, trainDataDir);
				try {
					ExternalResourceFactory.createDependency(hunposTrainDataWriterDesc,
							DictionaryBasedTagMapper.RESOURCE_KEY_MORPH_DICTIONARY,
							MorphDictionaryHolder.class);
					bindResource(hunposTrainDataWriterDesc,
							DictionaryBasedTagMapper.RESOURCE_KEY_MORPH_DICTIONARY, morphDictDesc);
				} catch (InvalidXMLException e) {
					throw new ResourceInitializationException(e);
				}
				return createAggregateDescription(hunposTrainDataWriterDesc);
			}
		};
		//
		Task trainingTask = new ExecutableTaskBase() {
			{
				setType("Training");
			}

			@Override
			public void execute(TaskContext taskCtx) throws Exception {
				File trainDataDir = taskCtx.getStorageLocation(KEY_TRAINING_DIR, READONLY);
				File trainDataFile = getTrainingDataFile(trainDataDir);
				File modelDir = taskCtx.getStorageLocation(KEY_MODEL_DIR, READWRITE);
				File modelFile = getModelFile(modelDir);
				// get executable of trainer
				ExecutableResolver trainExeResolver = trainerResolver();
				trainExeResolver.setPlatformDetector(new PlatformDetector());
				// make cmd line
				List<String> cmd = Lists.newLinkedList();
				cmd.add(trainExeResolver.getExecutable());
				cmd.add(modelFile.getPath());
				// start trainer process
				ProcessBuilder pb = new ProcessBuilder(cmd);
				pb.redirectErrorStream(true);
				Process trainProc = pb.start();
				// attach stdout & stderr streams gobbler
				StreamGobblerBase trainProcGobbler = StreamGobblerBase.toSystemOut(
						trainProc.getInputStream());
				new Thread(trainProcGobbler).start();
				// feed training data to stdin
				ProcessIOUtils.feedProcessInput(trainProc, trainDataFile, true);
				// wait for the end of training
				int trainProcExitCode;
				try {
					trainProcExitCode = trainProc.waitFor();
					// wait a little & stop gobbler
					Thread.sleep(1000);
				} finally {
					trainProcGobbler.done();
				}
				if (trainProcExitCode != 0) {
					throw new IllegalStateException(String.format(
							"Hunpos trainer returned exit code: %s", trainProcExitCode));
				}
			}
		};
		//
		UimaTask analysisTask = new AnalysisTaskBase("Analysis", inputTS) {
			@Override
			public AnalysisEngineDescription getAnalysisEngineDescription(TaskContext taskCtx)
					throws ResourceInitializationException, IOException {
				File modelDir = taskCtx.getStorageLocation(KEY_MODEL_DIR, AccessMode.READONLY);
				File modelFile = getModelFile(modelDir);
				File outputDir = taskCtx.getStorageLocation(KEY_OUTPUT_DIR, AccessMode.READWRITE);
				//
				AnalysisEngineDescription goldRemoverDesc = createGoldRemoverDesc();
				AnalysisEngineDescription hunposAnnotatorDesc = createPrimitiveDescription(
						HunposAnnotator.class,
						HunposAnnotator.PARAM_HUNPOS_MODEL_NAME, modelFile.getPath(),
						HunposAnnotator.PARAM_TAG_MAPPER_CLASS, DictionaryBasedTagMapper.class);
				try {
					ExternalResourceFactory.createDependency(hunposAnnotatorDesc,
							DictionaryBasedTagMapper.RESOURCE_KEY_MORPH_DICTIONARY,
							MorphDictionaryHolder.class);
					bindResource(hunposAnnotatorDesc,
							DictionaryBasedTagMapper.RESOURCE_KEY_MORPH_DICTIONARY,
							morphDictDesc);
				} catch (InvalidXMLException e) {
					throw new ResourceInitializationException(e);
				}
				AnalysisEngineDescription xmiWriterDesc = createXmiWriterDesc(outputDir);
				return createAggregateDescription(
						goldRemoverDesc, hunposAnnotatorDesc, xmiWriterDesc);
			}
		};
		//
		Task evaluationTask = new EvaluationTask();
		// configure data-flow between tasks
		corpusPartitioningTask.addImport(preprocessingTask, KEY_CORPUS);
		prepareTrainingDataTask.addImport(corpusPartitioningTask, KEY_CORPUS);
		trainingTask.addImport(prepareTrainingDataTask, KEY_TRAINING_DIR);
		analysisTask.addImport(corpusPartitioningTask, KEY_CORPUS);
		analysisTask.addImport(trainingTask, KEY_MODEL_DIR);
		evaluationTask.addImport(corpusPartitioningTask, KEY_CORPUS);
		evaluationTask.addImport(analysisTask, KEY_OUTPUT_DIR);
		// create parameter space
		Integer[] foldValues = ContiguousSet.create(
				Range.closedOpen(0, foldsNum),
				DiscreteDomain.integers()).toArray(new Integer[0]);
		@SuppressWarnings("unchecked")
		ParameterSpace pSpace = new ParameterSpace(
				Dimension.create(DISCRIMINATOR_SOURCE_CORPUS_DIR, srcCorpusDir),
				// posCategories discriminator is used in the preprocessing task
				Dimension.create(DISCRIMINATOR_POS_CATEGORIES, _posCategories),
				Dimension.create(DISCRIMINATOR_FOLD, foldValues));
		//
		BatchTask batchTask = new BatchTask();
		batchTask.addTask(preprocessingTask);
		batchTask.addTask(corpusPartitioningTask);
		batchTask.addTask(prepareTrainingDataTask);
		batchTask.addTask(trainingTask);
		batchTask.addTask(analysisTask);
		batchTask.addTask(evaluationTask);
		//
		batchTask.setParameterSpace(pSpace);
		batchTask.setExecutionPolicy(ExecutionPolicy.USE_EXISTING);
		Lab.getInstance().run(batchTask);
	}

	private File getTrainingDataFile(File dir) {
		return new File(dir, HunposTrainingDataWriter.TRAINING_DATA_FILENAME);
	}

	private File getModelFile(File dir) {
		return new File(dir, "hunpos.model");
	}
}