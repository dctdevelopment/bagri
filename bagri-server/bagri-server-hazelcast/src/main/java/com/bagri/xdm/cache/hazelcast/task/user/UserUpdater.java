package com.bagri.xdm.cache.hazelcast.task.user;

import static com.bagri.xdm.client.hazelcast.serialize.DataSerializationFactoryImpl.cli_UpdateUserTask;

import java.io.IOException;
import java.util.Map.Entry;

import com.bagri.common.security.Encryptor;
import com.bagri.xdm.system.XDMUser;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

public class UserUpdater extends UserProcessor implements IdentifiedDataSerializable {

	private String oldPassword;
	private String newPassword;
	
	public UserUpdater() {
		//
	}
	
	public UserUpdater(int version, String admin, String oldPassword, String newPassword) {
		super(version, admin);
		this.oldPassword = oldPassword;
		this.newPassword = newPassword;
	}

	@Override
	public Object process(Entry<String, XDMUser> entry) {
		logger.debug("process.enter; entry: {}", entry); 
		if (entry.getValue() != null) {
			XDMUser user = entry.getValue();
			if (user.getVersion() == getVersion()) {
				String pwd = Encryptor.encrypt(oldPassword);
				if (pwd.equals(user.getPassword())) {
					pwd = Encryptor.encrypt(newPassword);
					user.setPassword(pwd);
					user.updateVersion(getAdmin());
					entry.setValue(user);
					auditEntity(AuditType.update, user);
					return user;
				} else {
					logger.warn("process; existing password does not match for user: {}", entry.getKey()); 
				}
			} // else ..
		} 
		return null;
	}

	@Override
	public int getId() {
		return cli_UpdateUserTask;
	}
	
	@Override
	public void readData(ObjectDataInput in) throws IOException {
		super.readData(in);
		oldPassword = in.readUTF();
		newPassword = in.readUTF();
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		super.writeData(out);
		out.writeUTF(oldPassword);
		out.writeUTF(newPassword);
	}

	
}
