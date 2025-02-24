package com.e.d.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.e.d.model.entity.BoardEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<BoardEntity, Long> {
	Optional<BoardEntity> findByTitleAndWriterId(String title, long writerId);

	List<BoardEntity> findByWriterIdOrderByBlogIdDesc(long writerId);

	List<BoardEntity> findAllByOrderByBlogIdDesc();

	List<BoardEntity> findByTitleContainingIgnoreCaseOrWriterContainingIgnoreCaseOrContentContainingIgnoreCaseOrCategoryContainingIgnoreCase(
			String title, String writer, String content, String category);
	
	Optional<BoardEntity> findByWriterAndTitle(String writer, String title);
	
	List<BoardEntity> findByCategoryOrderByBlogIdDesc(String category);
	
	void deleteByWriterId(long writerId);
}
