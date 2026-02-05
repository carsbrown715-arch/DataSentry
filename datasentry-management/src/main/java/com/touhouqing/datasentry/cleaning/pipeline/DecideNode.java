package com.touhouqing.datasentry.cleaning.pipeline;

import com.touhouqing.datasentry.cleaning.enums.CleaningVerdict;
import com.touhouqing.datasentry.cleaning.model.CleaningContext;
import com.touhouqing.datasentry.cleaning.model.CleaningPolicyConfig;
import com.touhouqing.datasentry.cleaning.model.Finding;
import com.touhouqing.datasentry.cleaning.model.NodeResult;
import org.springframework.stereotype.Component;

@Component
public class DecideNode implements PipelineNode {

	@Override
	public NodeResult process(CleaningContext context) {
		if (context.getFindings() == null || context.getFindings().isEmpty()) {
			context.setVerdict(CleaningVerdict.ALLOW);
			return NodeResult.ok();
		}
		CleaningPolicyConfig config = context.getPolicySnapshot() != null ? context.getPolicySnapshot().getConfig()
				: new CleaningPolicyConfig();
		double maxSeverity = 0.0;
		for (Finding finding : context.getFindings()) {
			double severity = finding.getSeverity() != null ? finding.getSeverity() : 0.0;
			if (severity > maxSeverity) {
				maxSeverity = severity;
			}
		}
		if (maxSeverity >= config.resolvedBlockThreshold()) {
			context.setVerdict(CleaningVerdict.BLOCK);
		}
		else if (maxSeverity >= config.resolvedReviewThreshold()) {
			context.setVerdict(CleaningVerdict.REVIEW);
		}
		else {
			context.setVerdict(CleaningVerdict.ALLOW);
		}
		return NodeResult.ok();
	}

}
