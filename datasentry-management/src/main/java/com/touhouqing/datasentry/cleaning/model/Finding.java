package com.touhouqing.datasentry.cleaning.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Finding {

	private String type;

	private String category;

	private Double severity;

	private Integer start;

	private Integer end;

	private String detectorSource;

	private String replacement;

}
