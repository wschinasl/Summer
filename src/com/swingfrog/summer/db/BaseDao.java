package com.swingfrog.summer.db;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseDao<T> {

	private static final Logger log = LoggerFactory.getLogger(BaseDao.class);
	private QueryRunner queryRunner;
	private Class<T> clazz;
	
	@SuppressWarnings("unchecked")
	protected BaseDao() {
		queryRunner = new QueryRunner();
		Type superClass = getClass().getGenericSuperclass();
		if (superClass instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) superClass;
			Type[] typeArgs = parameterizedType.getActualTypeArguments();
			if (typeArgs != null && typeArgs.length > 0) {
				if (typeArgs[0] instanceof Class) {
					clazz = (Class<T>) typeArgs[0];
				}
			}
		}
	}
	
	protected int update(String sql, Object... args) {
		if (log.isDebugEnabled())
			log.debug("{}  {}", sql, args);
		int i = 0;
		try {
			i = queryRunner.update(DataBaseMgr.get().getConnection(), sql, args);
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			 try {
				DataBaseMgr.get().discardConnectionFromDao();
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
			}
		}	
		return i;
	}
	
	protected int[] batch(String sql, Object[][] args) {
		if (log.isDebugEnabled()) {
			for (Object[] array : args) {				
				log.debug("{}  {}", sql, array);
			}
		}
		try {
			return queryRunner.batch(DataBaseMgr.get().getConnection(), sql, args);
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			try {
				DataBaseMgr.get().discardConnectionFromDao();
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
			}
		}
		return null;
	}
	
	protected Long insertAndGetGeneratedKeys(String sql, Object... args) {
		if (log.isDebugEnabled())
			log.debug("{}  {}", sql, args);
		try {
			return queryRunner.insert(DataBaseMgr.get().getConnection(), sql, new ScalarHandler<Long>(), args);
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			 try {
				DataBaseMgr.get().discardConnectionFromDao();
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
			}
		}	
		return null;
	}

	protected T getBean(String sql, Object... args) {
		if (log.isDebugEnabled())
			log.debug("{}  {}", sql, args);
		try {
			return queryRunner.query(DataBaseMgr.get().getConnection(), sql, new BeanHandler<>(clazz), args);
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			try {
				DataBaseMgr.get().discardConnectionFromDao();
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
			}
		}
		return null;
	}

	protected List<T> listBean(String sql, Object... args) {
		if (log.isDebugEnabled())
			log.debug("{}  {}", sql, args);
		try {
			return queryRunner.query(DataBaseMgr.get().getConnection(), sql, new BeanListHandler<T>(clazz), args);
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			try {
				DataBaseMgr.get().discardConnectionFromDao();
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
			}
		}
		return null;
	}

	protected <E> E getValue(String sql, Object... args) {
		if (log.isDebugEnabled())
			log.debug("{}  {}", sql, args);
		try {
			return queryRunner.query(DataBaseMgr.get().getConnection(), sql, new ScalarHandler<E>(), args);
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			try {
				DataBaseMgr.get().discardConnectionFromDao();
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
			}
		}
		return null;
	}
	
	protected <E> List<E> listValue(String sql, Object... args) {
		if (log.isDebugEnabled())
			log.debug("{}  {}", sql, args);
		try {
			return queryRunner.query(DataBaseMgr.get().getConnection(), sql, new ColumnListHandler<E>(), args);
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			try {
				DataBaseMgr.get().discardConnectionFromDao();
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
			}
		}
		return null;
	}
	
	protected Map<String, Object> getMap(String sql, Object... args) {
		if (log.isDebugEnabled())
			log.debug("{}  {}", sql, args);
		try {
			return queryRunner.query(DataBaseMgr.get().getConnection(), sql, new MapHandler(), args);
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			try {
				DataBaseMgr.get().discardConnectionFromDao();
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
			}
		}
		return null;
	}
	
	protected List<Map<String, Object>> listMap(String sql, Object... args) {
		if (log.isDebugEnabled())
			log.debug("{}  {}", sql, args);
		try {
			return queryRunner.query(DataBaseMgr.get().getConnection(), sql, new MapListHandler(), args);
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			try {
				DataBaseMgr.get().discardConnectionFromDao();
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
			}
		}
		return null;
	}
	
	protected <E> E getBeanByClass(String sql, Class<E> clazz, Object... args) {
		if (log.isDebugEnabled())
			log.debug("{}  {}", sql, args);
		try {
			return queryRunner.query(DataBaseMgr.get().getConnection(), sql, new BeanHandler<>(clazz), args);
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			try {
				DataBaseMgr.get().discardConnectionFromDao();
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
			}
		}
		return null;
	}
	
	protected <E> List<E> listBeanByClass(String sql, Class<E> clazz, Object... args) {
		if (log.isDebugEnabled())
			log.debug("{}  {}", sql, args);
		try {
			return queryRunner.query(DataBaseMgr.get().getConnection(), sql, new BeanListHandler<E>(clazz), args);
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		} finally {
			try {
				DataBaseMgr.get().discardConnectionFromDao();
			} catch (SQLException e) {
				log.error(e.getMessage(), e);
			}
		}
		return null;
	}
}
