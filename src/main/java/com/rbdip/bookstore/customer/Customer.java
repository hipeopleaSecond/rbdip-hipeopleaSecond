package com.rbdip.bookstore.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Column(name = "last_name", nullable = false)
    private String lastName;
    private String address;
    private String phone;

    protected Customer() { // JPA
    }

    public Customer(String name, String address, String phone) {
        int separator = name.indexOf(' ');
        this.firstName = separator < 0 ? name : name.substring(0, separator);
        this.lastName = separator < 0 ? "" : name.substring(separator + 1);
        this.address = address;
        this.phone = phone;
    }

    public String getFullName() {
        return lastName.isEmpty() ? firstName : firstName + " " + lastName;
    }

    public String getAddress() { return address; }
    public String getPhone() { return phone; }
}
