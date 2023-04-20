package indi.shui4.thinking.spring.ioc.overview.domain;

import indi.shui4.thinking.spring.ioc.overview.enums.City;
import org.springframework.core.io.Resource;

/**
 * 用户
 *
 * @author shui4
 */
public class User {
    private String id;
    private String name;

    private City city;
    private Resource configFileLocation;

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", city=" + city +
                ", configFileLocation=" + configFileLocation +
                '}';
    }

    public Resource getConfigFileLocation() {
        return configFileLocation;
    }

    public void setConfigFileLocation(Resource configFileLocation) {
        this.configFileLocation = configFileLocation;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public static User createUser() {
        User user = new User();
        user.setId("1");
        user.setName("shui4");
        return user;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
