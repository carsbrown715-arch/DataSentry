package com.touhouqing.datasentry.cleaning.pipeline;

import com.touhouqing.datasentry.cleaning.model.CleaningContext;
import com.touhouqing.datasentry.cleaning.model.NodeResult;

public interface PipelineNode {

	NodeResult process(CleaningContext context);

}
