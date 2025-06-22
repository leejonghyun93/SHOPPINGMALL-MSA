package org.kosa.userservice.userService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.userservice.dto.UserWithdrawalEvent;
import org.kosa.userservice.dto.WithdrawRequestDto;
import org.kosa.userservice.dto.WithdrawResponseDto;
import org.kosa.userservice.entity.Member;
import org.kosa.userservice.entity.WithdrawnMember;
import org.kosa.userservice.exception.AlreadyWithdrawnException;
import org.kosa.userservice.exception.InvalidPasswordException;
import org.kosa.userservice.exception.InvalidUserStatusException;
import org.kosa.userservice.exception.UserNotFoundException;
import org.kosa.userservice.repository.UserRepository;
import org.kosa.userservice.repository.WithdrawnMemberRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserWithdrawalService {

    private final UserRepository userRepository;
    private final WithdrawnMemberRepository withdrawnMemberRepository;
    private final PasswordEncoder passwordEncoder;

    // 특정 KafkaTemplate Bean 주입
    @Qualifier("withdrawalKafkaTemplate")
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.user-withdrawal}")
    private String userWithdrawalTopic;

    /**
     * 회원탈퇴 처리 (메인 로직)
     */
    public WithdrawResponseDto withdrawUser(WithdrawRequestDto request) {
        try {
            // 1. 사용자 검증
            Member member = validateUser(request.getUserId(), request.getPassword());

            // 2. 이미 탈퇴한 회원인지 확인
            validateNotAlreadyWithdrawn(request.getUserId());

            // 3. 탈퇴 회원 테이블에 데이터 저장
            String withdrawnId = saveWithdrawnMember(member, request);

            // 4. 기존 회원 테이블 상태 변경
            updateMemberStatus(member, request.getWithdrawalDate());

            // 5. 카프카 이벤트 발행
            publishWithdrawalEvent(member, withdrawnId, request);

            log.info("회원탈퇴 처리 완료: userId={}, withdrawnId={}",
                    request.getUserId(), withdrawnId);

            return WithdrawResponseDto.builder()
                    .success(true)
                    .message("회원탈퇴가 정상적으로 처리되었습니다.")
                    .withdrawnId(withdrawnId)
                    .build();

        } catch (Exception e) {
            log.error("회원탈퇴 처리 실패: userId={}, error={}",
                    request.getUserId(), e.getMessage(), e);

            return WithdrawResponseDto.builder()
                    .success(false)
                    .message("회원탈퇴 처리 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 사용자 검증
     */
    private Member validateUser(String userId, String password) {
        Member member = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 회원입니다."));

        if (!"ACTIVE".equals(member.getStatus())) {
            throw new InvalidUserStatusException("탈퇴 처리할 수 없는 회원 상태입니다.");
        }

        if (!"N".equals(member.getSecessionYn())) {
            throw new AlreadyWithdrawnException("이미 탈퇴한 회원입니다.");
        }

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new InvalidPasswordException("비밀번호가 일치하지 않습니다.");
        }

        return member;
    }

    /**
     * 이미 탈퇴한 회원인지 확인
     */
    private void validateNotAlreadyWithdrawn(String userId) {
        if (withdrawnMemberRepository.existsByUserId(userId)) {
            throw new AlreadyWithdrawnException("이미 탈퇴 처리된 회원입니다.");
        }
    }

    /**
     * 탈퇴 회원 테이블에 저장
     */
    private String saveWithdrawnMember(Member member, WithdrawRequestDto request) {
        String withdrawnId = generateWithdrawnId();

        WithdrawnMember withdrawnMember = WithdrawnMember.builder()
                .withdrawnId(withdrawnId)
                .userId(member.getUserId())
                .withdrawnName(member.getName())
                .withdrawnEmail(member.getEmail())
                .withdrawnPhone(member.getPhone())
                .gradeId(member.getMemberGrade() != null ? member.getMemberGrade().getGradeId() : null)
                .withdrawnReason(request.getWithdrawalReason())
                .withdrawnOriginalCreatedDate(member.getCreatedDate().toLocalDate())
                .withdrawnWithdrawnDate(LocalDate.now())
                .withdrawnSecessionDate(request.getWithdrawalDate())
                .build();

        withdrawnMemberRepository.save(withdrawnMember);
        return withdrawnId;
    }

    /**
     * 기존 회원 상태 변경
     */
    private void updateMemberStatus(Member member, LocalDate secessionDate) {
        member.setSecessionYn("Y");
        member.setSecessionDate(secessionDate);
        member.setStatus("WITHDRAWN");
        member.setUpdatedDate(LocalDateTime.now());

        userRepository.save(member);
    }

    /**
     * 카프카 이벤트 발행
     */
    private void publishWithdrawalEvent(Member member, String withdrawnId, WithdrawRequestDto request) {
        UserWithdrawalEvent event = UserWithdrawalEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(member.getUserId())
                .withdrawnId(withdrawnId)
                .name(member.getName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .gradeId(member.getMemberGrade() != null ? member.getMemberGrade().getGradeId() : null)
                .withdrawalReason(request.getWithdrawalReason())
                .originalCreatedDate(member.getCreatedDate().toLocalDate())
                .withdrawalDate(LocalDate.now())
                .secessionDate(request.getWithdrawalDate())
                .eventTimestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(userWithdrawalTopic, member.getUserId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("회원탈퇴 이벤트 발행 성공: userId={}, eventId={}",
                                member.getUserId(), event.getEventId());
                    } else {
                        log.error("회원탈퇴 이벤트 발행 실패: userId={}, error={}",
                                member.getUserId(), ex.getMessage(), ex);
                    }
                });
    }

    /**
     * 탈퇴 ID 생성
     */
    private String generateWithdrawnId() {
        String prefix = "WD";
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniquePart = String.format("%06d", System.currentTimeMillis() % 1000000);

        return prefix + datePart + uniquePart;
    }

    /**
     * 탈퇴 회원 조회
     */
    @Transactional(readOnly = true)
    public Optional<WithdrawnMember> getWithdrawnMember(String userId) {
        return withdrawnMemberRepository.findByUserId(userId);
    }

    /**
     * 기간별 탈퇴 통계
     */
    @Transactional(readOnly = true)
    public List<WithdrawnMember> getWithdrawnMembersByDateRange(LocalDate startDate, LocalDate endDate) {
        return withdrawnMemberRepository.findByWithdrawnWithdrawnDateBetween(startDate, endDate);
    }
}