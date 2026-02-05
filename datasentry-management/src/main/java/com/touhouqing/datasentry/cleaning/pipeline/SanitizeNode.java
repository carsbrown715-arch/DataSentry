package com.touhouqing.datasentry.cleaning.pipeline;

import com.touhouqing.datasentry.cleaning.enums.CleaningVerdict;
import com.touhouqing.datasentry.cleaning.model.CleaningContext;
import com.touhouqing.datasentry.cleaning.model.NodeResult;
import com.touhouqing.datasentry.cleaning.util.CleaningSanitizer;
import org.springframework.stereotype.Component;

@Component
public class SanitizeNode implements PipelineNode {

	@Override
	public NodeResult process(CleaningContext context) {
		Object flag = context.getMetadata().get("sanitizeRequested");
		if (!(flag instanceof Boolean) || !((Boolean) flag)) {
			return NodeResult.skipped();
		}
		if (context.getVerdict() == CleaningVerdict.BLOCK || context.getVerdict() == CleaningVerdict.REVIEW) {
			return NodeResult.skipped();
		}
		if (context.getFindings() == null || context.getFindings().isEmpty()) {
			context.setSanitizedText(context.getOriginalText());
			context.setVerdict(CleaningVerdict.ALLOW);
			return NodeResult.ok();
		}
		String sanitized = CleaningSanitizer.sanitize(context.getOriginalText(), context.getFindings());
		context.setSanitizedText(sanitized);
		context.setVerdict(CleaningVerdict.REDACTED);
		return NodeResult.ok();
	}

}
