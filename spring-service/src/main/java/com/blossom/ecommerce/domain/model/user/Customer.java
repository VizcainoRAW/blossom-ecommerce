package com.blossom.ecommerce.domain.model.user;

import com.blossom.ecommerce.domain.exception.account.InvalidCustomerException;
import com.blossom.ecommerce.domain.model.user.valueobject.AccountType;
import com.blossom.ecommerce.domain.model.user.valueobject.DocumentType;

import java.util.UUID;

public class Customer extends Account {

    private String firstName;
    private String lastName;
    private DocumentType documentType;
    private String documentNumber;

    private Customer(UUID id, String identifier, String passwordHash, boolean active) {
        super(id, AccountType.CUSTOMER, identifier, passwordHash, active);
    }

    public static Customer register(String email, String passwordHash, String firstName,
                                    String lastName, DocumentType type, String number) {
        if (firstName == null || firstName.isBlank()) throw new InvalidCustomerException("First name required");
        if (lastName == null || lastName.isBlank())   throw new InvalidCustomerException("Last name required");
        boolean hasNumber = number != null && !number.isBlank();
        if ((type == null) == hasNumber)              throw new InvalidCustomerException("Document type and number go together");

        Customer c = new Customer(UUID.randomUUID(), email, passwordHash, true);
        c.firstName = firstName.trim();
        c.lastName = lastName.trim();
        c.documentType = type;
        c.documentNumber = hasNumber ? number.trim() : null;
        return c;
    }

    public static Customer restore(UUID id, String identifier, String passwordHash, boolean active,
                                   String firstName, String lastName, DocumentType type, String number) {
        Customer c = new Customer(id, identifier, passwordHash, active);
        c.firstName = firstName;
        c.lastName = lastName;
        c.documentType = type;
        c.documentNumber = number;
        return c;
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public DocumentType getDocumentType() { return documentType; }
    public String getDocumentNumber() { return documentNumber; }
    public String fullName() { return firstName + " " + lastName; }

    @Override
    public String toString() { return "Customer:" + fullName(); }
}
