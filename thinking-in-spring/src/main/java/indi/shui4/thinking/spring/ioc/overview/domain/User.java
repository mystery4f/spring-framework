package indi.shui4.thinking.spring.ioc.overview.domain;

import indi.shui4.thinking.spring.ioc.overview.enums.City;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * 用户
 *
 * @author shui4
 */
public class User implements BeanNameAware {
	private String id;
	private String name;
	private City city;

	private Company company;
	private Resource configFileLocation;
	private City[] workCities;
	private List<City> liteCities;
	private String beanName;

	private Properties context;

	private String contextAsText;

	public static User createUser() {
		User user = new User();
		user.setId(System.nanoTime());
		user.setName("shui4");
		return user;
	}

	@Override
	public String toString() {
		return "User{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				", city=" + city +
				", company=" + company +
				", configFileLocation=" + configFileLocation +
				", workCities=" + Arrays.toString(workCities) +
				", liteCities=" + liteCities +
				", beanName='" + beanName + '\'' +
				", context=" + context +
				", contextAsText='" + contextAsText + '\'' +
				'}';
	}

	public String getContextAsText() {
		return contextAsText;
	}

	public void setContextAsText(String contextAsText) {
		this.contextAsText = contextAsText;
	}

	@PostConstruct
	public void init() {
		System.out.println(beanName + "用户对象初始化...");
	}

	@PreDestroy
	public void destroy() {
		System.out.println(beanName + ":用户对象销毁...");
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

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setId(Long id) {
		this.id = id.toString();
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	public Company getCompany() {
		return company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

	public Properties getContext() {
		return context;
	}

	public void setContext(Properties context) {
		this.context = context;
	}

}
