package com.touhouqing.datasentry.cleaning.pipeline;

import com.touhouqing.datasentry.cleaning.model.CleaningContext;
import com.touhouqing.datasentry.cleaning.model.NodeResult;
import com.touhouqing.datasentry.cleaning.util.CleaningTextNormalizer;
import org.springframework.stereotype.Component;

@Component
public class NormalizeNode implements PipelineNode {

	@Override
	public NodeResult process(CleaningContext context) {
		context.setNormalizedText(CleaningTextNormalizer.normalize(context.getOriginalText()));
		return NodeResult.ok();
	}

}
