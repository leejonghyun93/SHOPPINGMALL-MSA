package org.kosa.cartservice.mapper;


import org.kosa.cartservice.dto.Cart;
import org.kosa.cartservice.dto.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, String> {

    Optional<Cart> findByUserIdAndStatus(String userId, String status);

    Optional<Cart> findActiveCartByUserId(String userId);
}