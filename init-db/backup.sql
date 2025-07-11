/*M!999999\- enable the sandbox mode */ 
-- MariaDB dump 10.19  Distrib 10.11.13-MariaDB, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: live_commerce_db
-- ------------------------------------------------------
-- Server version	10.11.13-MariaDB-0ubuntu0.24.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `tb_admin`
--

DROP TABLE IF EXISTS `tb_admin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_admin` (
  `ADMIN_ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '관리자 고유 ID',
  `USER_ID` varchar(50) NOT NULL COMMENT '회원 ID (FK)',
  `ACCESS_LEVEL` varchar(20) DEFAULT NULL COMMENT '권한 등급(SUPER, MODERATOR 등)',
  PRIMARY KEY (`ADMIN_ID`),
  KEY `USER_ID` (`USER_ID`) USING BTREE,
  CONSTRAINT `fk_admin_user` FOREIGN KEY (`USER_ID`) REFERENCES `tb_member` (`USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='관리자 전용 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_admin`
--

LOCK TABLES `tb_admin` WRITE;
/*!40000 ALTER TABLE `tb_admin` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_admin` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_board`
--

DROP TABLE IF EXISTS `tb_board`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_board` (
  `BOARD_ID` varchar(50) NOT NULL COMMENT '게시글 ID',
  `BOARD_TYPE_ID` varchar(20) NOT NULL COMMENT '게시판 타입 ID',
  `USER_ID` varchar(50) DEFAULT NULL COMMENT '작성자 ID',
  `TITLE` varchar(200) NOT NULL COMMENT '제목',
  `CONTENT` text NOT NULL COMMENT '내용',
  `BOARD_STATUS` varchar(20) DEFAULT 'ACTIVE' COMMENT '게시글 상태',
  `VIEW_COUNT` int(11) DEFAULT 0 COMMENT '조회수',
  `LIKE_COUNT` int(11) DEFAULT 0 COMMENT '좋아요 수',
  `REPLY_COUNT` int(11) DEFAULT 0 COMMENT '댓글 수',
  `IS_NOTICE` char(1) DEFAULT 'N' COMMENT '공지사항 여부',
  `IS_SECRET` char(1) DEFAULT 'N' COMMENT '비밀글 여부',
  `IS_TOP` char(1) DEFAULT 'N' COMMENT '상단 고정 여부',
  `AUTHOR_NAME` varchar(100) DEFAULT NULL COMMENT '작성자명',
  `AUTHOR_IP` varchar(45) DEFAULT NULL COMMENT '작성자 IP',
  `PASSWORD` varchar(255) DEFAULT NULL COMMENT '비회원 비밀번호',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  `UPDATED_DATE` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일',
  PRIMARY KEY (`BOARD_ID`),
  KEY `IDX_BOARD_CREATED_DATE` (`CREATED_DATE`) USING BTREE,
  KEY `IDX_BOARD_STATUS` (`BOARD_STATUS`) USING BTREE,
  KEY `IDX_BOARD_TYPE_ID` (`BOARD_TYPE_ID`) USING BTREE,
  KEY `IDX_BOARD_USER_ID` (`USER_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='통합 게시판 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_board`
--

LOCK TABLES `tb_board` WRITE;
/*!40000 ALTER TABLE `tb_board` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_board` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_board_file`
--

DROP TABLE IF EXISTS `tb_board_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_board_file` (
  `FILE_ID` varchar(50) NOT NULL COMMENT '파일 ID',
  `BOARD_ID` varchar(50) NOT NULL COMMENT '게시글 ID',
  `ORIGINAL_NAME` varchar(255) NOT NULL COMMENT '원본 파일명',
  `STORED_NAME` varchar(255) NOT NULL COMMENT '저장 파일명',
  `FILE_PATH` varchar(500) NOT NULL COMMENT '파일 경로',
  `FILE_SIZE` bigint(20) NOT NULL COMMENT '파일 크기',
  `FILE_EXTENSION` varchar(10) DEFAULT NULL COMMENT '파일 확장자',
  `DOWNLOAD_COUNT` int(11) DEFAULT 0 COMMENT '다운로드 횟수',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  PRIMARY KEY (`FILE_ID`),
  KEY `FK_BOARD_FILE_BOARD` (`BOARD_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='게시판 첨부파일 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_board_file`
--

LOCK TABLES `tb_board_file` WRITE;
/*!40000 ALTER TABLE `tb_board_file` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_board_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_board_reply`
--

DROP TABLE IF EXISTS `tb_board_reply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_board_reply` (
  `REPLY_ID` varchar(50) NOT NULL COMMENT '댓글 ID',
  `BOARD_ID` varchar(50) NOT NULL COMMENT '게시글 ID',
  `PARENT_REPLY_ID` varchar(50) DEFAULT NULL COMMENT '부모 댓글 ID',
  `USER_ID` varchar(50) DEFAULT NULL COMMENT '작성자 ID',
  `CONTENT` text NOT NULL COMMENT '댓글 내용',
  `REPLY_STATUS` varchar(20) DEFAULT 'ACTIVE' COMMENT '댓글 상태',
  `REPLY_DEPTH` int(11) DEFAULT 1 COMMENT '댓글 깊이',
  `AUTHOR_NAME` varchar(100) DEFAULT NULL COMMENT '작성자명',
  `AUTHOR_IP` varchar(45) DEFAULT NULL COMMENT '작성자 IP',
  `PASSWORD` varchar(255) DEFAULT NULL COMMENT '비회원 비밀번호',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  `UPDATED_DATE` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일',
  PRIMARY KEY (`REPLY_ID`),
  KEY `FK_BOARD_REPLY_PARENT` (`PARENT_REPLY_ID`) USING BTREE,
  KEY `IDX_BOARD_REPLY_BOARD_ID` (`BOARD_ID`) USING BTREE,
  KEY `IDX_BOARD_REPLY_USER_ID` (`USER_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='게시판 댓글 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_board_reply`
--

LOCK TABLES `tb_board_reply` WRITE;
/*!40000 ALTER TABLE `tb_board_reply` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_board_reply` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_board_type`
--

DROP TABLE IF EXISTS `tb_board_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_board_type` (
  `BOARD_TYPE_ID` varchar(20) NOT NULL COMMENT '게시판 타입 ID',
  `BOARD_TYPE_NAME` varchar(50) NOT NULL COMMENT '게시판 타입명',
  `BOARD_TYPE_DESCRIPTION` text DEFAULT NULL COMMENT '게시판 설명',
  `USE_YN` char(1) DEFAULT 'Y' COMMENT '사용 여부',
  `DISPLAY_ORDER` int(11) DEFAULT 1 COMMENT '표시 순서',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  `UPDATED_DATE` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일',
  PRIMARY KEY (`BOARD_TYPE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='게시판 타입 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_board_type`
--

LOCK TABLES `tb_board_type` WRITE;
/*!40000 ALTER TABLE `tb_board_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_board_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_broadcast_products`
--

DROP TABLE IF EXISTS `tb_broadcast_products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_broadcast_products` (
  `broadcast_product_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '방송-상품 연결 고유 ID',
  `broadcast_id` bigint(20) NOT NULL COMMENT '방송 ID',
  `product_id` int(11) NOT NULL COMMENT '상품 ID',
  `display_order` int(11) DEFAULT 1 COMMENT '상품 표시 순서',
  `is_featured` tinyint(1) DEFAULT 0 COMMENT '메인 상품 여부',
  `special_price` int(11) DEFAULT NULL COMMENT '방송 특가 (NULL이면 원래 가격)',
  `created_at` timestamp NULL DEFAULT current_timestamp() COMMENT '생성일시',
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일시',
  PRIMARY KEY (`broadcast_product_id`),
  UNIQUE KEY `unique_broadcast_product` (`broadcast_id`,`product_id`),
  KEY `idx_broadcast_id` (`broadcast_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_display_order` (`display_order`),
  KEY `idx_is_featured` (`is_featured`),
  CONSTRAINT `fk_broadcast` FOREIGN KEY (`broadcast_id`) REFERENCES `tb_live_broadcasts` (`broadcast_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_product` FOREIGN KEY (`product_id`) REFERENCES `tb_product` (`PRODUCT_ID`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=87 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='라이브 방송과 상품의 연결 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_broadcast_products`
--

LOCK TABLES `tb_broadcast_products` WRITE;
/*!40000 ALTER TABLE `tb_broadcast_products` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_broadcast_products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_broadcast_status_history`
--

DROP TABLE IF EXISTS `tb_broadcast_status_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_broadcast_status_history` (
  `history_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '방송 상태 변경 이력 고유 ID',
  `broadcast_id` bigint(20) NOT NULL COMMENT '방송 ID',
  `previous_status` varchar(20) DEFAULT NULL COMMENT '이전 상태',
  `new_status` varchar(20) NOT NULL COMMENT '새로운 상태',
  `changed_by` varchar(50) DEFAULT NULL COMMENT '상태 변경한 사용자 ID',
  `change_reason` varchar(255) DEFAULT NULL COMMENT '변경 사유',
  `created_at` timestamp NULL DEFAULT current_timestamp() COMMENT '변경 시각',
  PRIMARY KEY (`history_id`),
  KEY `fk_status_history_user` (`changed_by`),
  KEY `idx_broadcast_id` (`broadcast_id`),
  KEY `idx_new_status` (`new_status`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_status_history_broadcast` FOREIGN KEY (`broadcast_id`) REFERENCES `tb_live_broadcasts` (`broadcast_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_status_history_user` FOREIGN KEY (`changed_by`) REFERENCES `tb_member` (`USER_ID`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='라이브 방송 상태 변경 이력 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_broadcast_status_history`
--

LOCK TABLES `tb_broadcast_status_history` WRITE;
/*!40000 ALTER TABLE `tb_broadcast_status_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_broadcast_status_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_cart`
--

DROP TABLE IF EXISTS `tb_cart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_cart` (
  `CART_ID` varchar(50) NOT NULL COMMENT '장바구니 ID',
  `USER_ID` varchar(50) NOT NULL COMMENT '사용자 ID',
  `STATUS` varchar(20) DEFAULT 'ACTIVE' COMMENT '상태',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  `UPDATED_DATE` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일',
  PRIMARY KEY (`CART_ID`),
  KEY `IDX_CART_USER_ID` (`USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='장바구니 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_cart`
--

LOCK TABLES `tb_cart` WRITE;
/*!40000 ALTER TABLE `tb_cart` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_cart` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_cart_item`
--

DROP TABLE IF EXISTS `tb_cart_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_cart_item` (
  `CART_ITEM_ID` varchar(50) NOT NULL COMMENT '장바구니 상품 ID',
  `CART_ID` varchar(50) NOT NULL COMMENT '장바구니 ID',
  `PRODUCT_ID` int(11) NOT NULL COMMENT '상품 ID',
  `PRODUCT_OPTION_ID` varchar(50) DEFAULT NULL COMMENT '상품 옵션 ID',
  `QUANTITY` int(11) NOT NULL COMMENT '수량',
  `UPDATED_DATE` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일',
  PRIMARY KEY (`CART_ITEM_ID`),
  KEY `FK_CART_ITEM_PRODUCT` (`PRODUCT_ID`),
  KEY `FK_CART_ITEM_CART` (`CART_ID`),
  KEY `idx_cart_item_product_id` (`PRODUCT_ID`),
  KEY `idx_cart_item_cart_id_product_id` (`CART_ID`,`PRODUCT_ID`),
  CONSTRAINT `FK_CART_ITEM_CART` FOREIGN KEY (`CART_ID`) REFERENCES `tb_cart` (`CART_ID`),
  CONSTRAINT `fk_cart_item_product` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `tb_product` (`PRODUCT_ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='장바구니 상품 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_cart_item`
--

LOCK TABLES `tb_cart_item` WRITE;
/*!40000 ALTER TABLE `tb_cart_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_cart_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_category`
--

DROP TABLE IF EXISTS `tb_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_category` (
  `CATEGORY_ID` int(11) NOT NULL AUTO_INCREMENT,
  `parent_category_id` int(11) DEFAULT NULL COMMENT '상위 카테고리 ID',
  `name` varchar(255) NOT NULL COMMENT '카테고리명',
  `category_level` int(11) DEFAULT 1 COMMENT '카테고리 레벨',
  `category_display_order` int(11) DEFAULT NULL COMMENT '표시 순서',
  `category_use_yn` char(1) DEFAULT 'Y' COMMENT '사용 여부',
  `created_date` datetime DEFAULT current_timestamp() COMMENT '생성일',
  `updated_date` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일',
  `category_icon` varchar(255) DEFAULT NULL,
  `icon_url` varchar(500) DEFAULT NULL COMMENT '아이콘 URL',
  `icon_type` enum('svg','png','jpg') DEFAULT 'svg' COMMENT '아이콘 타입',
  PRIMARY KEY (`CATEGORY_ID`),
  KEY `parent_category_id` (`parent_category_id`),
  CONSTRAINT `tb_category_ibfk_1` FOREIGN KEY (`parent_category_id`) REFERENCES `tb_category` (`CATEGORY_ID`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=905 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_category`
--

LOCK TABLES `tb_category` WRITE;
/*!40000 ALTER TABLE `tb_category` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_delivery`
--

DROP TABLE IF EXISTS `tb_delivery`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_delivery` (
  `DELIVERY_ID` varchar(50) NOT NULL COMMENT '배송 ID',
  `ORDER_ID` varchar(50) NOT NULL COMMENT '주문 ID',
  `DELIVERY_COMPANY` varchar(100) DEFAULT NULL COMMENT '배송 업체',
  `DELIVERY_STATUS` varchar(20) NOT NULL COMMENT '배송 상태',
  `DELIVERY_TRACKING_NUMBER` varchar(100) DEFAULT NULL COMMENT '송장 번호',
  `DELIVERY_COMPLETED_DATE` datetime DEFAULT NULL COMMENT '배송 완료일',
  `DELIVERY_RECIPIENT_NAME` varchar(100) DEFAULT NULL COMMENT '수령인 이름',
  `DELIVERY_RECIPIENT_PHONE` varchar(20) DEFAULT NULL COMMENT '수령인 전화번호',
  `DELIVERY_ZIPCODE` varchar(10) DEFAULT NULL COMMENT '우편번호',
  `DELIVERY_ADDRESS_DETAIL` varchar(500) DEFAULT NULL COMMENT '상세 주소',
  `DELIVERY_MEMO` text DEFAULT NULL COMMENT '배송 메모',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  `UPDATED_DATE` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일',
  PRIMARY KEY (`DELIVERY_ID`),
  KEY `IDX_DELIVERY_ORDER_ID` (`ORDER_ID`) USING BTREE,
  CONSTRAINT `FK_DELIVERY_ORDER` FOREIGN KEY (`ORDER_ID`) REFERENCES `tb_order` (`ORDER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='배송 정보 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_delivery`
--

LOCK TABLES `tb_delivery` WRITE;
/*!40000 ALTER TABLE `tb_delivery` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_delivery` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_host`
--

DROP TABLE IF EXISTS `tb_host`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_host` (
  `HOST_ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '호스트 고유 ID',
  `USER_ID` varchar(50) NOT NULL COMMENT '회원 ID (FK)',
  `BUSINESS_NO` varchar(30) DEFAULT NULL COMMENT '사업자 번호',
  `BANK_NAME` varchar(30) DEFAULT NULL COMMENT '은행명',
  `ACCOUNT_NO` varchar(50) DEFAULT NULL COMMENT '계좌번호',
  `CHANNEL_NAME` varchar(50) DEFAULT NULL COMMENT '라이브 방송 채널명',
  `INTRO` text DEFAULT NULL COMMENT '소개글',
  `STATUS` varchar(20) DEFAULT 'PENDING' COMMENT '승인 상태(PENDING, APPROVED 등)',
  `APPROVED_YN` char(1) DEFAULT 'N' COMMENT '관리자 승인 여부 (Y/N)',
  PRIMARY KEY (`HOST_ID`),
  KEY `USER_ID` (`USER_ID`) USING BTREE,
  CONSTRAINT `fk_host_user` FOREIGN KEY (`USER_ID`) REFERENCES `tb_member` (`USER_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='호스트 전용 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_host`
--

LOCK TABLES `tb_host` WRITE;
/*!40000 ALTER TABLE `tb_host` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_host` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_inventory`
--

DROP TABLE IF EXISTS `tb_inventory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_inventory` (
  `INVENTORY_ID` varchar(50) NOT NULL COMMENT '재고 ID',
  `PRODUCT_ID` int(11) NOT NULL COMMENT '상품 ID',
  `INVENTORY_QUANTITY` int(11) NOT NULL DEFAULT 0 COMMENT '재고 수량',
  `INVENTORY_SAFETY_STOCK` int(11) DEFAULT 0 COMMENT '안전 재고',
  `INVENTORY_MAX_STOCK` int(11) DEFAULT NULL COMMENT '최대 재고',
  `INVENTORY_PENDING_IN` int(11) DEFAULT 0 COMMENT '입고 대기',
  `INVENTORY_PENDING_OUT` int(11) DEFAULT 0 COMMENT '출고 대기',
  `INVENTORY_LAST_OUT_DATE` datetime DEFAULT NULL COMMENT '마지막 출고일',
  `WAREHOUSE_MANAGER_NAME` varchar(100) DEFAULT NULL COMMENT '창고 관리자명',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  `UPDATED_DATE` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일',
  PRIMARY KEY (`INVENTORY_ID`),
  KEY `IDX_INVENTORY_PRODUCT_ID` (`PRODUCT_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='재고 관리 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_inventory`
--

LOCK TABLES `tb_inventory` WRITE;
/*!40000 ALTER TABLE `tb_inventory` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_inventory` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_inventory_history`
--

DROP TABLE IF EXISTS `tb_inventory_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_inventory_history` (
  `INVENTORY_HISTORY_ID` varchar(50) NOT NULL COMMENT '재고 이력 ID',
  `INVENTORY_ID` varchar(50) NOT NULL COMMENT '재고 ID',
  `INVENTORY_HISTORY_TYPE` varchar(20) NOT NULL COMMENT '재고 이력 유형',
  `INVENTORY_HISTORY_QUANTITY` int(11) NOT NULL COMMENT '재고 이력 수량',
  `INVENTORY_HISTORY_AFTER` int(11) NOT NULL COMMENT '변경 후 재고',
  `WAREHOUSE_MANAGER_NAME` varchar(100) DEFAULT NULL COMMENT '창고 관리자명',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  PRIMARY KEY (`INVENTORY_HISTORY_ID`),
  KEY `IDX_INVENTORY_HISTORY_INVENTORY_ID` (`INVENTORY_ID`) USING BTREE,
  CONSTRAINT `FK_INVENTORY_HISTORY_INVENTORY` FOREIGN KEY (`INVENTORY_ID`) REFERENCES `tb_inventory` (`INVENTORY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='재고 이력 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_inventory_history`
--

LOCK TABLES `tb_inventory_history` WRITE;
/*!40000 ALTER TABLE `tb_inventory_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_inventory_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_live_broadcast_chats`
--

DROP TABLE IF EXISTS `tb_live_broadcast_chats`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_live_broadcast_chats` (
  `chat_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '채팅 PK',
  `broadcast_id` bigint(20) NOT NULL COMMENT '해당 방송 ID',
  `user_id` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `message` text NOT NULL COMMENT '채팅 메시지 내용',
  `message_type` enum('text','error','sticker','system','notice','product_link','pinned') DEFAULT 'text' COMMENT '메시지 타입 (text, emoji, sticker, system, product_link)',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '메시지 삭제 여부',
  `is_blurred` tinyint(1) DEFAULT 0 COMMENT '메시지 블러 여부',
  `is_pinned` tinyint(1) DEFAULT 0 COMMENT '고정 메시지 여부',
  `product_id` bigint(20) DEFAULT NULL COMMENT '상품 링크 메시지일 경우 상품 ID (tb_product 참조)',
  `created_at` timestamp NULL DEFAULT current_timestamp() COMMENT '생성 시간',
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정 시간',
  PRIMARY KEY (`chat_id`),
  KEY `idx_broadcast_id` (`broadcast_id`),
  KEY `idx_broadcast_id_created_at` (`broadcast_id`,`created_at`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_message_type` (`message_type`),
  KEY `idx_is_blurred` (`is_blurred`),
  CONSTRAINT `fk_live_broadcast_chats_broadcast` FOREIGN KEY (`broadcast_id`) REFERENCES `tb_live_broadcasts` (`broadcast_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_live_broadcast_chats_user` FOREIGN KEY (`user_id`) REFERENCES `tb_member` (`USER_ID`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=429 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='라이브 방송 채팅 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_live_broadcast_chats`
--

LOCK TABLES `tb_live_broadcast_chats` WRITE;
/*!40000 ALTER TABLE `tb_live_broadcast_chats` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_live_broadcast_chats` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_live_broadcast_notifications`
--

DROP TABLE IF EXISTS `tb_live_broadcast_notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_live_broadcast_notifications` (
  `notification_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `broadcast_id` bigint(20) NOT NULL,
  `user_id` varchar(255) NOT NULL,
  `type` varchar(30) NOT NULL,
  `title` varchar(255) NOT NULL,
  `message` text NOT NULL,
  `is_read` tinyint(1) DEFAULT 0,
  `is_sent` tinyint(1) DEFAULT 0,
  `priority` varchar(10) DEFAULT 'normal',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `sent_at` timestamp NULL DEFAULT NULL,
  `read_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`notification_id`),
  KEY `idx_broadcast_id` (`broadcast_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_is_read` (`is_read`),
  CONSTRAINT `tb_live_broadcast_notifications_ibfk_1` FOREIGN KEY (`broadcast_id`) REFERENCES `tb_live_broadcasts` (`broadcast_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_live_broadcast_notifications`
--

LOCK TABLES `tb_live_broadcast_notifications` WRITE;
/*!40000 ALTER TABLE `tb_live_broadcast_notifications` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_live_broadcast_notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_live_broadcast_viewers`
--

DROP TABLE IF EXISTS `tb_live_broadcast_viewers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_live_broadcast_viewers` (
  `viewer_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '시청자 고유 ID',
  `broadcast_id` bigint(20) NOT NULL COMMENT '해당 방송 ID',
  `user_id` varchar(50) DEFAULT NULL COMMENT '시청자 ID (NULL이면 익명 시청자)',
  `username` varchar(50) DEFAULT NULL COMMENT '시청자명',
  `ip_address` varchar(45) DEFAULT NULL COMMENT 'IPv4/IPv6 주소',
  `user_agent` text DEFAULT NULL COMMENT '브라우저 정보',
  `device_type` varchar(20) DEFAULT 'desktop' COMMENT '디바이스 타입 (desktop, mobile, tablet, tv)',
  `joined_at` timestamp NULL DEFAULT current_timestamp() COMMENT '입장 시간',
  `left_at` timestamp NULL DEFAULT NULL COMMENT '퇴장 시간',
  `watch_duration` int(11) DEFAULT 0 COMMENT '시청 시간 (초)',
  `is_active` tinyint(1) DEFAULT 1 COMMENT '현재 시청 중 여부',
  `last_activity_at` timestamp NULL DEFAULT current_timestamp() COMMENT '마지막 활동 시간',
  PRIMARY KEY (`viewer_id`),
  KEY `idx_broadcast_id` (`broadcast_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_joined_at` (`joined_at`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_last_activity_at` (`last_activity_at`),
  CONSTRAINT `fk_viewer_broadcast` FOREIGN KEY (`broadcast_id`) REFERENCES `tb_live_broadcasts` (`broadcast_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_viewer_user` FOREIGN KEY (`user_id`) REFERENCES `tb_member` (`USER_ID`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='라이브 방송 시청자 기록 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_live_broadcast_viewers`
--

LOCK TABLES `tb_live_broadcast_viewers` WRITE;
/*!40000 ALTER TABLE `tb_live_broadcast_viewers` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_live_broadcast_viewers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_live_broadcasts`
--

DROP TABLE IF EXISTS `tb_live_broadcasts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_live_broadcasts` (
  `broadcast_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '라이브 방송 고유 ID',
  `broadcaster_id` varchar(50) NOT NULL COMMENT '방송자 ID',
  `title` varchar(255) NOT NULL COMMENT '방송 제목',
  `description` text DEFAULT NULL COMMENT '방송 설명',
  `broadcast_status` varchar(20) NOT NULL DEFAULT 'scheduled' COMMENT '방송 상태 (scheduled, starting, live, paused, ended, cancelled)',
  `scheduled_start_time` timestamp NOT NULL COMMENT '예정 시작 시간',
  `scheduled_end_time` timestamp NULL DEFAULT NULL COMMENT '예정 종료 시간',
  `actual_start_time` timestamp NULL DEFAULT NULL COMMENT '실제 시작 시간',
  `actual_end_time` timestamp NULL DEFAULT NULL COMMENT '실제 종료 시간',
  `is_public` tinyint(1) NOT NULL DEFAULT 1 COMMENT '공개 여부',
  `max_viewers` int(11) DEFAULT 0 COMMENT '최대 시청자 수 (0은 무제한)',
  `current_viewers` int(11) DEFAULT 0 COMMENT '현재 시청자 수',
  `total_viewers` int(11) DEFAULT 0 COMMENT '총 시청자 수',
  `peak_viewers` int(11) DEFAULT 0 COMMENT '최대 동시 시청자 수',
  `like_count` int(11) DEFAULT 0 COMMENT '좋아요 수',
  `thumbnail_url` varchar(500) DEFAULT NULL COMMENT '썸네일 이미지 URL',
  `stream_url` varchar(500) DEFAULT NULL COMMENT '스트림 URL',
  `category_id` int(11) DEFAULT NULL COMMENT '카테고리 ID (tb_category 참조)',
  `tags` varchar(500) DEFAULT NULL COMMENT '태그 목록 (콤마 구분)',
  `created_at` timestamp NULL DEFAULT current_timestamp() COMMENT '생성일시',
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일시',
  `stream_key` varchar(500) DEFAULT NULL COMMENT '스트림 KEY',
  `video_url` varchar(600) DEFAULT NULL COMMENT '다시보기 영상 링크',
  `obs_host` varchar(100) DEFAULT NULL COMMENT 'OBS 설치된 PC IP',
  `obs_port` int(10) DEFAULT NULL COMMENT 'OBS webSocket 포트번호',
  `obs_password` varchar(500) DEFAULT NULL COMMENT 'OBS webSocket 비밀번호',
  `nginx_host` varchar(100) DEFAULT NULL COMMENT 'DOCKER 서버 IP',
  PRIMARY KEY (`broadcast_id`),
  KEY `idx_broadcaster_id` (`broadcaster_id`),
  KEY `idx_broadcast_status` (`broadcast_status`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_scheduled_start_time` (`scheduled_start_time`),
  KEY `idx_actual_start_time` (`actual_start_time`),
  KEY `idx_current_viewers` (`current_viewers`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_broadcaster` FOREIGN KEY (`broadcaster_id`) REFERENCES `tb_member` (`USER_ID`) ON DELETE CASCADE,
  CONSTRAINT `fk_category` FOREIGN KEY (`category_id`) REFERENCES `tb_category` (`CATEGORY_ID`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=114 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='라이브 방송 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_live_broadcasts`
--

LOCK TABLES `tb_live_broadcasts` WRITE;
/*!40000 ALTER TABLE `tb_live_broadcasts` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_live_broadcasts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_member`
--

DROP TABLE IF EXISTS `tb_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_member` (
  `USER_ID` varchar(50) NOT NULL COMMENT '사용자 ID',
  `PASSWORD` varchar(255) NOT NULL COMMENT '비밀번호',
  `NAME` varchar(100) NOT NULL COMMENT '이름',
  `EMAIL` varchar(100) NOT NULL COMMENT '이메일',
  `PHONE` varchar(20) DEFAULT NULL COMMENT '전화번호',
  `ZIPCODE` varchar(10) DEFAULT NULL COMMENT '우편번호',
  `ADDRESS` varchar(500) DEFAULT NULL COMMENT '주소',
  `BIRTH_DATE` date DEFAULT NULL COMMENT '생년월일',
  `GENDER` char(1) DEFAULT NULL COMMENT '성별',
  `SUCCESSION_YN` varchar(1) DEFAULT 'N',
  `BLACKLISTED` varchar(1) DEFAULT 'N',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  `SESSION_DATE` datetime DEFAULT NULL COMMENT '세션일',
  `LOGIN_FAIL_CNT` int(11) DEFAULT 0 COMMENT '로그인 실패 횟수',
  `STATUS` varchar(20) DEFAULT 'Y' COMMENT '상태',
  `LAST_LOGIN` datetime DEFAULT NULL COMMENT '마지막 로그인',
  `MARKETING_AGREE` varchar(1) DEFAULT 'N',
  `SOCIAL_ID` varchar(100) DEFAULT NULL COMMENT '소셜 ID',
  `MARKETING_AGENT` varchar(100) DEFAULT NULL COMMENT '마케팅 에이전트',
  `GRADE_ID` varchar(20) DEFAULT NULL COMMENT '등급 ID',
  `UPDATED_DATE` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일',
  `MYADDRESS` varchar(200) DEFAULT NULL COMMENT '상세주소',
  `SECESSION_YN` varchar(1) DEFAULT 'N',
  `SECESSION_DATE` date DEFAULT NULL COMMENT '탈퇴 날짜',
  `PROFILE_IMG` varchar(255) DEFAULT NULL COMMENT '프로필 이미지 URL',
  `SOCIAL_TYPE` varchar(50) DEFAULT NULL COMMENT '소셜 로그인 타입',
  `nickname` varchar(100) DEFAULT NULL COMMENT '별명',
  PRIMARY KEY (`USER_ID`),
  UNIQUE KEY `nickname` (`nickname`),
  KEY `IDX_MEMBER_GRADE_ID` (`GRADE_ID`) USING BTREE,
  CONSTRAINT `FK_MEMBER_GRADE` FOREIGN KEY (`GRADE_ID`) REFERENCES `tb_member_grade` (`GRADE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='회원 정보 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_member`
--

LOCK TABLES `tb_member` WRITE;
/*!40000 ALTER TABLE `tb_member` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_member_grade`
--

DROP TABLE IF EXISTS `tb_member_grade`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_member_grade` (
  `GRADE_ID` varchar(20) NOT NULL COMMENT '등급 ID',
  `GRADE_NAME` varchar(50) NOT NULL COMMENT '등급명',
  `GRADE_MIN_AMOUNT` int(11) DEFAULT 0 COMMENT '등급 최소 금액',
  `GRADE_POINT_RATE` decimal(5,2) DEFAULT 0.00 COMMENT '포인트 적립률',
  `GRADE_DISCOUNT_RATE` decimal(5,2) DEFAULT 0.00 COMMENT '할인율',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  `UPDATED_DATE` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일',
  PRIMARY KEY (`GRADE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='회원 등급 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_member_grade`
--

LOCK TABLES `tb_member_grade` WRITE;
/*!40000 ALTER TABLE `tb_member_grade` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_member_grade` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_order`
--

DROP TABLE IF EXISTS `tb_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_order` (
  `ORDER_ID` varchar(50) NOT NULL COMMENT '주문 ID',
  `USER_ID` varchar(50) NOT NULL COMMENT '사용자 ID',
  `ORDER_DATE` datetime NOT NULL COMMENT '주문일',
  `order_status` varchar(20) NOT NULL,
  `PHONE` varchar(20) DEFAULT NULL COMMENT '전화번호',
  `EMAIL` varchar(100) DEFAULT NULL COMMENT '이메일',
  `RECIPIENT_NAME` varchar(100) DEFAULT NULL COMMENT '수령인',
  `RECIPIENT_PHONE` varchar(20) DEFAULT NULL COMMENT '수령인 전화번호',
  `ORDER_ZIPCODE` varchar(10) DEFAULT NULL COMMENT '주문 우편번호',
  `ORDER_ADDRESS_DETAIL` varchar(500) DEFAULT NULL COMMENT '주문 상세주소',
  `DELIVERY_MEMO` text DEFAULT NULL COMMENT '배송 메모',
  `TOTAL_PRICE` int(11) NOT NULL DEFAULT 0 COMMENT '총 주문상품 가격',
  `DELIVERY_FEE` int(11) DEFAULT 0 COMMENT '배송비',
  `DISCOUNT_AMOUNT` int(11) DEFAULT 0 COMMENT '할인 금액',
  `USED_POINT` int(11) DEFAULT 0 COMMENT '사용 포인트',
  `PAYMENT_METHOD` varchar(20) DEFAULT NULL COMMENT '결제 방법',
  `SAVED_POINT` int(11) DEFAULT 0 COMMENT '적립 포인트',
  `PAYMENT_METHOD_NAME` varchar(100) DEFAULT NULL COMMENT '결제 수단명',
  `SHIPPING_DATE` datetime DEFAULT NULL COMMENT '배송일',
  `ESTIMATED_DATE` datetime DEFAULT NULL COMMENT '예상 도착일',
  `TRACKING_NUMBER` varchar(100) DEFAULT NULL COMMENT '추적 번호',
  `DELIVERY_COMPANY` varchar(100) DEFAULT NULL COMMENT '배송 업체',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  `UPDATED_DATE` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일',
  `COUPON_ID` varchar(50) DEFAULT NULL COMMENT '사용된 쿠폰 ID',
  `PROMOTION_NAME` varchar(100) DEFAULT NULL COMMENT '프로모션 이름',
  `ORIGINAL_TOTAL_PRICE` int(11) DEFAULT 0 COMMENT '할인 전 총액',
  `FINAL_PAYMENT_AMOUNT` int(11) DEFAULT 0 COMMENT '실제 결제 금액',
  `DISCOUNT_TYPE` varchar(20) DEFAULT NULL COMMENT '할인 방식',
  `ORDER_MEMO` text DEFAULT NULL COMMENT '주문 관리메모',
  `HOST_ID` bigint(20) DEFAULT NULL COMMENT '호스트 ID (FK)',
  PRIMARY KEY (`ORDER_ID`),
  KEY `IDX_ORDER_ORDER_DATE` (`ORDER_DATE`) USING BTREE,
  KEY `IDX_ORDER_USER_ID` (`USER_ID`) USING BTREE,
  KEY `FK_ORDER_HOST` (`HOST_ID`),
  CONSTRAINT `FK_ORDER_HOST` FOREIGN KEY (`HOST_ID`) REFERENCES `tb_host` (`HOST_ID`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='주문 정보 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_order`
--

LOCK TABLES `tb_order` WRITE;
/*!40000 ALTER TABLE `tb_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_order_cancels`
--

DROP TABLE IF EXISTS `tb_order_cancels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_order_cancels` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID (PK)',
  `order_id` varchar(255) NOT NULL,
  `user_id` varchar(255) NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `detail` varchar(255) DEFAULT NULL,
  `refund_amount` int(11) NOT NULL DEFAULT 0 COMMENT '환불 금액',
  `refund_status` varchar(255) NOT NULL,
  `payment_id` varchar(255) DEFAULT NULL,
  `payment_cancel_id` varchar(255) DEFAULT NULL,
  `cancel_date` datetime NOT NULL COMMENT '취소 날짜',
  `created_at` datetime NOT NULL COMMENT '생성일',
  `updated_at` datetime DEFAULT NULL COMMENT '수정일',
  PRIMARY KEY (`id`),
  KEY `IDX_ORDER_CANCEL_ORDER_ID` (`order_id`),
  KEY `IDX_ORDER_CANCEL_USER_ID` (`user_id`),
  KEY `IDX_ORDER_CANCEL_STATUS` (`refund_status`),
  KEY `IDX_ORDER_CANCEL_DATE` (`cancel_date`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='주문 취소 정보 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_order_cancels`
--

LOCK TABLES `tb_order_cancels` WRITE;
/*!40000 ALTER TABLE `tb_order_cancels` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_order_cancels` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_order_item`
--

DROP TABLE IF EXISTS `tb_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_order_item` (
  `ORDER_ITEM_ID` varchar(50) NOT NULL COMMENT '주문 상품 ID',
  `ORDER_ID` varchar(50) NOT NULL COMMENT '주문 ID',
  `PRODUCT_ID` int(11) NOT NULL COMMENT '상품 ID',
  `NAME` varchar(200) NOT NULL COMMENT '상품명',
  `QUANTITY` int(11) NOT NULL DEFAULT 0 COMMENT '수량',
  `STATUS` varchar(20) NOT NULL COMMENT '상태',
  `TOTAL_PRICE` int(11) NOT NULL DEFAULT 0 COMMENT '해당 상품 총 가격',
  `DELIVERY_FEE` int(11) DEFAULT 0 COMMENT '배송비',
  `IMAGE_URL` varchar(500) DEFAULT NULL COMMENT '이미지 URL',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  `UPDATED_DATE` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일',
  `ORIGINAL_PRICE` int(11) DEFAULT 0 COMMENT '원래 상품 가격',
  `DISCOUNT_AMOUNT` int(11) DEFAULT 0 COMMENT '할인 금액',
  `DISCOUNT_RATE` int(11) DEFAULT 0 COMMENT '할인율',
  `COUPON_APPLIED` tinyint(1) DEFAULT 0 COMMENT '쿠폰 적용 여부',
  `POINT_EARNED` int(11) DEFAULT 0 COMMENT '적립 포인트',
  PRIMARY KEY (`ORDER_ITEM_ID`),
  KEY `FK_ORDER_ITEM_PRODUCT` (`PRODUCT_ID`) USING BTREE,
  KEY `IDX_ORDER_ITEM_ORDER_ID` (`ORDER_ID`) USING BTREE,
  CONSTRAINT `FK_ORDER_ITEM_ORDER` FOREIGN KEY (`ORDER_ID`) REFERENCES `tb_order` (`ORDER_ID`),
  CONSTRAINT `FK_ORDER_ITEM_PRODUCT` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `tb_product` (`PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='주문 상품 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_order_item`
--

LOCK TABLES `tb_order_item` WRITE;
/*!40000 ALTER TABLE `tb_order_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_order_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_payment`
--

DROP TABLE IF EXISTS `tb_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_payment` (
  `PAYMENT_ID` varchar(50) NOT NULL COMMENT '결제 ID',
  `ORDER_ID` varchar(50) NOT NULL COMMENT '주문 ID',
  `invoice_po_id` varchar(100) DEFAULT NULL,
  `PAYMENT_AMOUNT` int(11) NOT NULL COMMENT '결제 금액',
  `PAYMENT_STATUS` varchar(20) NOT NULL COMMENT '결제 상태',
  `payment_method` varchar(50) DEFAULT NULL,
  `bank_name` varchar(100) DEFAULT NULL,
  `card_name` varchar(100) DEFAULT NULL,
  `PAYMENT_SECOND_AMOUNT` int(11) DEFAULT NULL COMMENT '두 번째 결제 금액',
  `PAYMENT_PC_NAME` varchar(100) DEFAULT NULL COMMENT '결제 PC명',
  `PAYMENT_CASH_NAME` varchar(100) DEFAULT NULL COMMENT '현금 결제명',
  `payment_approval_number` varchar(100) DEFAULT NULL,
  `PAYMENT_INSTALLMENT_NUMBER` int(11) DEFAULT NULL COMMENT '할부 개월',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  `UPDATED_DATE` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일',
  PRIMARY KEY (`PAYMENT_ID`),
  KEY `IDX_PAYMENT_ORDER_ID` (`ORDER_ID`) USING BTREE,
  CONSTRAINT `FK_PAYMENT_ORDER` FOREIGN KEY (`ORDER_ID`) REFERENCES `tb_order` (`ORDER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='결제 정보 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_payment`
--

LOCK TABLES `tb_payment` WRITE;
/*!40000 ALTER TABLE `tb_payment` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_payment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_product`
--

DROP TABLE IF EXISTS `tb_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_product` (
  `PRODUCT_ID` int(11) NOT NULL AUTO_INCREMENT,
  `NAME` varchar(200) NOT NULL COMMENT '상품명',
  `PRICE` int(11) NOT NULL COMMENT '가격',
  `SALE_PRICE` int(11) NOT NULL COMMENT '판매가',
  `PRODUCT_DESCRIPTION` mediumtext DEFAULT NULL,
  `PRODUCT_SHORT_DESCRIPTION` varchar(500) DEFAULT NULL COMMENT '간단 설명',
  `PRODUCT_STATUS` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '상품 상태',
  `PRODUCT_RATING` decimal(3,2) DEFAULT 0.00 COMMENT '평점',
  `PRODUCT_REVIEW_COUNT` int(11) DEFAULT 0 COMMENT '리뷰 수',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  `UPDATED_DATE` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일',
  `MAIN_IMAGE` varchar(500) DEFAULT NULL COMMENT '메인 이미지',
  `VIEW_COUNT` int(11) DEFAULT 0 COMMENT '조회수',
  `STOCK` int(11) DEFAULT 0 COMMENT '상품 재고',
  `HOST_ID` bigint(20) NOT NULL,
  `category_id` int(11) NOT NULL COMMENT '카테고리 ID',
  `display_yn` char(1) NOT NULL DEFAULT 'Y' COMMENT '진열여부(Y:진열, N:비진열)',
  PRIMARY KEY (`PRODUCT_ID`),
  KEY `fk_product_host` (`HOST_ID`),
  KEY `FK_PRODUCT_CATEGORY` (`category_id`),
  CONSTRAINT `FK_PRODUCT_CATEGORY` FOREIGN KEY (`category_id`) REFERENCES `tb_category` (`CATEGORY_ID`) ON DELETE CASCADE,
  CONSTRAINT `fk_product_host` FOREIGN KEY (`HOST_ID`) REFERENCES `tb_host` (`HOST_ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=52 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='상품 정보 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_product`
--

LOCK TABLES `tb_product` WRITE;
/*!40000 ALTER TABLE `tb_product` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_product_image`
--

DROP TABLE IF EXISTS `tb_product_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_product_image` (
  `IMAGE_ID` int(11) NOT NULL AUTO_INCREMENT COMMENT '이미지 ID',
  `PRODUCT_ID` int(11) DEFAULT NULL,
  `IMAGE_URL` varchar(500) DEFAULT NULL COMMENT '이미지 URL (웹 접근용)',
  `FILE_PATH` varchar(500) DEFAULT NULL COMMENT '로컬 파일 경로',
  `FILE_NAME` varchar(200) DEFAULT NULL COMMENT '원본 파일명',
  `FILE_SIZE` bigint(20) DEFAULT NULL COMMENT '파일 크기(bytes)',
  `STORAGE_TYPE` varchar(20) DEFAULT 'LOCAL' COMMENT '저장소 타입(LOCAL/S3/URL)',
  `IMAGE_SEQ` int(11) NOT NULL DEFAULT 1 COMMENT '이미지 순서',
  `is_main_image` char(1) DEFAULT NULL,
  `IMAGE_ALT` varchar(200) DEFAULT NULL COMMENT '이미지 대체 텍스트',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  `UPDATED_DATE` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일',
  PRIMARY KEY (`IMAGE_ID`),
  KEY `IDX_PRODUCT_IMAGE_PRODUCT_ID` (`PRODUCT_ID`),
  KEY `IDX_PRODUCT_IMAGE_SEQ` (`PRODUCT_ID`,`IMAGE_SEQ`),
  KEY `IDX_PRODUCT_MAIN_IMAGE` (`PRODUCT_ID`,`is_main_image`),
  CONSTRAINT `fk_product_image_product` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `tb_product` (`PRODUCT_ID`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_storage_type` CHECK (`STORAGE_TYPE` in ('LOCAL','S3','URL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='상품 이미지 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_product_image`
--

LOCK TABLES `tb_product_image` WRITE;
/*!40000 ALTER TABLE `tb_product_image` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_product_image` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_product_options`
--

DROP TABLE IF EXISTS `tb_product_options`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_product_options` (
  `OPTION_ID` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `PRODUCT_ID` int(11) DEFAULT NULL,
  `OPTION_NAME` varchar(100) NOT NULL,
  `SALE_PRICE` int(10) unsigned DEFAULT 0,
  `STOCK` int(10) unsigned DEFAULT 0,
  `STATUS` enum('판매중','품절','판매중지') DEFAULT '판매중',
  `CREATED_DATE` datetime DEFAULT current_timestamp(),
  `UPDATED_DATE` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`OPTION_ID`),
  KEY `fk_product_options_product` (`PRODUCT_ID`),
  CONSTRAINT `fk_product_options_product` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `tb_product` (`PRODUCT_ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_product_options`
--

LOCK TABLES `tb_product_options` WRITE;
/*!40000 ALTER TABLE `tb_product_options` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_product_options` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_product_qna`
--

DROP TABLE IF EXISTS `tb_product_qna`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_product_qna` (
  `QNA_ID` varchar(50) NOT NULL COMMENT 'Q&A ID',
  `PRODUCT_ID` int(11) NOT NULL COMMENT '상품 ID',
  `USER_ID` varchar(50) DEFAULT NULL COMMENT '작성자 ID',
  `QNA_TYPE` varchar(20) DEFAULT NULL COMMENT 'Q&A 유형',
  `TITLE` varchar(200) NOT NULL COMMENT '제목',
  `CONTENT` text NOT NULL COMMENT '내용',
  `QNA_STATUS` varchar(20) DEFAULT 'WAITING' COMMENT 'Q&A 상태',
  `IS_SECRET` char(1) DEFAULT 'N' COMMENT '비밀글 여부',
  `VIEW_COUNT` int(11) DEFAULT 0 COMMENT '조회수',
  `ANSWER_DATE` datetime DEFAULT NULL COMMENT '답변일',
  `ANSWER_USER_ID` varchar(50) DEFAULT NULL COMMENT '답변자 ID',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  `UPDATED_DATE` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일',
  `AUTHOR_NAME` varchar(255) DEFAULT NULL,
  `AUTHOR_IP` varchar(45) DEFAULT NULL,
  `PASSWORD` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`QNA_ID`),
  KEY `IDX_PRODUCT_QNA_CREATED_DATE` (`CREATED_DATE`) USING BTREE,
  KEY `IDX_PRODUCT_QNA_PRODUCT_ID` (`PRODUCT_ID`) USING BTREE,
  KEY `IDX_PRODUCT_QNA_STATUS` (`QNA_STATUS`) USING BTREE,
  KEY `IDX_PRODUCT_QNA_USER_ID` (`USER_ID`) USING BTREE,
  KEY `fk_qna_answer_user` (`ANSWER_USER_ID`),
  CONSTRAINT `FK_INQUIRY_PRODUCT` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `tb_product` (`PRODUCT_ID`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_qna_answer_user` FOREIGN KEY (`ANSWER_USER_ID`) REFERENCES `tb_member` (`USER_ID`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_qna_user` FOREIGN KEY (`USER_ID`) REFERENCES `tb_member` (`USER_ID`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='상품 Q&A 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_product_qna`
--

LOCK TABLES `tb_product_qna` WRITE;
/*!40000 ALTER TABLE `tb_product_qna` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_product_qna` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_product_qna_answer`
--

DROP TABLE IF EXISTS `tb_product_qna_answer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_product_qna_answer` (
  `ANSWER_ID` int(11) NOT NULL AUTO_INCREMENT COMMENT '답변 ID',
  `QNA_ID` varchar(50) NOT NULL COMMENT 'Q&A ID',
  `USER_ID` varchar(50) DEFAULT NULL COMMENT '답변자 ID',
  `CONTENT` text NOT NULL COMMENT '답변 내용',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  `UPDATED_DATE` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일',
  `ANSWER_STATUS` varchar(20) DEFAULT 'ACTIVE' COMMENT '답변 상태',
  `IS_SELLER` char(1) DEFAULT 'Y' COMMENT '판매자 답변 여부',
  `AUTHOR_NAME` varchar(100) DEFAULT NULL COMMENT '답변자명',
  PRIMARY KEY (`ANSWER_ID`),
  KEY `IDX_PRODUCT_QNA_ANSWER_QNA_ID` (`QNA_ID`) USING BTREE,
  CONSTRAINT `FK_PRODUCT_QNA_ANSWER_QNA` FOREIGN KEY (`QNA_ID`) REFERENCES `tb_product_qna` (`QNA_ID`),
  CONSTRAINT `fk_qna_answer_qna_id` FOREIGN KEY (`QNA_ID`) REFERENCES `tb_product_qna` (`QNA_ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='상품 Q&A 답변 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_product_qna_answer`
--

LOCK TABLES `tb_product_qna_answer` WRITE;
/*!40000 ALTER TABLE `tb_product_qna_answer` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_product_qna_answer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_product_qna_file`
--

DROP TABLE IF EXISTS `tb_product_qna_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_product_qna_file` (
  `FILE_ID` varchar(50) NOT NULL COMMENT '파일 ID',
  `QNA_ID` varchar(50) NOT NULL COMMENT 'Q&A ID',
  `ORIGINAL_NAME` varchar(255) NOT NULL COMMENT '원본 파일명',
  `STORED_NAME` varchar(255) NOT NULL COMMENT '저장 파일명',
  `FILE_PATH` varchar(500) NOT NULL COMMENT '파일 경로',
  `FILE_SIZE` bigint(20) NOT NULL COMMENT '파일 크기',
  `FILE_EXTENSION` varchar(10) DEFAULT NULL COMMENT '파일 확장자',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  PRIMARY KEY (`FILE_ID`),
  KEY `FK_PRODUCT_QNA_FILE_QNA` (`QNA_ID`) USING BTREE,
  CONSTRAINT `FK_PRODUCT_QNA_FILE_QNA` FOREIGN KEY (`QNA_ID`) REFERENCES `tb_product_qna` (`QNA_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='상품 Q&A 첨부파일 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_product_qna_file`
--

LOCK TABLES `tb_product_qna_file` WRITE;
/*!40000 ALTER TABLE `tb_product_qna_file` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_product_qna_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_product_review`
--

DROP TABLE IF EXISTS `tb_product_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_product_review` (
  `REVIEW_ID` varchar(50) NOT NULL COMMENT '후기 ID',
  `PRODUCT_ID` int(11) NOT NULL COMMENT '상품 ID',
  `ORDER_ID` varchar(50) DEFAULT NULL COMMENT '주문 ID',
  `USER_ID` varchar(50) NOT NULL COMMENT '작성자 ID',
  `CONTENT` text NOT NULL COMMENT '후기 내용',
  `RATING` int(11) NOT NULL COMMENT '평점 1-5',
  `HELPFUL_COUNT` int(11) DEFAULT 0 COMMENT '도움됨 수',
  `IS_PHOTO` char(1) DEFAULT 'N' COMMENT '포토후기 여부',
  `IS_VERIFIED` char(1) DEFAULT 'N' COMMENT '구매확인 여부',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  `UPDATED_DATE` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일',
  `display_yn` char(1) NOT NULL DEFAULT 'Y' COMMENT '진열여부(Y:진열, N:비진열)',
  `TITLE` varchar(255) DEFAULT NULL,
  `REVIEW_STATUS` varchar(50) DEFAULT NULL,
  `VIEW_COUNT` int(11) DEFAULT 0,
  `AUTHOR_NAME` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`REVIEW_ID`),
  KEY `IDX_PRODUCT_REVIEW_CREATED_DATE` (`CREATED_DATE`),
  KEY `IDX_PRODUCT_REVIEW_PRODUCT_ID` (`PRODUCT_ID`),
  KEY `IDX_PRODUCT_REVIEW_RATING` (`RATING`),
  KEY `IDX_PRODUCT_REVIEW_USER_ID` (`USER_ID`),
  CONSTRAINT `FK_REVIEW_PRODUCT` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `tb_product` (`PRODUCT_ID`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_rating` CHECK (`RATING` >= 1 and `RATING` <= 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='상품 후기 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_product_review`
--

LOCK TABLES `tb_product_review` WRITE;
/*!40000 ALTER TABLE `tb_product_review` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_product_review` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_product_review_image`
--

DROP TABLE IF EXISTS `tb_product_review_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_product_review_image` (
  `IMAGE_ID` varchar(50) NOT NULL COMMENT '이미지 ID',
  `REVIEW_ID` varchar(50) NOT NULL COMMENT '후기 ID',
  `IMAGE_URL` varchar(500) NOT NULL COMMENT '이미지 URL',
  `IMAGE_PATH` varchar(500) NOT NULL COMMENT '이미지 경로',
  `ORIGINAL_NAME` varchar(255) NOT NULL COMMENT '원본 파일명',
  `IMAGE_SIZE` bigint(20) DEFAULT NULL COMMENT '이미지 크기',
  `IMAGE_SEQ` int(11) NOT NULL COMMENT '이미지 순서',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  PRIMARY KEY (`IMAGE_ID`),
  KEY `IDX_PRODUCT_REVIEW_IMAGE_REVIEW_ID` (`REVIEW_ID`) USING BTREE,
  CONSTRAINT `FK_PRODUCT_REVIEW_IMAGE_REVIEW` FOREIGN KEY (`REVIEW_ID`) REFERENCES `tb_product_review` (`REVIEW_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='상품 후기 이미지 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_product_review_image`
--

LOCK TABLES `tb_product_review_image` WRITE;
/*!40000 ALTER TABLE `tb_product_review_image` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_product_review_image` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_review_helpful`
--

DROP TABLE IF EXISTS `tb_review_helpful`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_review_helpful` (
  `HELPFUL_ID` varchar(50) NOT NULL COMMENT '도움됨 ID',
  `REVIEW_ID` varchar(50) NOT NULL COMMENT '후기 ID',
  `USER_ID` varchar(50) NOT NULL COMMENT '사용자 ID',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  PRIMARY KEY (`HELPFUL_ID`),
  KEY `IDX_REVIEW_HELPFUL_REVIEW_ID` (`REVIEW_ID`) USING BTREE,
  KEY `IDX_REVIEW_HELPFUL_USER_ID` (`USER_ID`) USING BTREE,
  CONSTRAINT `FK_REVIEW_HELPFUL_REVIEW` FOREIGN KEY (`REVIEW_ID`) REFERENCES `tb_product_review` (`REVIEW_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='후기 도움됨 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_review_helpful`
--

LOCK TABLES `tb_review_helpful` WRITE;
/*!40000 ALTER TABLE `tb_review_helpful` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_review_helpful` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_wish`
--

DROP TABLE IF EXISTS `tb_wish`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_wish` (
  `WISH_ID` varchar(50) NOT NULL COMMENT '위시리스트 ID',
  `USER_ID` varchar(50) NOT NULL COMMENT '사용자 ID',
  `PRODUCT_ID` int(11) NOT NULL COMMENT '상품 ID',
  `CREATED_DATE` datetime DEFAULT current_timestamp() COMMENT '생성일',
  PRIMARY KEY (`WISH_ID`),
  KEY `FK_WISH_MEMBER` (`USER_ID`) USING BTREE,
  KEY `FK_WISH_PRODUCT` (`PRODUCT_ID`) USING BTREE,
  CONSTRAINT `FK_WISH_MEMBER` FOREIGN KEY (`USER_ID`) REFERENCES `tb_member` (`USER_ID`),
  CONSTRAINT `FK_WISH_PRODUCT` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `tb_product` (`PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='위시리스트 테이블';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_wish`
--

LOCK TABLES `tb_wish` WRITE;
/*!40000 ALTER TABLE `tb_wish` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_wish` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tb_withdrawn_member`
--

DROP TABLE IF EXISTS `tb_withdrawn_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_withdrawn_member` (
  `WITHDRAWN_ID` varchar(30) NOT NULL COMMENT '탈퇴회원ID',
  `USER_ID` varchar(30) NOT NULL COMMENT '회원아이디',
  `WITHDRAWN_NAME` varchar(30) NOT NULL COMMENT '이름',
  `WITHDRAWN_EMAIL` varchar(50) NOT NULL COMMENT '이메일',
  `WITHDRAWN_PHONE` varchar(20) DEFAULT NULL COMMENT '전화번호',
  `GRADE_ID` varchar(20) NOT NULL COMMENT '등급ID',
  `WITHDRAWN_REASON` varchar(255) NOT NULL COMMENT '탈퇴사유',
  `WITHDRAWN_ORIGINAL_CREATED_DATE` date NOT NULL COMMENT '원래 가입일',
  `WITHDRAWN_WITHDRAWN_DATE` date NOT NULL COMMENT '탈퇴처리일',
  `WITHDRAWN_SECESSION_DATE` date NOT NULL COMMENT '탈퇴일',
  PRIMARY KEY (`WITHDRAWN_ID`),
  KEY `IDX_WITHDRAWN_DATE` (`WITHDRAWN_WITHDRAWN_DATE`) USING BTREE,
  KEY `IDX_WITHDRAWN_USER_ID` (`USER_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='탈퇴회원';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tb_withdrawn_member`
--

LOCK TABLES `tb_withdrawn_member` WRITE;
/*!40000 ALTER TABLE `tb_withdrawn_member` DISABLE KEYS */;
/*!40000 ALTER TABLE `tb_withdrawn_member` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-07-10 14:10:31
