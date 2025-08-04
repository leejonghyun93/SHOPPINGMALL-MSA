package org.kosa.userservice.userService.board;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.userservice.dto.board.PageRequestDto;
import org.kosa.userservice.dto.board.ProductQnaDto;
import org.kosa.userservice.mapper.ProductQnaMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQnaService {

    private final ProductQnaMapper productQnaMapper;

    // 페이징된 Q&A 목록 조회
    public List<ProductQnaDto> getPagedQnas(PageRequestDto requestDto) {
        log.info("서비스: getPagedQnas 호출됨 - {}", requestDto);

        try {
            if (requestDto != null) {
                requestDto.validate();
            }

            List<ProductQnaDto> result = productQnaMapper.selectPagedQnas(requestDto);
            log.info("서비스: Q&A DB 조회 결과 {} 건", result.size());
            return result;

        } catch (Exception e) {
            log.error("서비스: getPagedQnas DB 조회 중 에러 발생", e);
            return new ArrayList<>();
        }
    }

    // 컨트롤러용 오버로드 메서드
    public List<ProductQnaDto> getPagedQnas(int page, int size, String searchValue, String sortBy) {
        log.info("서비스: getPagedQnas 오버로드 메서드 호출됨");

        PageRequestDto requestDto = PageRequestDto.builder()
                .page(page)
                .size(size)
                .searchValue(searchValue)
                .sortBy(sortBy)
                .build();

        return getPagedQnas(requestDto);
    }

    // 상품별 Q&A 조회
    public List<ProductQnaDto> getProductQnas(Integer productId, int page, int size, String sortBy) {
        log.info("서비스: getProductQnas 호출됨 - productId: {}, page: {}, size: {}", productId, page, size);

        try {
            int startRow = (page - 1) * size;
            log.info("서비스: 계산된 startRow: {}", startRow);

            List<ProductQnaDto> result = productQnaMapper.selectProductQnasByIntId(
                    productId, startRow, size, sortBy);

            log.info("서비스: 상품 {} Q&A DB 조회 결과 {} 건", productId, result.size());

            if (!result.isEmpty()) {
                ProductQnaDto first = result.get(0);
                log.info("서비스: 첫 번째 Q&A - qnaId: {}, title: {}",
                        first.getQnaId(), first.getTitle());
            }

            return result;

        } catch (Exception e) {
            log.error("서비스: getProductQnas DB 조회 중 에러 발생 - productId: {}", productId, e);
            return new ArrayList<>();
        }
    }

    // Q&A 상세 조회 (조회수 증가 포함)
    public ProductQnaDto getQnaById(String qnaId, boolean increaseView) {
        log.info("서비스: getQnaById 호출됨 - qnaId: {}, increaseView: {}", qnaId, increaseView);

        try {
            if (increaseView) {
                productQnaMapper.increaseViewCount(qnaId);
                log.info("서비스: Q&A 조회수 증가 - qnaId: {}", qnaId);
            }

            ProductQnaDto result = productQnaMapper.selectQnaById(qnaId);
            log.info("서비스: Q&A 조회 결과 - {}", result != null ? "성공" : "없음");
            return result;

        } catch (Exception e) {
            log.error("서비스: getQnaById 중 에러 발생", e);
            return null;
        }
    }

    //  Q&A 등록 (구매 인증 포함)
    @Transactional
    public String createQna(ProductQnaDto qnaDto) {
        log.info("서비스: createQna 호출됨 - productId: {}, userId: {}, title: {}",
                qnaDto.getProductId(), qnaDto.getUserId(), qnaDto.getTitle());

        try {
            // Q&A ID 생성
            String qnaId = generateQnaId();
            qnaDto.setQnaId(qnaId);

            // 필수 필드 검증
            if (qnaDto.getUserId() == null || qnaDto.getUserId().trim().isEmpty()) {
                throw new RuntimeException("사용자 ID가 없습니다.");
            }

            if (qnaDto.getAuthorName() == null || qnaDto.getAuthorName().trim().isEmpty()) {
                throw new RuntimeException("작성자 이름이 없습니다.");
            }

            // 기본값 설정
            if (qnaDto.getQnaStatus() == null) {
                qnaDto.setQnaStatus("WAITING");
            }
            if (qnaDto.getIsSecret() == null) {
                qnaDto.setIsSecret("N");
            }
            if (qnaDto.getViewCount() == null) {
                qnaDto.setViewCount(0);
            }
            if (qnaDto.getQnaType() == null) {
                qnaDto.setQnaType("기타");
            }

            // 생성일, 수정일 설정
            LocalDateTime now = LocalDateTime.now();
            qnaDto.setCreatedDate(now);
            qnaDto.setUpdatedDate(now);

            log.info("서비스: Q&A 등록 준비 완료 - qnaId: {}, 작성자: {}",
                    qnaId, qnaDto.getAuthorName());

            // DB 삽입
            int result = productQnaMapper.insertQna(qnaDto);

            if (result > 0) {
                log.info("서비스: Q&A 등록 성공 - qnaId: {}", qnaId);
                return qnaId;
            } else {
                throw new RuntimeException("Q&A 등록에 실패했습니다.");
            }

        } catch (Exception e) {
            log.error("서비스: createQna 중 에러 발생", e);
            throw new RuntimeException("Q&A 등록 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    //  Q&A 수정
    @Transactional
    public boolean updateQna(ProductQnaDto qnaDto) {
        log.info("서비스: updateQna 호출됨 - qnaId: {}", qnaDto.getQnaId());

        try {
            // 수정일 설정
            qnaDto.setUpdatedDate(LocalDateTime.now());

            // DB 업데이트
            int result = productQnaMapper.updateQna(qnaDto);

            boolean success = result > 0;
            log.info("서비스: Q&A 수정 결과 - {}", success ? "성공" : "실패");
            return success;

        } catch (Exception e) {
            log.error("서비스: updateQna 중 에러 발생", e);
            return false;
        }
    }

    // Q&A 삭제
    @Transactional
    public boolean deleteQna(String qnaId) {
        log.info("서비스: deleteQna 호출됨 - qnaId: {}", qnaId);

        try {
            // Q&A 삭제
            int result = productQnaMapper.deleteQna(qnaId);

            boolean success = result > 0;
            log.info("서비스: Q&A 삭제 결과 - {}", success ? "성공" : "실패");
            return success;

        } catch (Exception e) {
            log.error("서비스: deleteQna 중 에러 발생", e);
            return false;
        }
    }

    // 회원 정보 조회
    public String getMemberNameByUserId(String userId) {
        log.info("서비스: getMemberNameByUserId 호출됨 - userId: {}", userId);

        try {
            String memberName = productQnaMapper.selectMemberNameByUserId(userId);
            log.info("서비스: 회원 이름 조회 결과 - userId: {}, name: {}", userId, memberName);
            return memberName;
        } catch (Exception e) {
            log.error("서비스: 회원 이름 조회 중 에러 발생", e);
            return null;
        }
    }

    // 구매 인증 확인
    public boolean verifyPurchase(String userId, Integer productId) {
        log.info("서비스: verifyPurchase 호출됨 - userId: {}, productId: {}", userId, productId);

        try {
            int count = productQnaMapper.checkPurchaseVerification(userId, productId);
            boolean isPurchased = count > 0;

            log.info("서비스: 구매 인증 결과 - userId: {}, productId: {}, 구매여부: {}",
                    userId, productId, isPurchased);

            return isPurchased;
        } catch (Exception e) {
            log.error("서비스: 구매 인증 확인 중 에러 발생", e);
            return false;
        }
    }

    // 상품별 Q&A 개수 조회
    public int getProductQnaCount(Integer productId) {
        try {
            int count = productQnaMapper.getProductQnaCountByIntId(productId);
            log.info("서비스: 상품 {} Q&A 개수: {}", productId, count);
            return count;
        } catch (Exception e) {
            log.error("서비스: 상품별 Q&A 개수 조회 중 에러 발생", e);
            return 0;
        }
    }

    // 전체 Q&A 개수 조회
    public int getTotalCount(String searchValue) {
        try {
            return productQnaMapper.getTotalCount(searchValue);
        } catch (Exception e) {
            log.error("서비스: 전체 Q&A 개수 조회 중 에러 발생", e);
            return 0;
        }
    }

    // 사용자별 Q&A 조회 (마이페이지용)
    public List<ProductQnaDto> getUserQnas(String userId, int page, int size) {
        log.info("서비스: getUserQnas 호출됨 - userId: {}, page: {}, size: {}", userId, page, size);

        try {
            int startRow = (page - 1) * size;
            List<ProductQnaDto> result = productQnaMapper.selectQnasByUserId(userId, startRow, size);
            log.info("서비스: 사용자 {} Q&A 조회 결과 {} 건", userId, result.size());
            return result;

        } catch (Exception e) {
            log.error("서비스: getUserQnas 중 에러 발생", e);
            return new ArrayList<>();
        }
    }

    // 사용자별 Q&A 개수
    public int getUserQnaCount(String userId) {
        try {
            int count = productQnaMapper.getUserQnaCount(userId);
            log.info("서비스: 사용자 {} Q&A 개수: {}", userId, count);
            return count;
        } catch (Exception e) {
            log.error("서비스: 사용자별 Q&A 개수 조회 중 에러 발생", e);
            return 0;
        }
    }

    // ID 생성 메서드
    private String generateQnaId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 1000);
        return "Q" + timestamp + String.format("%03d", random);
    }

    // 유틸리티 메서드들
    private String booleanToString(boolean value) {
        return value ? "Y" : "N";
    }

    private boolean stringToBoolean(String value) {
        return "Y".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
    }
}