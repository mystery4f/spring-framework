package indi.shui4.thinking.ioc.java.beans;

/**
 * Person
 * Setter / Getter 方法
 * 可写方法 / 可读方法
 *
 * @author shui4
 */
public class Person {
    /**
     * name
     */
    private String name;
    /**
     * age
     */
    private Integer age;


    /**
     * getAge
     *
     * @return Integer
     */
    public Integer getAge() {
        return age;
    }

    /**
     * setAge
     *
     * @param age age
     */
    public void setAge(Integer age) {
        this.age = age;
    }

    /**
     * getName
     *
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * setName
     *
     * @param name name
     */
    public void setName(String name) {
        this.name = name;
    }
}
