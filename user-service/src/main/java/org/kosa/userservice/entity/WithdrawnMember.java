package org.kosa.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_WITHDRAWN_MEMBER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawnMember {

    @Id
    @Column(name = "WITHDRAWN_ID", nullable = false, length = 30)
    private String withdrawnId; // PK - WITHDRAWN_ID

    @Column(name = "USER_ID", nullable = false, length = 30)
    private String userId; // 회원아이디

    @Column(name = "WITHDRAWN_NAME", nullable = false, length = 30)
    private String withdrawnName; // 이름

    @Column(name = "WITHDRAWN_EMAIL", nullable = false, length = 50)
    private String withdrawnEmail; // 이메일

    @Column(name = "WITHDRAWN_PHONE")
    private String withdrawnPhone; 

    @Column(name = "GRADE_ID", nullable = false, length = 20)
    private String gradeId; // 등급ID

    @Column(name = "WITHDRAWN_REASON", nullable = false, length = 255)
    private String withdrawnReason; // 탈퇴사유

    @Column(name = "WITHDRAWN_ORIGINAL_CREATED_DATE", nullable = false)
    private LocalDate withdrawnOriginalCreatedDate; // 원래 가입일

    @Column(name = "WITHDRAWN_WITHDRAWN_DATE", nullable = false)
    private LocalDate withdrawnWithdrawnDate; // 탈퇴처리일

    @Column(name = "WITHDRAWN_SECESSION_DATE", nullable = false)
    private LocalDate withdrawnSecessionDate; // 탈퇴일
}