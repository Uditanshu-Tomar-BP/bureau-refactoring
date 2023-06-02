package com.bharatpe.lending.bureaurefactoring.utils;

import com.bharatpe.common.dao.InternalClientDao;
import com.bharatpe.common.entities.InternalClient;
import com.bharatpe.common.utils.AesEncryption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CommonUtils {
    @Autowired
    InternalClientDao internalClientDao;

    @Autowired
    AesEncryption aesEncryption;

    public String getInternalSecret(String internalClient) {

        InternalClient client = internalClientDao.findByClientName(internalClient);
        if (client != null) {
            return aesEncryption.decrypt(client.getSecret());

        }
        return null;
    }

}
