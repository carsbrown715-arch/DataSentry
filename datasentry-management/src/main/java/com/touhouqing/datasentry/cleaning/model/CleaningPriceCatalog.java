package com.touhouqing.datasentry.cleaning.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("datasentry_cleaning_price_catalog")
public class CleaningPriceCatalog {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String provider;

	private String model;

	private String version;

	@TableField("input_price_per_1k")
	private BigDecimal inputPricePer1k;

	@TableField("output_price_per_1k")
	private BigDecimal outputPricePer1k;

	private String currency;

	private LocalDateTime createdTime;

	private LocalDateTime updatedTime;

}
