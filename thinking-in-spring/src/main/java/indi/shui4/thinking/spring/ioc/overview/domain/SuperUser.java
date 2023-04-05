package indi.shui4.thinking.spring.ioc.overview.domain;

import indi.shui4.thinking.spring.ioc.overview.annotation.Supper;

/**
 * @author shui4
 */
@Supper
public class SuperUser extends User {
	private String address;

	@Override
	public String toString() {
		return "SuperUser{" +
				"address='" + address + '\'' +
				"} " + super.toString();
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
}
