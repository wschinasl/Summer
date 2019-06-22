package com.swingfrog.summer.db;

public class PersistenceDO {

	private Long id;

	private String data;

	public static PersistenceDO build(Long id, String data) {
		PersistenceDO pe = new PersistenceDO();
		pe.setId(id);
		pe.setData(data);
		return pe;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "PersistenceDO{" +
				"id=" + id +
				", data='" + data + '\'' +
				'}';
	}
}
