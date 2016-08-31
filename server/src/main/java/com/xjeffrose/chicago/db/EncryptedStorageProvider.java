package com.xjeffrose.chicago.db;

import com.intel.chimera.cipher.Cipher;
import com.intel.chimera.cipher.CipherTransformation;
import com.intel.chimera.stream.CryptoInputStream;
import com.intel.chimera.stream.CryptoOutputStream;
import com.intel.chimera.utils.Utils;
import com.xjeffrose.chicago.ZkClient;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

@Slf4j
public class EncryptedStorageProvider implements StorageProvider {
  private final StorageProvider db;
  private final byte[] key;
  private final byte[] iv;
  private final Properties properties = new Properties();
  private Cipher cipher;

  public EncryptedStorageProvider(StorageProvider db) {
    this.db = db;
    this.key = new byte[16];
    this.iv = new byte[16];

    configure();
  }

  public EncryptedStorageProvider(StorageProvider db, byte[] key, byte[] iv) {
    this.db = db;
    this.key = key;
    this.iv = iv;

    configure();
  }


  private void configure() {
    properties.setProperty("chimera.crypto.cipher.classes", "com.intel.chimera.cipher.OpensslCipher");
    try {
      cipher = Utils.getCipherInstance(CipherTransformation.AES_CTR_NOPADDING, properties);
    } catch (IOException e) {
      log.error("Error while configuring the encryption handler: ", e);
    }
  }

  synchronized byte[] encrypt(byte[] raw) {
    try {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      CryptoOutputStream cos = new CryptoOutputStream(os, cipher, 4096, key, iv);

      cos.write(raw);
      cos.flush();
      cos.close();

      return os.toByteArray();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  synchronized byte[] decrypt(byte[] encryptedData) {
    try {
      final byte[] decryptedData = new byte[encryptedData.length];
      CryptoInputStream cis = null;
      cis = new CryptoInputStream(new ByteArrayInputStream(encryptedData), cipher, 4096, key, iv);
      cis.read(decryptedData, 0, encryptedData.length);

      return decryptedData;
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  @Override
  public boolean write(byte[] colFam, byte[] key, byte[] val) {
    return db.write(colFam, key, encrypt(val));
  }

  @Override
  public byte[] read(byte[] colFam, byte[] key) {
    byte[] read = db.read(colFam, key);

    return decrypt(read);
  }

  @Override
  public boolean delete(byte[] colFam, byte[] key) {
    return db.delete(colFam, key);
  }

  @Override
  public boolean delete(byte[] colFam) {
    return db.delete(colFam);
  }

  @Override
  public byte[] tsWrite(byte[] colFam, byte[] val) {
    return db.tsWrite(colFam, encrypt(val));
  }

  @Override
  public byte[] batchWrite(byte[] colFam, byte[] val) {
    return db.batchWrite(colFam, encrypt(val));
  }

  @Override
  public List<DBRecord> stream(byte[] colFam, byte[] key) {
    List<DBRecord> records  = db.stream(colFam, key);
    for(DBRecord record : records){
      record.setValue(decrypt(record.getValue()));
    }
    return records;
  }

  @Override
  public byte[] tsWrite(byte[] colFam, byte[] key, byte[] val) {
    return db.tsWrite(colFam, key, encrypt(val));
  }

  @Override
  public void close() {
    db.close();
  }

  @Override
  public void open() {
    db.open();
  }

  @Override public void setZkClient(ZkClient zkClient) {
    db.setZkClient(zkClient);
  }

  @Override public List<byte[]> getKeys(byte[] colFam, byte[] offset) {
    return db.getKeys(colFam,offset);
  }

  @Override public List<String> getColFams() {
    return db.getColFams();
  }
}
