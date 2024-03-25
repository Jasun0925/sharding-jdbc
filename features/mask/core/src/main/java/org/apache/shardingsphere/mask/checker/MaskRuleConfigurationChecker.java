/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.mask.checker;

import org.apache.shardingsphere.infra.algorithm.core.config.AlgorithmConfiguration;
import org.apache.shardingsphere.infra.algorithm.core.exception.AlgorithmNotFoundOnColumnException;
import org.apache.shardingsphere.infra.config.rule.checker.RuleConfigurationChecker;
import org.apache.shardingsphere.infra.exception.core.ShardingSpherePreconditions;
import org.apache.shardingsphere.infra.exception.rule.DuplicateRuleException;
import org.apache.shardingsphere.infra.rule.ShardingSphereRule;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.mask.api.config.MaskRuleConfiguration;
import org.apache.shardingsphere.mask.api.config.rule.MaskColumnRuleConfiguration;
import org.apache.shardingsphere.mask.api.config.rule.MaskTableRuleConfiguration;
import org.apache.shardingsphere.mask.constant.MaskOrder;
import org.apache.shardingsphere.mask.spi.MaskAlgorithm;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Mask rule configuration checker.
 */
public final class MaskRuleConfigurationChecker implements RuleConfigurationChecker<MaskRuleConfiguration> {
    
    @Override
    public void check(final String databaseName, final MaskRuleConfiguration ruleConfig, final Map<String, DataSource> dataSourceMap, final Collection<ShardingSphereRule> builtRules) {
        checkMaskAlgorithms(ruleConfig.getMaskAlgorithms());
        checkTables(databaseName, ruleConfig.getTables(), ruleConfig.getMaskAlgorithms());
    }
    
    private void checkMaskAlgorithms(final Map<String, AlgorithmConfiguration> maskAlgorithms) {
        maskAlgorithms.values().forEach(each -> TypedSPILoader.checkService(MaskAlgorithm.class, each.getType(), each.getProps()));
    }
    
    private void checkTables(final String databaseName, final Collection<MaskTableRuleConfiguration> tables, final Map<String, AlgorithmConfiguration> maskAlgorithms) {
        checkTablesNotDuplicated(databaseName, tables);
        tables.forEach(each -> checkColumns(databaseName, each.getName(), each.getColumns(), maskAlgorithms));
    }
    
    private void checkTablesNotDuplicated(final String databaseName, final Collection<MaskTableRuleConfiguration> tables) {
        Collection<String> duplicatedTables = tables.stream().map(MaskTableRuleConfiguration::getName)
                .collect(Collectors.groupingBy(each -> each, Collectors.counting())).entrySet().stream().filter(each -> each.getValue() > 1).map(Entry::getKey).collect(Collectors.toSet());
        ShardingSpherePreconditions.checkState(duplicatedTables.isEmpty(), () -> new DuplicateRuleException("MASK", databaseName, duplicatedTables));
    }
    
    private void checkColumns(final String databaseName, final String tableName, final Collection<MaskColumnRuleConfiguration> columns, final Map<String, AlgorithmConfiguration> maskAlgorithms) {
        for (MaskColumnRuleConfiguration each : columns) {
            ShardingSpherePreconditions.checkState(maskAlgorithms.containsKey(each.getMaskAlgorithm()),
                    () -> new AlgorithmNotFoundOnColumnException("mask", each.getMaskAlgorithm(), databaseName, tableName, each.getLogicColumn()));
        }
    }
    
    @Override
    public int getOrder() {
        return MaskOrder.ORDER;
    }
    
    @Override
    public Class<MaskRuleConfiguration> getTypeClass() {
        return MaskRuleConfiguration.class;
    }
}
