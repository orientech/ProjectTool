package cc.oit.dao.impl.mybatis;

import cc.oit.dao.dialect.Dialect;
import cc.oit.dao.dialect.MySql5Dialect;
import cc.oit.dao.dialect.OracleDialect;
import cc.oit.enums.DBDialectType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.util.Properties;

/**
 * 支持物理分页。
 * Created by unknown
 */
@Intercepts({ @Signature(type = StatementHandler.class, method = "prepare", args = { Connection.class }) })
public class PageInterceptor implements Interceptor {
	
	protected final Log logger = LogFactory.getLog(getClass());

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
		BoundSql boundSql = statementHandler.getBoundSql();
		MetaObject metaStatementHandler = MetaObject.forObject(statementHandler, new DefaultObjectFactory(), new DefaultObjectWrapperFactory());
		RowBounds rowBounds = (RowBounds) metaStatementHandler.getValue("delegate.rowBounds");
		Configuration configuration = (Configuration) metaStatementHandler.getValue("delegate.configuration");
		DBDialectType databaseType = null;
		try {
			databaseType = DBDialectType.valueOf(configuration.getVariables().getProperty("dialect").toUpperCase());
		} catch (Exception e) {
			// ignore
		}
		if (databaseType == null) {
			throw new RuntimeException("the value of the dialect property in configuration.xml is not defined : " + configuration.getVariables().getProperty("dialect"));
		}
		Dialect dialect = null;
		switch (databaseType) {
		case MYSQL:
			dialect = new MySql5Dialect();
			break;
		case ORACLE:
			dialect = new OracleDialect();
			break;
		}

		String sql = (String) metaStatementHandler.getValue("delegate.boundSql.sql");
		if ((rowBounds != null) && (rowBounds != RowBounds.DEFAULT)) {
			sql = dialect.getLimitString(sql, rowBounds.getOffset(), rowBounds.getLimit());
		}
		
		metaStatementHandler.setValue("delegate.boundSql.sql", sql);
		metaStatementHandler.setValue("delegate.rowBounds.offset", RowBounds.NO_ROW_OFFSET);
		metaStatementHandler.setValue("delegate.rowBounds.limit", RowBounds.NO_ROW_LIMIT);
		logger.debug("SQL : " + boundSql.getSql());
		return invocation.proceed();
	}

	@Override
	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}

	@Override
	public void setProperties(Properties properties) {}

}