package com.touhouqing.datasentry.cleaning.detector;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxJavaType;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import com.touhouqing.datasentry.cleaning.model.CleaningPolicyConfig;
import com.touhouqing.datasentry.cleaning.model.CleaningRule;
import com.touhouqing.datasentry.cleaning.model.Finding;
import com.touhouqing.datasentry.cleaning.service.CleaningOpsStateService;
import com.touhouqing.datasentry.properties.DataSentryProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OnnxL2DetectionProvider implements L2DetectionProvider {

	private static final int DEFAULT_FEATURE_DIM = 16;

	private static final int RESERVED_FEATURES = 8;

	private static final float MAX_TEXT_LENGTH = 512.0F;

	private final DataSentryProperties dataSentryProperties;

	private final CleaningOpsStateService opsStateService;

	private final Object sessionLock = new Object();

	private volatile OrtSession session;

	private volatile String loadedModelPath;

	@Override
	public String name() {
		return "ONNX";
	}

	public boolean isReady() {
		return getOrCreateSession() != null;
	}

	public String runtimeVersion() {
		try {
			return OrtEnvironment.getEnvironment().getVersion();
		}
		catch (Exception e) {
			return null;
		}
	}

	@Override
	public List<Finding> detect(String text, CleaningRule rule, CleaningPolicyConfig config) {
		if (text == null || text.isBlank()) {
			return List.of();
		}
		OrtSession activeSession = getOrCreateSession();
		if (activeSession == null) {
			return List.of();
		}
		long startNanos = System.nanoTime();
		try {
			double score = inferScore(activeSession, text);
			opsStateService.markOnnxInferenceSuccess(elapsedMillis(startNanos));
			double providerThreshold = dataSentryProperties.getCleaning().getL2().getThreshold();
			double policyThreshold = config != null ? config.resolvedL2Threshold() : 0.6;
			double threshold = Math.max(0.0, Math.min(1.0, Math.max(providerThreshold, policyThreshold)));
			if (score < threshold) {
				return List.of();
			}
			List<Finding> findings = new ArrayList<>();
			findings.add(Finding.builder()
				.type(rule.getCategory())
				.category(rule.getCategory())
				.severity(score)
				.start(0)
				.end(text.length())
				.detectorSource("L2_ONNX_RUNTIME")
				.build());
			return findings;
		}
		catch (Exception e) {
			opsStateService.markOnnxInferenceFailure(elapsedMillis(startNanos));
			throw e;
		}
	}

	@PreDestroy
	public void destroy() {
		closeSession();
	}

	private OrtSession getOrCreateSession() {
		String modelPath = resolveModelPath();
		if (modelPath == null) {
			closeSession();
			return null;
		}
		OrtSession active = this.session;
		if (active != null && modelPath.equals(this.loadedModelPath)) {
			return active;
		}
		synchronized (sessionLock) {
			active = this.session;
			if (active != null && modelPath.equals(this.loadedModelPath)) {
				return active;
			}
			closeSessionInternal();
			try {
				OrtEnvironment environment = OrtEnvironment.getEnvironment();
				OrtSession.SessionOptions options = new OrtSession.SessionOptions();
				options.setInterOpNumThreads(1);
				options.setIntraOpNumThreads(1);
				options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
				OrtSession created = environment.createSession(modelPath, options);
				this.session = created;
				this.loadedModelPath = modelPath;
				opsStateService.markOnnxModelLoaded(buildModelSignature(created), runtimeVersion());
				log.info("ONNX model loaded: {}", modelPath);
				return created;
			}
			catch (Exception e) {
				opsStateService.markOnnxModelLoadFailure();
				log.warn("Failed to initialize ONNX Runtime model: {}", modelPath, e);
				this.loadedModelPath = null;
				this.session = null;
				return null;
			}
		}
	}

	private double inferScore(OrtSession activeSession, String text) {
		try {
			Map<String, NodeInfo> inputs = activeSession.getInputInfo();
			if (inputs == null || inputs.isEmpty()) {
				throw new IllegalStateException("ONNX model has no input node");
			}
			Map.Entry<String, NodeInfo> entry = inputs.entrySet().iterator().next();
			String inputName = entry.getKey();
			TensorInfo inputInfo = extractTensorInfo(entry.getValue());
			if (inputInfo == null) {
				throw new IllegalStateException("ONNX input node is not tensor");
			}
			try (OnnxTensor inputTensor = createInputTensor(inputInfo, text);
					OrtSession.Result result = activeSession.run(Map.of(inputName, inputTensor))) {
				double rawScore = extractRawScore(result);
				return normalizeScore(rawScore);
			}
		}
		catch (Exception e) {
			throw new IllegalStateException("ONNX inference failed", e);
		}
	}

	private TensorInfo extractTensorInfo(NodeInfo nodeInfo) {
		if (nodeInfo == null || !(nodeInfo.getInfo() instanceof TensorInfo tensorInfo)) {
			return null;
		}
		return tensorInfo;
	}

	private OnnxTensor createInputTensor(TensorInfo inputInfo, String text) throws OrtException {
		OrtEnvironment environment = OrtEnvironment.getEnvironment();
		if (inputInfo.type == OnnxJavaType.STRING) {
			long[] shape = inputInfo.getShape();
			if (shape != null && shape.length >= 2) {
				return OnnxTensor.createTensor(environment, new String[][] { { text } });
			}
			return OnnxTensor.createTensor(environment, new String[] { text });
		}
		if (inputInfo.type == OnnxJavaType.FLOAT) {
			int featureDim = resolveFeatureDimension(inputInfo);
			float[][] features = new float[1][featureDim];
			buildFeatureVector(text, features[0]);
			return OnnxTensor.createTensor(environment, features);
		}
		throw new IllegalStateException("Unsupported ONNX input type: " + inputInfo.type);
	}

	private int resolveFeatureDimension(TensorInfo inputInfo) {
		long[] shape = inputInfo.getShape();
		if (shape != null && shape.length >= 2 && shape[1] > 0 && shape[1] <= Integer.MAX_VALUE) {
			return (int) shape[1];
		}
		return DEFAULT_FEATURE_DIM;
	}

	private void buildFeatureVector(String text, float[] featureVector) {
		String normalized = text.toLowerCase(Locale.ROOT);
		featureVector[0] = normalizeLength(text.length());
		featureVector[1] = containsAny(normalized, "http", "www.", "链接", "点击") ? 1.0F : 0.0F;
		featureVector[2] = containsAny(normalized, "银行卡", "身份证", "手机号", "验证码") ? 1.0F : 0.0F;
		featureVector[3] = containsAny(normalized, "转账", "返利", "兼职", "刷单") ? 1.0F : 0.0F;
		featureVector[4] = countRatio(text, Character::isDigit);
		featureVector[5] = countRatio(text, Character::isUpperCase);
		featureVector[6] = countRatio(text,
				value -> !Character.isLetterOrDigit(value) && !Character.isWhitespace(value));
		featureVector[7] = containsAny(normalized, "telegram", "qq", "vx", "wx") ? 1.0F : 0.0F;
		if (featureVector.length <= RESERVED_FEATURES) {
			return;
		}
		for (int index = 0; index < normalized.length(); index++) {
			int bucket = RESERVED_FEATURES
					+ Math.floorMod(normalized.charAt(index), featureVector.length - RESERVED_FEATURES);
			featureVector[bucket] += 1.0F;
		}
		float max = 1.0F;
		for (int index = RESERVED_FEATURES; index < featureVector.length; index++) {
			max = Math.max(max, featureVector[index]);
		}
		for (int index = RESERVED_FEATURES; index < featureVector.length; index++) {
			featureVector[index] = featureVector[index] / max;
		}
	}

	private float normalizeLength(int length) {
		return Math.min(1.0F, length / MAX_TEXT_LENGTH);
	}

	private boolean containsAny(String text, String... terms) {
		for (String term : terms) {
			if (text.contains(term)) {
				return true;
			}
		}
		return false;
	}

	private float countRatio(String text, IntPredicate predicate) {
		if (text == null || text.isBlank()) {
			return 0.0F;
		}
		int count = 0;
		for (int index = 0; index < text.length(); index++) {
			if (predicate.test(text.charAt(index))) {
				count++;
			}
		}
		return (float) count / (float) Math.max(1, text.length());
	}

	private double extractRawScore(OrtSession.Result result) throws OrtException {
		for (Map.Entry<String, OnnxValue> entry : result) {
			double value = extractRawScore(entry.getValue());
			if (!Double.isNaN(value)) {
				return value;
			}
		}
		throw new IllegalStateException("ONNX output tensor does not contain numeric score");
	}

	private double extractRawScore(OnnxValue value) throws OrtException {
		if (!(value instanceof OnnxTensor tensor)) {
			return Double.NaN;
		}
		Object raw = tensor.getValue();
		if (raw instanceof float[] floatArray && floatArray.length > 0) {
			return floatArray[floatArray.length - 1];
		}
		if (raw instanceof float[][] floatMatrix && floatMatrix.length > 0 && floatMatrix[0].length > 0) {
			return floatMatrix[0][floatMatrix[0].length - 1];
		}
		if (raw instanceof double[] doubleArray && doubleArray.length > 0) {
			return doubleArray[doubleArray.length - 1];
		}
		if (raw instanceof double[][] doubleMatrix && doubleMatrix.length > 0 && doubleMatrix[0].length > 0) {
			return doubleMatrix[0][doubleMatrix[0].length - 1];
		}
		if (raw instanceof long[] longArray && longArray.length > 0) {
			return longArray[longArray.length - 1];
		}
		if (raw instanceof long[][] longMatrix && longMatrix.length > 0 && longMatrix[0].length > 0) {
			return longMatrix[0][longMatrix[0].length - 1];
		}
		if (raw instanceof int[] intArray && intArray.length > 0) {
			return intArray[intArray.length - 1];
		}
		if (raw instanceof int[][] intMatrix && intMatrix.length > 0 && intMatrix[0].length > 0) {
			return intMatrix[0][intMatrix[0].length - 1];
		}
		return Double.NaN;
	}

	private double normalizeScore(double rawScore) {
		if (Double.isNaN(rawScore) || Double.isInfinite(rawScore)) {
			throw new IllegalStateException("Invalid ONNX score: " + rawScore);
		}
		if (rawScore >= 0.0 && rawScore <= 1.0) {
			return rawScore;
		}
		double sigmoid = 1.0 / (1.0 + Math.exp(-rawScore));
		if (Double.isNaN(sigmoid) || Double.isInfinite(sigmoid)) {
			throw new IllegalStateException("Invalid ONNX score after sigmoid: " + rawScore);
		}
		return Math.max(0.0, Math.min(1.0, sigmoid));
	}

	private String resolveModelPath() {
		String modelPath = dataSentryProperties.getCleaning().getL2().getOnnxModelPath();
		if (modelPath == null || modelPath.isBlank()) {
			return null;
		}
		Path path = Path.of(modelPath.trim());
		if (!Files.exists(path) || !Files.isRegularFile(path)) {
			log.debug("ONNX model path unavailable: {}", modelPath);
			return null;
		}
		return path.toAbsolutePath().toString();
	}

	private String buildModelSignature(OrtSession activeSession) {
		try {
			String inputSignature = firstNodeSignature(activeSession.getInputInfo());
			String outputSignature = firstNodeSignature(activeSession.getOutputInfo());
			return "input{" + inputSignature + "};output{" + outputSignature + "}";
		}
		catch (Exception e) {
			return "unknown";
		}
	}

	private String firstNodeSignature(Map<String, NodeInfo> nodeInfoMap) {
		if (nodeInfoMap == null || nodeInfoMap.isEmpty()) {
			return "empty";
		}
		Map.Entry<String, NodeInfo> first = nodeInfoMap.entrySet().iterator().next();
		String nodeName = first.getKey();
		NodeInfo nodeInfo = first.getValue();
		if (nodeInfo == null || !(nodeInfo.getInfo() instanceof TensorInfo tensorInfo)) {
			return nodeName + ":non-tensor";
		}
		String shape = Arrays.toString(tensorInfo.getShape());
		return nodeName + ":" + tensorInfo.type + ":" + shape;
	}

	private long elapsedMillis(long startNanos) {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
	}

	private void closeSession() {
		synchronized (sessionLock) {
			closeSessionInternal();
		}
	}

	private void closeSessionInternal() {
		if (this.session != null) {
			try {
				this.session.close();
			}
			catch (Exception e) {
				log.debug("Failed to close ONNX session", e);
			}
		}
		this.session = null;
		this.loadedModelPath = null;
	}

	@FunctionalInterface
	private interface IntPredicate {

		boolean test(int value);

	}

}
