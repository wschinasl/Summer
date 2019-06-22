package com.swingfrog.summer.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class PersistenceDao<T extends AbstractPersistenceEntity> extends BaseDao<PersistenceDO> {

	boolean createTable() {
		return update("CREATE TABLE IF NOT EXISTS `" + getTableName() + "` (\n" +
				"  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,\n" +
				"  `data` longtext,\n" +
				"  `createTime` datetime DEFAULT NULL,\n" +
				"  `updateTime` datetime DEFAULT NULL,\n" +
				"  PRIMARY KEY (`id`)\n" +
				") AUTO_INCREMENT=" + getAutoIncrement()) > 0;
	}

	public T add(T n) {
		PersistenceDO persistenceDO = entityTransformPersistenceDO(n);
		Long id = insertAndGetGeneratedKeys("insert into " + getTableName() + "(id,data,createTime,updateTime) values(?,?,now(),now())",
				persistenceDO.getId(), 
				persistenceDO.getData());
		n.setId(id);
		return n;
	}

	public boolean update(T n) {
		PersistenceDO persistenceDO = entityTransformPersistenceDO(n);
		return update("update " + getTableName() + " set data = ?, updateTime = now() where id = ?",
				persistenceDO.getData(),
				persistenceDO.getId()) > 0;
	}

	public Map<T, Boolean> update(List<T> ns) {
		Object[][] params = new Object[ns.size()][];
		int i = 0;
		for (T n : ns) {
			PersistenceDO persistenceDO = entityTransformPersistenceDO(n);
			params[i] = new Object[] {persistenceDO.getData(), persistenceDO.getId()};
			i ++;
		}
		int[] results = batch("update " + getTableName() + " set data = ?, updateTime = now() where id = ?", params);
		Map<T, Boolean> map = new HashMap<>();
		for (int j = 0; j < results.length; j++) {
			T n = ns.get(j);
			if (n != null) {
				map.put(n, results[j] > 0);
			}
		}
		return map;
	}

	public boolean delete(T n) {
		if (n.getId() == null) {
			return false;
		}
		return update("delete from " + getTableName() + " where id = ?", n.getId()) > 0;
	}

	public T get(long id) {
		PersistenceDO persistenceDO = getBeanByClass("select * from " + getTableName() + " where id = ?", PersistenceDO.class, id);
		if (persistenceDO == null) {
			return null;
		}
		return persistenceDOTransformEntity(persistenceDO);
	}

	public List<T> list() {
		List<PersistenceDO> persistenceDOs = listBeanByClass("select * from " + getTableName(), PersistenceDO.class);
		return persistenceDOs.stream().map(this::persistenceDOTransformEntity).collect(Collectors.toList());
	}
	
	private T persistenceDOTransformEntity(PersistenceDO persistenceDO) {
		T entity = decdeData(persistenceDO.getData());
		entity.setId(persistenceDO.getId());
		return entity;
	}
	
	private PersistenceDO entityTransformPersistenceDO(T n) {
		return PersistenceDO.build(n.getId(), encodeData(n));
	}

	protected abstract String getTableName();
	protected long getAutoIncrement() {
		return 1;
	}
	protected abstract String encodeData(T n);
	protected abstract T decdeData(String data);
}
