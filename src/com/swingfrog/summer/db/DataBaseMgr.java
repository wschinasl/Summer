package com.swingfrog.summer.db;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;

public class DataBaseMgr {

	private DruidDataSource dataSource;
	private ThreadLocal<ConnInfo> local = new ThreadLocal<ConnInfo>() {
		protected ConnInfo initialValue() {
			return new ConnInfo();
		}
	};
	
	private static class SingleCase {
		public static final DataBaseMgr INSTANCE = new DataBaseMgr();
	}
	
	private DataBaseMgr() {
		
	}
	
	public static DataBaseMgr get() {
		return SingleCase.INSTANCE;
	}
	
	public void loadConfig(String path) throws Exception {
		loadConfig(new FileInputStream(path));
	}
	
	public void loadConfig(InputStream in) throws Exception {
		Properties properties = new Properties();
		properties.load(in);
		dataSource = (DruidDataSource) DruidDataSourceFactory.createDataSource(properties);
	}
	
	public Connection getConnection() throws SQLException {
		Connection conn = local.get().getConn();
		if (conn == null) {
			conn = dataSource.getConnection();
			if (local.get().isTransaction()) {
				conn.setAutoCommit(false);
			}
			local.get().setConn(conn);
		}
		return conn;
	}
	
	private void discardConnection() throws SQLException {
		Connection conn = local.get().getConn();
		if (conn != null) {
			conn.setAutoCommit(true);
			dataSource.discardConnection(conn);
		}
		local.get().dispose();
	}
	
	public void discardConnectionFromDao() throws SQLException {
		if (!local.get().isRemoteDiscard() && !local.get().isServiceDiscard()) {
			discardConnection();
		}
	}
	
	public void discardConnectionFromService() throws SQLException {
		if (!local.get().isRemoteDiscard()) {
			discardConnection();
		}
	}
	
	public void discardConnectionFromRemote() throws SQLException {
		discardConnection();
	}

	public void openTransaction() {
		local.get().setTransaction(true);
	}
	
	public void setDiscardConnectionLevelForService() {
		local.get().setServiceDiscard(true);
	}
	
	public void setDiscardConnectionLevelForRemote() {
		local.get().setRemoteDiscard(true);
	}

	private static class ConnInfo {
		private boolean serviceDiscard;
		private boolean remoteDiscard;
		private Connection conn;
		private boolean transaction;
		public ConnInfo() {
			dispose();
		}
		public boolean isServiceDiscard() {
			return serviceDiscard;
		}
		public void setServiceDiscard(boolean serviceDiscard) {
			this.serviceDiscard = serviceDiscard;
		}
		public boolean isRemoteDiscard() {
			return remoteDiscard;
		}
		public void setRemoteDiscard(boolean remoteDiscard) {
			this.remoteDiscard = remoteDiscard;
		}
		public Connection getConn() {
			return conn;
		}
		public void setConn(Connection conn) {
			this.conn = conn;
		}
		public boolean isTransaction() {
			return transaction;
		}
		public void setTransaction(boolean transaction) {
			this.transaction = transaction;
		}
		public void dispose() {
			serviceDiscard = false;
			remoteDiscard = false;
			conn = null;
			transaction = false;
		}
	}
}
