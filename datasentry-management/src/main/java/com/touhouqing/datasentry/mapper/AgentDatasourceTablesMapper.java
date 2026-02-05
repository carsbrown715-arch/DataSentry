/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.touhouqing.datasentry.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgentDatasourceTablesMapper {

	/** Select all tables for the current agent datasource. */
	@Select("""
			SELECT table_name
			FROM datasentry_agent_datasource_tables
			WHERE agent_datasource_id = #{agentDatasourceId}
			""")
	List<String> getAgentDatasourceTables(@Param("agentDatasourceId") Long agentDatasourceId);

	/** Remove tables that are not included in the current list. */
	@Delete("""
			<script>
			DELETE FROM datasentry_agent_datasource_tables
			WHERE agent_datasource_id = #{agentDatasourceId}
			<if test='tables != null and tables.size() > 0'>
			  AND table_name NOT IN (
			    <foreach collection='tables' item='table' separator=','>#{table}</foreach>
			  )
			</if>
			</script>
			""")
	int removeExpireTables(@Param("agentDatasourceId") Long agentDatasourceId, @Param("tables") List<String> tables);

	/** Remove all tables for the current agent datasource. */
	@Delete("""
			DELETE FROM datasentry_agent_datasource_tables
			WHERE agent_datasource_id = #{agentDatasourceId}
			""")
	int removeAllTables(@Param("agentDatasourceId") Long agentDatasourceId);

	/** Insert user-selected tables. */
	@Insert("""
			<script>
			INSERT IGNORE INTO datasentry_agent_datasource_tables (agent_datasource_id, table_name)
			VALUES
			<if test='tables != null and tables.size() > 0'>
			  <foreach collection='tables' item='table' separator=','>(#{agentDatasourceId}, #{table})</foreach>
			</if>
			</script>
			""")
	int insertNewTables(@Param("agentDatasourceId") Long agentDatasourceId, @Param("tables") List<String> tables);

	/** Update user's selection (tables must not be empty). */
	default int updateAgentDatasourceTables(Long agentDatasourceId, List<String> tables) {
		if (tables.isEmpty()) {
			throw new IllegalArgumentException("tables cannot be empty");
		}
		int deleteCount = removeExpireTables(agentDatasourceId, tables);
		int insertCount = insertNewTables(agentDatasourceId, tables);
		return deleteCount + insertCount;
	}

}
