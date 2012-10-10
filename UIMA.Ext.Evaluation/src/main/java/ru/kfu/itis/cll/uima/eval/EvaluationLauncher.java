/**
 * 
 */
package ru.kfu.itis.cll.uima.eval;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.io.IOUtils;

/**
 * @author Rinat Gareev (Kazan Federal University)
 * 
 */
public class EvaluationLauncher {

	public static void main(String[] args) throws Exception {
		if (!(args.length == 1 || args.length == 3)) {
			System.err.println(
					"Usage: <properties-config-filepath> [<csv-log-filepath> <metrics-filepath>]");
			return;
		}
		File propsFile = new File(args[0]);
		if (!propsFile.isFile()) {
			System.err.println("Can't find file " + propsFile);
			return;
		}
		Writer logWriter;
		Writer metricsWriter;
		boolean usingSysOut = false;
		if (args.length == 1) {
			usingSysOut = true;
			logWriter = new OutputStreamWriter(System.out);
			metricsWriter = logWriter;
		} else {
			logWriter = new OutputStreamWriter(
					new FileOutputStream(args[1]),
					"utf-8");
			metricsWriter = new OutputStreamWriter(
					new FileOutputStream(args[2]),
					"utf-8");
		}
		try {
			EvaluationConfig cfg = new EvaluationConfig();
			cfg.readFromProperties(propsFile);

			EvaluationContext evalCtx = new EvaluationContext();
			LoggingEvaluationListener loggingListener = new LoggingEvaluationListener(
					logWriter);
			evalCtx.addListener(loggingListener);

			for (String curType : cfg.getAnnoTypes()) {
				SoftPrecisionRecallListener curTypeMetricListener =
						new SoftPrecisionRecallListener(curType, metricsWriter);
				evalCtx.addListener(curTypeMetricListener);
			}

			new GoldStandardBasedEvaluation(cfg).run(evalCtx);
		} finally {
			if (!usingSysOut) {
				IOUtils.closeQuietly(logWriter);
				IOUtils.closeQuietly(metricsWriter);
			}
		}
	}

	private EvaluationLauncher() {
	}
}