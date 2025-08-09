package com.mongodb.domain;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

// LocalCMKService 的作用可以概括为：
// 确保本地主密钥的存在 ：如果密钥文件 my-key.txt 不存在，就自动生成一个。
// 提供符合规范的凭证 ：读取密钥文件的内容，并将其包装成 MongoDB 驱动进行加密配置时所需要的标准格式。
// 最终， MongoEncryptionConfiguration 会调用 localCMKService.getKmsProviderCredentials() 方法，获取到这个包含本地主密钥的凭证，并用它来配置 MongoClient ，从而让整个应用程序具备自动进行字段级加密和解密的能力。

@Service
public class LocalCMKService {

    private static final String CUSTOMER_KEY_PATH = "src/main/resources/my-key.txt";
    private static final int KEY_SIZE = 96;

    private boolean isCustomerMasterKeyFileExists() {
        return new File(CUSTOMER_KEY_PATH).isFile();
    }

    // `create()` 方法会生成一个 96（KEY_SIZE）字节的随机字节数组。这串随机字节就是我们的**客户主密钥 (CMK)**。
    // 然后，它将这个字节数组写入到 `src/main/resources/my-key.txt` 文件中。这个文件就是我们加密体系的“根”，**必须妥善保管，绝对不能泄露**。
    private void create() throws IOException {
        byte[] cmk = new byte[KEY_SIZE];
        new SecureRandom().nextBytes(cmk);

        try (FileOutputStream stream = new FileOutputStream(CUSTOMER_KEY_PATH)) {
            stream.write(cmk);
        } catch (IOException e) {
            throw new IOException("Unable to write Customer Master Key file: " + e.getMessage(), e);
        }
    }

    // 无论是刚刚创建了新密钥，还是密钥文件原本就存在，程序都会调用 read() 方法从 my-key.txt 文件中读取 96 字节的内容。
    // read() 方法还包含一个校验逻辑，确保读取到的字节数正好是 96 字节，如果不是，就会抛出异常。
   private byte[] read() throws IOException {
        byte[] cmk = new byte[KEY_SIZE];

        try (FileInputStream fis = new FileInputStream(CUSTOMER_KEY_PATH)) {
            int bytesRead = fis.read(cmk);
            if (bytesRead != KEY_SIZE) {
                throw new IOException("Expected the customer master key file to be " + KEY_SIZE + " bytes, but read " + bytesRead + " bytes.");
            }
        } catch (IOException e) {
            throw new IOException("Unable to read the Customer Master Key: " + e.getMessage(), e);
        }

        return cmk;
    }

    // 构建 KMS 提供者凭证
    // 这是最后一步，也是最关键的一步。它将读取到的密钥字节数组（ localCustomerMasterKey ）包装成一个符合 MongoDB 驱动要求的 Map 结构。
    // kmsProviderCredentials.put("local", keyMap) 这行代码告诉 MongoDB 驱动：
    // 我们要使用的 KMS 提供者是 "local" 类型（即本地密钥文件）。
    // 这个 "local" 提供者所需要的配置信息在 keyMap 中。
    // keyMap.put("key", localCustomerMasterKey) 这行代码则进一步说明：
    // 对于 "local" 提供者，它需要的具体密钥是 localCustomerMasterKey 这个字节数组。
   public Map<String, Map<String, Object>> getKmsProviderCredentials() throws IOException {

        try {
            if (!isCustomerMasterKeyFileExists()) {
                create();
            }

            byte[] localCustomerMasterKey = read();

            Map<String, Object> keyMap = new HashMap<>();
            keyMap.put("key", localCustomerMasterKey);

            Map<String, Map<String, Object>> kmsProviderCredentials = new HashMap<>();
            kmsProviderCredentials.put("local", keyMap);

            return kmsProviderCredentials;
        }catch (Exception e) {
            throw new IOException("Unable to read the Customer Master Key file: " + e.getMessage(), e);
        }

    }

}
