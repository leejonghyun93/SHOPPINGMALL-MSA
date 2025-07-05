package org.kosa.userservice.userService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.userservice.dto.UserDto;
import org.kosa.userservice.entity.*;
import org.kosa.userservice.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WithdrawnUserRepository withdrawnUserRepository;
    private final UserGradeRepository userGradeRepository;

    @Transactional
    public Member saveMember(Member member) {
        log.info("회원 등록 시작 - userId: {}", member.getUserId());

        member.setPassword(passwordEncoder.encode(member.getPassword()));

        if (member.getMemberGrade() == null) {
            MemberGrade defaultGrade = getLowestGrade();
            member.setMemberGrade(defaultGrade);
            log.info("기본 등급({}) 설정 완료", defaultGrade.getGradeName());
        }

        if (member.getStatus() == null) {
            member.setStatus("Y");
        }
        if (member.getLoginFailCnt() == null) {
            member.setLoginFailCnt(0);
        }
        if (member.getGender() == null) {
            member.setGender("U");
        }
        if (member.getMarketingAgree() == null) {
            member.setMarketingAgree("N");
        }
        if (member.getSuccessionYn() == null) {
            member.setSuccessionYn("N");
        }
        if (member.getBlacklisted() == null) {
            member.setBlacklisted("N");
        }
        if (member.getSecessionYn() == null) {
            member.setSecessionYn("N");
        }

        Member savedMember = userRepository.save(member);
        log.info("회원 등록 완료 - userId: {}, 등급: {}",
                savedMember.getUserId(),
                savedMember.getMemberGrade().getGradeName());

        return savedMember;
    }

    private MemberGrade getLowestGrade() {
        return userGradeRepository.findTopByOrderByGradeMinAmountAsc()
                .orElseThrow(() -> new RuntimeException("기본 등급을 찾을 수 없습니다."));
    }

    @Transactional
    public void updateMemberGradeByPurchaseAmount(String userId, int totalPurchaseAmount) {
        log.info("등급 업데이트 확인 - userId: {}, 누적구매금액: {}", userId, totalPurchaseAmount);

        Member member = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        if (isSpecialGrade(member.getMemberGrade())) {
            log.info("특별 등급은 변경하지 않음 - userId: {}, 등급: {}",
                    userId, member.getMemberGrade().getGradeName());
            return;
        }

        Optional<MemberGrade> newGrade = userGradeRepository.findBestGradeByPurchaseAmount(totalPurchaseAmount);

        if (newGrade.isPresent() &&
                !newGrade.get().getGradeId().equals(member.getMemberGrade().getGradeId())) {

            String oldGradeName = member.getMemberGrade().getGradeName();
            member.setMemberGrade(newGrade.get());
            userRepository.save(member);

            log.info("등급 업그레이드 완료 - userId: {}, {} → {} (구매금액: {}원)",
                    userId, oldGradeName, newGrade.get().getGradeName(), totalPurchaseAmount);
        }
    }

    private boolean isSpecialGrade(MemberGrade grade) {
        String gradeName = grade.getGradeName().toUpperCase();
        return gradeName.contains("관리자") || gradeName.contains("ADMIN") ||
                gradeName.contains("호스트") || gradeName.contains("HOST");
    }

    private Optional<MemberGrade> findBestGradeByPurchaseAmount(int purchaseAmount) {
        return userGradeRepository.findBestGradeByPurchaseAmount(purchaseAmount);
    }

    public Optional<UserDto> getMemberDetail(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Optional.empty();
        }

        return userRepository.findByUserId(userId.trim())
                .map(this::toUserDto);
    }

    private UserDto toUserDto(Member member) {
        return UserDto.builder()
                .userId(member.getUserId())
                .name(member.getName())
                .password(member.getPassword())
                .email(member.getEmail())
                .phone(member.getPhone())
                .zipcode(member.getZipcode())
                .address(member.getAddress())
                .birthDate(member.getBirthDate())
                .gender(member.getGender())
                .successionYn(member.getSuccessionYn())
                .blacklisted(member.getBlacklisted())
                .createdDate(member.getCreatedDate())
                .sessionDate(member.getSessionDate())
                .loginFailCnt(member.getLoginFailCnt())
                .status(member.getStatus())
                .lastLogin(member.getLastLogin())
                .marketingAgree(member.getMarketingAgree())
                .socialId(member.getSocialId())
                .marketingAgent(member.getMarketingAgent())
                .gradeId(member.getMemberGrade() != null ? member.getMemberGrade().getGradeId() : null)
                .updatedDate(member.getUpdatedDate())
                .myAddress(member.getMyAddress())
                .secessionYn(member.getSecessionYn())
                .secessionDate(member.getSecessionDate())
                .profileImg(member.getProfileImg())
                .socialType(member.getSocialType())
                .accountLocked(member.isAccountLocked())
                .isBlacklisted(member.isBlacklisted())
                .isSeceded(member.isSeceded())
                .build();
    }

    public boolean isMemberExists(String userId) {
        return userRepository.existsByUserId(userId);
    }

    @Transactional
    public void deleteMember(String userId) {
        Member member = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("회원이 존재하지 않습니다."));

        WithdrawnMember withdrawnMember = WithdrawnMember.builder()
                .withdrawnId(generateWithdrawnId())
                .userId(member.getUserId())
                .withdrawnName(member.getName())
                .withdrawnEmail(member.getEmail())
                .withdrawnPhone(member.getPhone())
                .gradeId(member.getMemberGrade() != null ? member.getMemberGrade().getGradeId() : null)
                .withdrawnReason("사용자 요청")
                .withdrawnOriginalCreatedDate(member.getCreatedDate().toLocalDate())
                .withdrawnWithdrawnDate(LocalDate.now())
                .withdrawnSecessionDate(LocalDate.now())
                .build();
        withdrawnUserRepository.save(withdrawnMember);

        userRepository.delete(member);
        log.info("회원 탈퇴 처리 완료 - userId: {}", userId);
    }

    private String generateWithdrawnId() {
        return "WD" + System.currentTimeMillis();
    }

    public Map<String, String> getNicknameMapByUserIds(List<String> userIds) {
        Map<String, String> nicknameMap = new HashMap<>();
        for (String id : userIds) {
            userRepository.findById(id).ifPresent(member -> {
                nicknameMap.put(id, member.getName());
            });
        }
        return nicknameMap;
    }

    @Transactional
    public void updateMember(UserDto userDto) {
        Member member = userRepository.findById(userDto.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        member.setName(userDto.getName());
        member.setEmail(userDto.getEmail());
        member.setPhone(userDto.getPhone());
        member.setZipcode(userDto.getZipcode());
        member.setAddress(userDto.getAddress());
        member.setBirthDate(userDto.getBirthDate());
        member.setGender(userDto.getGender());
        member.setMarketingAgree(userDto.getMarketingAgree());
        member.setMyAddress(userDto.getMyAddress());

        String password = userDto.getPassword();
        if (password != null && !password.trim().isEmpty()) {
            member.setPassword(passwordEncoder.encode(password));
        }

        userRepository.save(member);
        log.info("회원 정보 수정 완료 - userId: {}", userDto.getUserId());
    }

    public Optional<Member> findMemberEntityByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }

    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public Optional<UserDto> getMemberByNameAndEmail(String name, String email) {
        try {
            Optional<Member> memberOpt = userRepository.findByNameAndEmailAndStatusAndSecessionYn(
                    name, email, "Y", "N"
            );

            if (memberOpt.isPresent()) {
                Member member = memberOpt.get();
                UserDto userDto = toUserDto(member);
                return Optional.of(userDto);
            } else {
                return Optional.empty();
            }

        } catch (Exception e) {
            log.error("회원 조회 중 오류 발생: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Transactional
    public void updatePasswordRaw(String userId, String rawPassword) {
        Optional<Member> memberOptional = userRepository.findByUserId(userId);
        if (memberOptional.isPresent()) {
            Member member = memberOptional.get();
            member.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(member);
            log.info("비밀번호 업데이트 완료 (원본 비밀번호 암호화) - userId: {}", userId);
        } else {
            throw new RuntimeException("해당 회원을 찾을 수 없습니다.");
        }
    }

    public Optional<MemberGrade> getMemberGrade(String userId) {
        return userRepository.findByUserId(userId)
                .map(Member::getMemberGrade);
    }

    public long countMembersByGrade(String gradeId) {
        return userRepository.countByMemberGradeGradeId(gradeId);
    }

    @Transactional
    public void changeMemberGrade(String userId, String newGradeId) {
        Member member = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        MemberGrade newGrade = userGradeRepository.findById(newGradeId)
                .orElseThrow(() -> new RuntimeException("등급을 찾을 수 없습니다: " + newGradeId));

        String oldGradeName = member.getMemberGrade().getGradeName();
        member.setMemberGrade(newGrade);
        userRepository.save(member);

        log.info("관리자에 의한 등급 변경 완료 - userId: {}, {} → {}",
                userId, oldGradeName, newGrade.getGradeName());
    }

    public List<MemberGrade> getAllGrades() {
        return userGradeRepository.findAllByOrderByGradeMinAmountAsc();
    }

    public String getUserEmailByUserId(String userId) {
        try {
            Optional<Member> memberOpt = userRepository.findByUserId(userId);
            if (memberOpt.isPresent()) {
                Member member = memberOpt.get();
                String email = member.getEmail();
                return email;
            }

            return null;

        } catch (Exception e) {
            log.error("사용자 이메일 조회 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("사용자 이메일 조회 실패", e);
        }
    }

}