# AutoAssignment
目的用于解决两个对象之间的赋值

1.简单使用：
class Person {
   String name;
   int age;
   String sex;
}

class User{
   @Param(name = "name")
   String userName;
   @Param(name = "sex")
   String userSex;
}

...

Person person = new Person();
person.name = "da";
person.age = "1";
person.sex = "男";

User user = new User();
Resolver resolver = new FieldResolver();
resolver.

...

例子：
