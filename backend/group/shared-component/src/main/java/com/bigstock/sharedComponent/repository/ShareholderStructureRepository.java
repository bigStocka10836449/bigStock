package com.bigstock.sharedComponent.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigstock.sharedComponent.entity.ShareholderStructure;

public interface ShareholderStructureRepository extends JpaRepository<ShareholderStructure, String> {

	@Query(value = "select s from ShareholderStructure s where s.stockCode = :stockCode order by s.weekOfYear desc")
	public List<ShareholderStructure> getShareholderStructureByStockCodeDesc(@Param("stockCode") String stockCode);
	
	// List + Pageable limits the SQL result without issuing a count query.
	@Query(value = "select s from ShareholderStructure s where s.stockCode = :stockCode order by s.weekOfYear desc")
	List<ShareholderStructure> getShareholderStructureByStockCodeDesc(
			@Param("stockCode") String stockCode, Pageable pageable);

	@Query(value = "select DISTINCT  s.stockCode from ShareholderStructure s")
	public List<String> getAllShareholderStructureStockCode();
	

	@Query(value = "select distinct ( ss.stockCode) from ShareholderStructure ss where ss.weekOfYear = :maxWeekOfYear ")
	public List<String> getAreadyFinshGrapsStockCode(@Param("maxWeekOfYear") String maxWeekOfYear);

	@Query(value = "select s from ShareholderStructure s where s.weekOfYear between :startDate and :endDate")
	public List<ShareholderStructure> getShareholderStructureByWeeksBetween(@Param("startDate") String startDate, @Param("endDate") String endDate);

	@Query(value = "SELECT count(*)  FROM ShareholderStructure s WHERE s.weekOfYear = :weekOfYear")
	public int countByWeekOfYear(String weekOfYear);
	
	@Query(value ="select increase.* "
			+ "from bstock.shareholder_structure increase "
			+ "where increase.Stock_code in ("
			+ "    select distinct(a.stock_code) "
			+ "    from ("
			+ "        select "
			+ "            stock_code,"
			+ "            week_of_year,"
			+ "            (SUM(CAST(REPLACE(between_400_and_600_board_lot, ',', '') AS numeric)) +"
			+ "            SUM(CAST(REPLACE(between_600_and_800_board_lot, ',', '') AS numeric)) +"
			+ "            SUM(CAST(REPLACE(between_800_and_1000_board_lot, ',', '') AS numeric)) +"
			+ "            SUM(CAST(REPLACE(over_1000_board_lot, ',', '') AS numeric))) as total"
			+ "        FROM"
			+ "            bstock.shareholder_structure "
			+ "        WHERE"
			+ "            bstock.shareholder_structure.week_of_year = :thirdWeekOfYear"
			+ "        group by week_of_year, stock_code "
			+ "        order by stock_code, week_of_year desc"
			+ "    ) a,"
			+ "    ("
			+ "        select "
			+ "            stock_code,"
			+ "            week_of_year,"
			+ "            (SUM(CAST(REPLACE(between_400_and_600_board_lot, ',', '') AS numeric)) +"
			+ "            SUM(CAST(REPLACE(between_600_and_800_board_lot, ',', '') AS numeric)) +"
			+ "            SUM(CAST(REPLACE(between_800_and_1000_board_lot, ',', '') AS numeric)) +"
			+ "            SUM(CAST(REPLACE(over_1000_board_lot, ',', '') AS numeric)) ) as total"
			+ "        FROM"
			+ "            bstock.shareholder_structure "
			+ "        WHERE"
			+ "            bstock.shareholder_structure.week_of_year = :secondWeekOfYear"
			+ "        group by week_of_year, stock_code "
			+ "        order by stock_code, week_of_year desc"
			+ "    ) b,"
			+ "    ("
			+ "        select "
			+ "            stock_code,"
			+ "            week_of_year,"
			+ "            (SUM(CAST(REPLACE(between_400_and_600_board_lot, ',', '') AS numeric)) +"
			+ "            SUM(CAST(REPLACE(between_600_and_800_board_lot, ',', '') AS numeric)) +"
			+ "            SUM(CAST(REPLACE(between_800_and_1000_board_lot, ',', '') AS numeric)) +"
			+ "            SUM(CAST(REPLACE(over_1000_board_lot, ',', '') AS numeric)) ) as total"
			+ "        FROM"
			+ "            bstock.shareholder_structure "
			+ "        WHERE"
			+ "            bstock.shareholder_structure.week_of_year = :firstWeekOfYear"
			+ "        group by week_of_year, stock_code "
			+ "        order by stock_code, week_of_year desc"
			+ "    ) c"
			+ "    where  (c.total > b.total and b.total > a.total)"
			+ "    and c.stock_code = b.stock_code"
			+ "    and b.stock_code = a.stock_code"
			+ ")"
			+ "and increase.week_of_year = :firstWeekOfYear"
			+ "" , nativeQuery= true)
	public List<ShareholderStructure> getByOverFourHundreLotContinueIncrease(
			@Param("firstWeekOfYear") String firstWeekOfYear, @Param("secondWeekOfYear") String secondWeekOfYear,
			@Param("thirdWeekOfYear") String thirdWeekOfYear);
}
