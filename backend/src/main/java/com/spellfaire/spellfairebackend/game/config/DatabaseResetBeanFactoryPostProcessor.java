package com.spellfaire.spellfairebackend.game.config;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Drops all tables in the current schema on startup (dev convenience).
 *
 * Why this exists:
 * - Hibernate's create/create-drop can emit DROP FOREIGN KEY statements that fail on a fresh schema in MySQL.
 * - Dropping tables with IF EXISTS + disabling FK checks is safe even when the schema is empty.
 *
 * This runs before the JPA EntityManagerFactory is created, so Hibernate can recreate tables afterwards.
 */
@Component
public class DatabaseResetBeanFactoryPostProcessor
	implements BeanFactoryPostProcessor, PriorityOrdered, EnvironmentAware {

	private static final Logger log = LoggerFactory.getLogger(DatabaseResetBeanFactoryPostProcessor.class);

	private static final String RESET_PROPERTY = "spellfaire.db.reset-on-startup";

	private Environment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		boolean resetEnabled = environment.getProperty(RESET_PROPERTY, Boolean.class, Boolean.FALSE);
		if (!resetEnabled) {
			return;
		}

		String jdbcUrl = environment.getProperty("spring.datasource.url");
		String username = environment.getProperty("spring.datasource.username");
		String password = environment.getProperty("spring.datasource.password", "");
		String driverClassName = environment.getProperty("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");

		if (jdbcUrl == null || jdbcUrl.isBlank()) {
			log.warn("DB reset requested but spring.datasource.url is not set; skipping");
			return;
		}

		try {
			Class.forName(driverClassName);
		} catch (ClassNotFoundException ex) {
			log.warn("DB reset requested but JDBC driver '{}' is not available; skipping", driverClassName, ex);
			return;
		}

		try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
			String schemaName = connection.getCatalog();
			if (schemaName == null || schemaName.isBlank()) {
				log.warn("DB reset requested but schema name (catalog) is empty; skipping");
				return;
			}

			List<String> tableNames = listTableNames(connection, schemaName);
			if (tableNames.isEmpty()) {
				log.info("DB reset: schema '{}' has no tables; nothing to drop", schemaName);
				return;
			}

			log.info("DB reset: dropping {} tables in schema '{}'", tableNames.size(), schemaName);

			try (Statement statement = connection.createStatement()) {
				statement.execute("SET FOREIGN_KEY_CHECKS = 0");
				for (String tableName : tableNames) {
					// Backtick-quote because some names may collide with reserved keywords.
					statement.execute("DROP TABLE IF EXISTS `" + tableName + "`");
				}
				statement.execute("SET FOREIGN_KEY_CHECKS = 1");
			}
		} catch (Exception ex) {
			// If this fails, we log and continue; the app may still start with an existing schema.
			log.warn("DB reset failed; continuing startup without dropping schema", ex);
		}
	}

	private static List<String> listTableNames(Connection connection, String schemaName) throws Exception {
		String sql = "select table_name from information_schema.tables where table_schema = ? and table_type = 'BASE TABLE'";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, schemaName);
			try (ResultSet rs = ps.executeQuery()) {
				List<String> result = new ArrayList<>();
				while (rs.next()) {
					result.add(rs.getString(1));
				}
				return result;
			}
		}
	}
}
