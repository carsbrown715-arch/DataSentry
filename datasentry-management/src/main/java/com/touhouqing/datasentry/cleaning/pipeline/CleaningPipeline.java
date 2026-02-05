package com.touhouqing.datasentry.cleaning.pipeline;

import com.touhouqing.datasentry.cleaning.model.CleaningContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CleaningPipeline {

	private final NormalizeNode normalizeNode;

	private final DetectNode detectNode;

	private final DecideNode decideNode;

	private final SanitizeNode sanitizeNode;

	private final AuditNode auditNode;

	public CleaningContext execute(CleaningContext context, boolean sanitizeRequested) {
		context.getMetadata().put("sanitizeRequested", sanitizeRequested);
		normalizeNode.process(context);
		detectNode.process(context);
		decideNode.process(context);
		sanitizeNode.process(context);
		auditNode.process(context);
		return context;
	}

}
