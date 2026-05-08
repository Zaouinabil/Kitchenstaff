package be.kitchenstaff.repository;

import be.kitchenstaff.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByCategoryId(Long categoryId);

    List<Item> findByActiveTrue();
}