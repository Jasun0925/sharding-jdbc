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

package org.apache.shardingsphere.sharding.merge.dql.pagination;

import org.apache.shardingsphere.infra.binder.statement.dml.SelectStatementContext;
import org.apache.shardingsphere.infra.database.DefaultDatabase;
import org.apache.shardingsphere.infra.database.type.DatabaseTypeFactory;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.QueryResult;
import org.apache.shardingsphere.infra.merge.result.MergedResult;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.session.SQLSession;
import org.apache.shardingsphere.sharding.merge.dql.ShardingDQLResultMerger;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.item.ProjectionsSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.pagination.limit.LimitSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.pagination.limit.NumberLiteralLimitValueSegment;
import org.apache.shardingsphere.sql.parser.sql.dialect.statement.mysql.dml.MySQLSelectStatement;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class LimitDecoratorMergedResultTest {
    
    @Test
    public void assertNextForSkipAll() throws SQLException {
        ShardingSphereDatabase database = mock(ShardingSphereDatabase.class, RETURNS_DEEP_STUBS);
        MySQLSelectStatement selectStatement = new MySQLSelectStatement();
        selectStatement.setProjections(new ProjectionsSegment(0, 0));
        selectStatement.setLimit(new LimitSegment(0, 0, new NumberLiteralLimitValueSegment(0, 0, Integer.MAX_VALUE), null));
        SelectStatementContext selectStatementContext = new SelectStatementContext(
                Collections.singletonMap(DefaultDatabase.LOGIC_NAME, database), Collections.emptyList(), selectStatement, DefaultDatabase.LOGIC_NAME);
        when(database.getName()).thenReturn(DefaultDatabase.LOGIC_NAME);
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypeFactory.getInstance("MySQL"));
        MergedResult actual = resultMerger.merge(Arrays.asList(mockQueryResult(), mockQueryResult(), mockQueryResult(), mockQueryResult()), selectStatementContext, database,
                mock(SQLSession.class));
        assertFalse(actual.next());
    }
    
    @Test
    public void assertNextWithoutRowCount() throws SQLException {
        ShardingSphereDatabase database = mock(ShardingSphereDatabase.class, RETURNS_DEEP_STUBS);
        MySQLSelectStatement selectStatement = new MySQLSelectStatement();
        selectStatement.setProjections(new ProjectionsSegment(0, 0));
        selectStatement.setLimit(new LimitSegment(0, 0, new NumberLiteralLimitValueSegment(0, 0, 2), null));
        SelectStatementContext selectStatementContext = new SelectStatementContext(
                Collections.singletonMap(DefaultDatabase.LOGIC_NAME, database), Collections.emptyList(), selectStatement, DefaultDatabase.LOGIC_NAME);
        when(database.getName()).thenReturn(DefaultDatabase.LOGIC_NAME);
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypeFactory.getInstance("MySQL"));
        MergedResult actual = resultMerger.merge(Arrays.asList(mockQueryResult(), mockQueryResult(), mockQueryResult(), mockQueryResult()), selectStatementContext, database,
                mock(SQLSession.class));
        for (int i = 0; i < 6; i++) {
            assertTrue(actual.next());
        }
        assertFalse(actual.next());
    }
    
    @Test
    public void assertNextWithRowCount() throws SQLException {
        ShardingSphereDatabase database = mock(ShardingSphereDatabase.class, RETURNS_DEEP_STUBS);
        MySQLSelectStatement selectStatement = new MySQLSelectStatement();
        selectStatement.setProjections(new ProjectionsSegment(0, 0));
        selectStatement.setLimit(new LimitSegment(0, 0, new NumberLiteralLimitValueSegment(0, 0, 2), new NumberLiteralLimitValueSegment(0, 0, 2)));
        SelectStatementContext selectStatementContext = new SelectStatementContext(Collections.singletonMap(DefaultDatabase.LOGIC_NAME, database),
                Collections.emptyList(), selectStatement, DefaultDatabase.LOGIC_NAME);
        when(database.getName()).thenReturn(DefaultDatabase.LOGIC_NAME);
        ShardingDQLResultMerger resultMerger = new ShardingDQLResultMerger(DatabaseTypeFactory.getInstance("MySQL"));
        MergedResult actual = resultMerger.merge(Arrays.asList(mockQueryResult(), mockQueryResult(), mockQueryResult(), mockQueryResult()), selectStatementContext, database,
                mock(SQLSession.class));
        assertTrue(actual.next());
        assertTrue(actual.next());
        assertFalse(actual.next());
    }
    
    private QueryResult mockQueryResult() throws SQLException {
        QueryResult result = mock(QueryResult.class, RETURNS_DEEP_STUBS);
        when(result.next()).thenReturn(true, true, false);
        return result;
    }
}
