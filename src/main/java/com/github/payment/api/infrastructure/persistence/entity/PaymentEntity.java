package com.github.payment.api.infrastructure.persistence.entity;


import com.github.payment.api.domain.enuns.Currency;
import com.github.payment.api.domain.enuns.PaymentMethod;
import com.github.payment.api.domain.enuns.StatusPayment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "payment")
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_payment", nullable = false)
    private StatusPayment statusPayment;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_method_id", nullable = false)
    private String paymentMethodId;

    @Column(name = "gateway_transaction_id")
    private String gatewayTransactionId;

    @CreationTimestamp //TODO = VERIFICAR ATRIBUTO TIMESTAMP
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(nullable = false)
    private int retry;



}
