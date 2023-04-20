package indi.shui4.thinking.spring.ioc.overview.domain;

import indi.shui4.thinking.spring.ioc.overview.enums.City;
import org.springframework.core.io.Resource;

import java.util.Arrays;
import java.util.List;

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

    private City[] workCities;
    private List<City> liteCities;

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", city=" + city +
                ", configFileLocation=" + configFileLocation +
                ", workCities=" + Arrays.toString(workCities) +
                ", liteCities=" + liteCities +
                '}';
    }

    public List<City> getLiteCities() {
        return liteCities;
    }

    public void setLiteCities(List<City> liteCities) {
        this.liteCities = liteCities;
    }

    public City[] getWorkCities() {
        return workCities;
    }

    public void setWorkCities(City[] workCities) {
        this.workCities = workCities;
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
