package com.blossom.ecommerce.domain.model.user;

import com.blossom.ecommerce.domain.exception.account.InvalidCustomerException;
import com.blossom.ecommerce.domain.exception.account.InvalidIdentifierException;
import com.blossom.ecommerce.domain.model.user.valueobject.AccountType;
import com.blossom.ecommerce.domain.model.user.valueobject.DocumentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerTest {

    @Test
    void registerNormalizesIdentifierAndTrimsNames() {
        Customer c = Customer.register("  Ana@Mail.COM ", "hash", " Ana ", " Diaz ", DocumentType.CC, " 123 ");

        assertThat(c.getId()).isNotNull();
        assertThat(c.getIdentifier()).isEqualTo("ana@mail.com");
        assertThat(c.getPasswordHash()).isEqualTo("hash");
        assertThat(c.getAccountType()).isEqualTo(AccountType.CUSTOMER);
        assertThat(c.isActive()).isTrue();
        assertThat(c.fullName()).isEqualTo("Ana Diaz");
        assertThat(c.getDocumentType()).isEqualTo(DocumentType.CC);
        assertThat(c.getDocumentNumber()).isEqualTo("123");
        assertThat(c.toString()).isEqualTo("Customer:Ana Diaz");
    }

    @Test
    void registerWithoutDocumentIsAllowed() {
        Customer c = Customer.register("a@b.c", "hash", "Ana", "Diaz", null, null);

        assertThat(c.getDocumentType()).isNull();
        assertThat(c.getDocumentNumber()).isNull();
    }

    @Test
    void registerRequiresNames() {
        assertThatThrownBy(() -> Customer.register("a@b.c", "h", " ", "Diaz", null, null))
                .isInstanceOf(InvalidCustomerException.class).hasMessage("First name required");
        assertThatThrownBy(() -> Customer.register("a@b.c", "h", "Ana", null, null, null))
                .isInstanceOf(InvalidCustomerException.class).hasMessage("Last name required");
    }

    @Test
    void documentTypeAndNumberGoTogether() {
        assertThatThrownBy(() -> Customer.register("a@b.c", "h", "Ana", "Diaz", DocumentType.CC, " "))
                .isInstanceOf(InvalidCustomerException.class);
        assertThatThrownBy(() -> Customer.register("a@b.c", "h", "Ana", "Diaz", null, "123"))
                .isInstanceOf(InvalidCustomerException.class);
    }

    @Test
    void identifierIsRequired() {
        assertThatThrownBy(() -> Customer.register(" ", "h", "Ana", "Diaz", null, null))
                .isInstanceOf(InvalidIdentifierException.class);
        assertThatThrownBy(() -> Customer.register(null, "h", "Ana", "Diaz", null, null))
                .isInstanceOf(InvalidIdentifierException.class);
    }

    @Test
    void restoreKeepsStoredValues() {
        UUID id = UUID.randomUUID();
        Customer c = Customer.restore(id, "a@b.c", "hash", false, "Ana", "Diaz", DocumentType.PASSPORT, "X1");

        assertThat(c.getId()).isEqualTo(id);
        assertThat(c.isActive()).isFalse();
        assertThat(c.getFirstName()).isEqualTo("Ana");
        assertThat(c.getLastName()).isEqualTo("Diaz");
        assertThat(c.getDocumentType()).isEqualTo(DocumentType.PASSPORT);
    }

    @Test
    void employeeRestoreKeepsStoredValues() {
        UUID id = UUID.randomUUID();
        Employee e = Employee.restore(id, "admin@b.c", "hash", true, "Ad", "Min", 7L, DocumentType.CC, "9");

        assertThat(e.getAccountType()).isEqualTo(AccountType.EMPLOYEE);
        assertThat(e.getRoleId()).isEqualTo(7L);
        assertThat(e.getFirstName()).isEqualTo("Ad");
        assertThat(e.getLastName()).isEqualTo("Min");
        assertThat(e.getDocumentType()).isEqualTo(DocumentType.CC);
        assertThat(e.getDocumentNumber()).isEqualTo("9");
        assertThat(e.toString()).isEqualTo("Employee:Ad Min");
    }
}
