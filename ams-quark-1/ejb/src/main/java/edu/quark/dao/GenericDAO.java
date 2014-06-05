package edu.quark.dao;

import java.io.Serializable;
import java.util.List;


public interface GenericDAO<T, PK extends Serializable> {

	/** Persist the newInstance object into database */
	PK create(T newInstance);

	/**
	 * Retrieve an object that was previously persisted to the database using
	 * the indicated id as primary key
	 */
	T read(PK id);

	/** Save changes made to a persistent object. */
	void update(T transientObject);

	/** Find all persistent objects of matching type. */
	List<T> findAll();

	/** Remove an object from persistent storage in the database */
	void delete(T persistentObj);

}
