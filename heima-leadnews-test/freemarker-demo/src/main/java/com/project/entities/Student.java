package com.project.entities;

import lombok.Data;

import java.util.Date;

@Data
public class Student {
    private String name;

    private int age;

    private Date birthday;

    private Float money;
}
