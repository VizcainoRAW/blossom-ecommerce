package com.blossom.ecommerce.domain.model.user;

import com.blossom.ecommerce.domain.model.user.valueobject.AccountType;
import com.blossom.ecommerce.domain.model.user.valueobject.DocumentType;

import java.util.UUID;

public class Employee extends Account {

    private String firstName;
    private String lastName;
    private Long roleId;
    private DocumentType documentType;
    private String documentNumber;

    private Employee(UUID id, String identifier, String passwordHash, boolean active) {
        super(id, AccountType.EMPLOYEE, identifier, passwordHash, active);
    }

    public static Employee restore(UUID id, String identifier, String passwordHash, boolean active,
                                   String firstName, String lastName, Long roleId, DocumentType type, String number) {
        Employee e = new Employee(id, identifier, passwordHash, active);
        e.firstName = firstName;
        e.lastName = lastName;
        e.roleId = roleId;
        e.documentType = type;
        e.documentNumber = number;
        return e;
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public Long getRoleId() { return roleId; }
    public DocumentType getDocumentType() { return documentType; }
    public String getDocumentNumber() { return documentNumber; }

    @Override
    public String toString() { return "Employee:" + firstName + " " + lastName; }
}
