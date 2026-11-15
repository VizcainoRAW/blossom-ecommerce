package com.blossom.ecommerce.infrastructure.persistence;

import com.blossom.ecommerce.domain.model.user.valueobject.DocumentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employees")
@DiscriminatorValue("EMPLOYEE")
@PrimaryKeyJoinColumn(name = "account_id")
@Getter
@Setter
public class EmployeeEntity extends AccountEntity {

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type")
    private DocumentType documentType;

    @Column(name = "document_number")
    private String documentNumber;
}
