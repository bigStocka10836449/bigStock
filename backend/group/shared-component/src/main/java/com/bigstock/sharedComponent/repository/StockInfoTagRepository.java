package com.bigstock.sharedComponent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigstock.sharedComponent.entity.StockInfoTag;

public interface StockInfoTagRepository extends JpaRepository<StockInfoTag, String> {

	/**
	 * Find stocks containing a specific tag
	 */
	@Query(value = """
			    SELECT * FROM bstock.stock_info_tag
			    WHERE tags @> CAST(ARRAY[:tag] AS text[])
			""", nativeQuery = true)
	List<StockInfoTag> findByTag(@Param("tag") String tag);

	@Query(value = """
			    SELECT * FROM bstock.stock_info_tag
			    WHERE tags @> CAST(:tags AS text[])
			""", nativeQuery = true)
	List<StockInfoTag> findByAllTags(@Param("tags") String[] tags);

}
