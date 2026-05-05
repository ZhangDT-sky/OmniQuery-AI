package com.omniquery.security;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.util.JdbcConstants;
import com.omniquery.security.model.AccessPolicy;
import com.omniquery.security.model.AclRewriteResult;
import com.omniquery.security.model.UserAccessContext;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public class AclRewriter {

    private final AccessPolicy accessPolicy;

    public AclRewriter(AccessPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public AclRewriteResult rewrite(String sql, UserAccessContext user) {
        if (user.isAdmin()) {
            return new AclRewriteResult(sql, List.of());
        }
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        SQLSelectStatement select = (SQLSelectStatement) statements.get(0);
        SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) select.getSelect().getQuery();
        List<SQLExprTableSource> tableSources = new ArrayList<>();
        collectTableSources(queryBlock.getFrom(), tableSources);
        if (tableSources.isEmpty()) {
            return new AclRewriteResult(sql, List.of());
        }
        SQLBinaryOpExpr combined = null;
        List<Object> parameters = new ArrayList<>();
        for (SQLExprTableSource tableSource : tableSources) {
            String table = tableSource.getTableName().toLowerCase();
            String tenantColumn = accessPolicy.tenantColumns().get(table);
            if (tenantColumn == null) {
                continue;
            }
            String qualifier = tableSource.getAlias() == null ? null : tableSource.getAlias();
            String columnReference = qualifier == null ? tenantColumn : qualifier + "." + tenantColumn;
            SQLBinaryOpExpr aclCondition = new SQLBinaryOpExpr(
                new SQLIdentifierExpr(columnReference),
                SQLBinaryOperator.Equality,
                new SQLVariantRefExpr("?")
            );
            parameters.add(user.tenantId());
            combined = combined == null
                ? aclCondition
                : new SQLBinaryOpExpr(combined, SQLBinaryOperator.BooleanAnd, aclCondition);
        }
        if (combined == null) {
            return new AclRewriteResult(sql, List.of());
        }
        if (queryBlock.getWhere() == null) {
            queryBlock.setWhere(combined);
        } else {
            queryBlock.setWhere(new SQLBinaryOpExpr(queryBlock.getWhere(), SQLBinaryOperator.BooleanAnd, combined));
        }
        return new AclRewriteResult(SQLUtils.toSQLString(statements, JdbcConstants.MYSQL), Collections.unmodifiableList(parameters));
    }

    private void collectTableSources(SQLTableSource source, List<SQLExprTableSource> tableSources) {
        if (source instanceof SQLExprTableSource tableSource) {
            tableSources.add(tableSource);
        } else if (source instanceof SQLJoinTableSource join) {
            collectTableSources(join.getLeft(), tableSources);
            collectTableSources(join.getRight(), tableSources);
        }
    }
}
