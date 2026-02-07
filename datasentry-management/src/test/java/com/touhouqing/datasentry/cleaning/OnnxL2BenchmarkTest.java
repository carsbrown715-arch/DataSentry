package com.touhouqing.datasentry.cleaning;

import com.touhouqing.datasentry.cleaning.detector.OnnxL2DetectionProvider;
import com.touhouqing.datasentry.cleaning.model.CleaningPolicyConfig;
import com.touhouqing.datasentry.cleaning.model.CleaningRule;
import com.touhouqing.datasentry.cleaning.model.Finding;
import com.touhouqing.datasentry.cleaning.service.CleaningOpsStateService;
import com.touhouqing.datasentry.properties.DataSentryProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OnnxL2BenchmarkTest {

	@Test
	public void benchmarkOnnxInferenceLatency() {
		String modelPath = System.getenv("DATASENTRY_ONNX_BENCH_MODEL_PATH");
		Assumptions.assumeTrue(StringUtils.hasText(modelPath),
				"Skip benchmark: DATASENTRY_ONNX_BENCH_MODEL_PATH is empty");
		Assumptions.assumeTrue(Files.isRegularFile(Path.of(modelPath)), "Skip benchmark: model file does not exist");

		DataSentryProperties properties = new DataSentryProperties();
		properties.getCleaning().getL2().setProvider("ONNX");
		properties.getCleaning().getL2().setOnnxModelPath(modelPath);
		properties.getCleaning().getL2().setThreshold(0.0);

		CleaningOpsStateService opsStateService = new CleaningOpsStateService();
		OnnxL2DetectionProvider provider = new OnnxL2DetectionProvider(properties, opsStateService);
		Assertions.assertTrue(provider.isReady());

		CleaningRule rule = CleaningRule.builder().category("RISK").build();
		CleaningPolicyConfig config = CleaningPolicyConfig.builder().l2Threshold(0.0).build();
		List<String> corpus = benchmarkCorpus();

		int warmupRounds = 50;
		for (int index = 0; index < warmupRounds; index++) {
			provider.detect(corpus.get(index % corpus.size()), rule, config);
		}

		int iterations = 300;
		List<Long> latencyNs = new ArrayList<>();
		int findingCount = 0;
		long benchmarkStartNs = System.nanoTime();
		for (int index = 0; index < iterations; index++) {
			String text = corpus.get(index % corpus.size());
			long callStartNs = System.nanoTime();
			List<Finding> findings = provider.detect(text, rule, config);
			latencyNs.add(System.nanoTime() - callStartNs);
			findingCount += findings.size();
		}
		long benchmarkTotalNs = System.nanoTime() - benchmarkStartNs;

		double p50Ms = percentileMs(latencyNs, 0.50);
		double p95Ms = percentileMs(latencyNs, 0.95);
		double avgMs = averageMs(latencyNs);
		double qps = iterations * 1_000_000_000.0 / benchmarkTotalNs;

		System.out.printf(
				"[ONNX-BENCH] model=%s runtime=%s iterations=%d findings=%d avg=%.3fms p50=%.3fms p95=%.3fms qps=%.2f%n",
				modelPath, provider.runtimeVersion(), iterations, findingCount, avgMs, p50Ms, p95Ms, qps);
		System.out.printf("[ONNX-BENCH] ops success=%d failure=%d fallback=%d avgLatency=%.3fms p95Latency=%dms%n",
				opsStateService.getOnnxInferenceSuccessCount(), opsStateService.getOnnxInferenceFailureCount(),
				opsStateService.getOnnxFallbackCount(), opsStateService.getOnnxInferenceAvgLatencyMs(),
				opsStateService.getOnnxInferenceP95LatencyMs());

		Assertions.assertTrue(p95Ms >= 0.0);
		Assertions.assertTrue(qps > 0.0);
	}

	private List<String> benchmarkCorpus() {
		List<String> corpus = new ArrayList<>();
		corpus.add("尊敬的用户，点击链接领取返利，填写银行卡与验证码立即到账。");
		corpus.add("项目周报：本周完成清洗任务 128 批次，DLQ 积压下降 23%。");
		corpus.add("telegram 联系方式 @xxx，兼职刷单日结 300，先垫付后返现。");
		corpus.add("正常客服会话：请提供订单号，我们帮你查询物流状态。");
		corpus.add("身份证号和手机号已脱敏，保留审计 hash 供追踪。");
		corpus.add("风控提示：疑似钓鱼链接，建议标记为高风险并触发人工复核。");
		return corpus;
	}

	private double averageMs(List<Long> latencyNs) {
		if (latencyNs == null || latencyNs.isEmpty()) {
			return 0.0;
		}
		long totalNs = 0L;
		for (Long latency : latencyNs) {
			totalNs += latency;
		}
		return totalNs / 1_000_000.0 / latencyNs.size();
	}

	private double percentileMs(List<Long> latencyNs, double ratio) {
		if (latencyNs == null || latencyNs.isEmpty()) {
			return 0.0;
		}
		List<Long> sorted = new ArrayList<>(latencyNs);
		Collections.sort(sorted);
		int index = (int) Math.ceil(sorted.size() * ratio) - 1;
		if (index < 0) {
			index = 0;
		}
		return sorted.get(index) / 1_000_000.0;
	}

}
